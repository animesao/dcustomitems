package me.dcplugin.dcustomitems.api;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import me.dcplugin.dcustomitems.api.placeholders.CustomPlaceholder;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Оптимизированный Runtime компилятор для .java файлов
 * 
 * Оптимизации:
 * - Кэширование скомпилированных классов
 * - Пропуск неизмененных файлов
 * - Асинхронная компиляция
 * - Минимальная нагрузка на сервер
 */
public class JavaItemCompiler {

    private final Main plugin;
    private final Path itemsDir;
    private final Path compiledDir;
    private final Path cacheDir;
    private final Map<String, byte[]> compiledClasses = new ConcurrentHashMap<>();
    private final Map<String, String> fileHashes = new ConcurrentHashMap<>(); // file -> hash
    private final Map<String, String> sourceClassAliases = new ConcurrentHashMap<>();
    private final Map<String, File> moduleSourceFolders = new ConcurrentHashMap<>();
    private CustomClassLoader classLoader;
    private final AtomicBoolean compiling = new AtomicBoolean(false);
    
    private static final String PACKAGE = "items";

    public JavaItemCompiler(Main plugin) {
        this.plugin = plugin;
        this.itemsDir = plugin.getDataFolder().toPath().resolve("items");
        this.compiledDir = plugin.getDataFolder().toPath().resolve("compiled");
        this.cacheDir = plugin.getDataFolder().toPath().resolve("cache");
    }

    /**
     * Асинхронная компиляция всех .java файлов
     * Возвращает CompletableFuture с результатом
     */
    public CompletableFuture<CompileResult> compileAllAsync() {
        if (compiling.get()) {
            plugin.getLogger().info("[JavaCompiler] Already compiling, skipping...");
            return CompletableFuture.completedFuture(new CompileResult());
        }
        
        return CompletableFuture.supplyAsync(() -> {
            compiling.set(true);
            try {
                return compileAllInternal();
            } finally {
                compiling.set(false);
            }
        });
    }

    /**
     * Синхронная компиляция (для использования при старте)
     */
    public CompileResult compileAll() {
        return compileAllInternal();
    }

    private CompileResult compileAllInternal() {
        long startTime = System.currentTimeMillis();
        compiledClasses.clear();
        CompileResult result = new CompileResult();

        try {
            Files.createDirectories(compiledDir);
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                "[JavaCompiler] Cannot create directories: " + e.getMessage(), e);
            return result;
        }

        // Загружаем кэш хэшей файлов
        loadFileHashesCache();

        // Ищем .java файлы рекурсивно, включая подпапки-модули.
        List<File> javaFiles = findJavaFiles(itemsDir.toFile());
        if (javaFiles.isEmpty()) {
            return result;
        }

        // Сначала строим таблицу имён. Это позволяет файлам одного модуля
        // ссылаться друг на друга после переименования классов компилятором.
        sourceClassAliases.clear();
        for (File javaFile : javaFiles) {
            String source = readFileContent(javaFile);
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("public\\s+(?:final\\s+)?class\\s+(\\w+)")
                .matcher(source);
            if (matcher.find()) {
                String generatedName = getGeneratedClassName(javaFile);
                sourceClassAliases.put(matcher.group(1), generatedName);
                if (detectClassType(source) == ClassType.MODULE) {
                    moduleSourceFolders.put(PACKAGE + "." + generatedName, javaFile.getParentFile());
                }
            }
        }

        plugin.getLogger().info("[JavaCompiler] Found " + javaFiles.size() + " .java files");

        int skipped = 0;
        int compiled = 0;

        for (File javaFile : javaFiles) {
            // Проверяем, изменился ли файл
            String currentHash = getFileHash(javaFile);
            String cacheKey = getCacheKey(javaFile);
            String cachedHash = fileHashes.get(cacheKey);
            
            if (currentHash.equals(cachedHash)) {
                // Файл не изменился - пропускаем компиляцию
                String fullClassName = PACKAGE + "." + getGeneratedClassName(javaFile);
                
                // Загружаем из кэша весь класс-пакет: основной класс и его
                // вложенные классы (например, Outer$MenuItem). Раньше здесь
                // загружался только Outer.class, из-за чего модули с nested
                // classes падали после рестарта при режиме Skipped.
                Map<String, byte[]> cachedBundle = loadClassBundleFromCache(
                    fullClassName,
                    readFileContent(javaFile)
                );
                if (!cachedBundle.isEmpty()) {
                    compiledClasses.putAll(cachedBundle);
                    ClassType classType = detectClassType(readFileContent(javaFile));
                    addToResult(result, fullClassName, classType);
                    skipped++;
                    continue;
                }
            }

            // Компилируем файл
            if (compileFile(javaFile, result)) {
                compiled++;
                fileHashes.put(cacheKey, currentHash);
            }
        }

        // Сохраняем кэш хэшей
        saveFileHashesCache();

        // Загружаем классы
        if (compiledClasses.size() > 0) {
            try {
                classLoader = new CustomClassLoader(getClass().getClassLoader());
                for (Map.Entry<String, byte[]> entry : compiledClasses.entrySet()) {
                    classLoader.addClass(entry.getKey(), entry.getValue());
                }
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                "[JavaCompiler] Error loading classes: " + e.getMessage(), e);
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        plugin.getLogger().info("[JavaCompiler] Compiled: " + compiled + ", Skipped: " + skipped + " (" + elapsed + "ms)");

        return result;
    }

    private boolean compileFile(File javaFile, CompileResult result) {
        try {
            String sourceCode = readFileContent(javaFile);
            String fileName = javaFile.getName().replace(".java", "");

            // Добавляем импорты если нет
            if (!sourceCode.contains("import me.dcplugin.dcustomitems.api")) {
                sourceCode = "import me.dcplugin.dcustomitems.api.AbstractCustomItem;\n" +
                             "import me.dcplugin.dcustomitems.api.ItemAPI;\n" +
                             "import me.dcplugin.dcustomitems.api.commands.CustomCommand;\n" +
                             "import me.dcplugin.dcustomitems.api.placeholders.CustomPlaceholder;\n" +
                             sourceCode;
            }

            ClassType classType = detectClassType(sourceCode);
            String generatedClassName = getGeneratedClassName(javaFile);
            sourceCode = replaceClassNames(sourceCode);

            final String finalSourceCode = "package " + PACKAGE + ";\n" + sourceCode;
            final String fullClassName = PACKAGE + "." + generatedClassName;

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                plugin.getLogger().severe("[JavaCompiler] JavaCompiler not found! JDK required.");
                return false;
            }

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

            JavaFileObject source = new SimpleJavaFileObject(
                URI.create("string:///" + generatedClassName + ".java"),
                JavaFileObject.Kind.SOURCE
            ) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    return finalSourceCode;
                }
            };

            final Map<String, byte[]> classBytesHolder = new HashMap<>();

            JavaFileManager virtualFileManager = new ForwardingJavaFileManager<StandardJavaFileManager>(fileManager) {
                @Override
                public JavaFileObject getJavaFileForOutput(Location location, String className,
                                                           JavaFileObject.Kind kind, FileObject sibling) {
                    return new SimpleJavaFileObject(
                        URI.create("mem:///" + className.replace('.', '/') + ".class"),
                        JavaFileObject.Kind.CLASS
                    ) {
                        @Override
                        public OutputStream openOutputStream() {
                            return new ByteArrayOutputStream() {
                                @Override
                                public void close() throws IOException {
                                    super.close();
                                    classBytesHolder.put(className, toByteArray());
                                }
                            };
                        }
                    };
                }
            };

            List<String> options = List.of(
                "-classpath", getClasspath(),
                "-d", compiledDir.toString(),
                "-proc:none"
            );

            JavaCompiler.CompilationTask task = compiler.getTask(
                null, virtualFileManager, diagnostics, options, null, List.of(source)
            );

            boolean success = task.call();
            fileManager.close();

            if (success) {
                compiledClasses.putAll(classBytesHolder);
                
                // Сохраняем в кэш
                for (Map.Entry<String, byte[]> entry : classBytesHolder.entrySet()) {
                    saveToCache(entry.getKey(), entry.getValue());
                    // Оставляем class-файлы на диске, чтобы следующий исходник
                    // из той же папки мог использовать уже скомпилированный класс.
                    saveToCompiledDir(entry.getKey(), entry.getValue());
                }

                addToResult(result, fullClassName, classType);
                return true;
            } else {
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    String error = javaFile.getName() + ":" + diagnostic.getLineNumber() + " - " + diagnostic.getMessage(null);
                    plugin.getLogger().warning("[JavaCompiler] ❌ " + error);
                    result.errors.add(error);
                }
                return false;
            }

        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                "[JavaCompiler] Error: " + javaFile.getName() + ": " + e.getMessage(), e);
            result.errors.add(javaFile.getName() + ": " + e.getMessage());
            return false;
        }
    }

    private void addToResult(CompileResult result, String fullClassName, ClassType classType) {
        switch (classType) {
            case ITEM:
                result.items.add(fullClassName);
                break;
            case COMMAND:
                result.commands.add(fullClassName);
                break;
            case PLACEHOLDER:
                result.placeholders.add(fullClassName);
                break;
            case MODULE:
                result.modules.add(fullClassName);
                break;
            case UTILITY:
                // Компилируется, но не регистрируется (messages.java и т.п.)
                break;
        }
    }

    private String readFileContent(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().severe("[JavaCompiler] Error reading file: " + file.getName());
            return "";
        }
    }

    private ClassType detectClassType(String sourceCode) {
        // Порядок важен: не-утилитные файлы могут упоминать имена других типов
        if (sourceCode.contains("extends Module")) return ClassType.MODULE;
        if (sourceCode.contains("extends CustomCommand")) return ClassType.COMMAND;
        if (sourceCode.contains("extends CustomPlaceholder")) return ClassType.PLACEHOLDER;
        if (sourceCode.contains("extends AbstractCustomItem")) return ClassType.ITEM;
        // Утилиты (например messages.java) компилируются, но не регистрируются
        return ClassType.UTILITY;
    }

    private String replaceClassNames(String sourceCode) {
        String result = sourceCode;
        List<Map.Entry<String, String>> aliases = new ArrayList<>(sourceClassAliases.entrySet());
        aliases.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
        for (Map.Entry<String, String> alias : aliases) {
            result = result.replaceAll("\\b" + java.util.regex.Pattern.quote(alias.getKey()) + "\\b", alias.getValue());
        }
        return result;
    }

    private List<File> findJavaFiles(File directory) {
        List<File> files = new ArrayList<>();
        File[] children = directory.listFiles();
        if (children == null) return files;
        Arrays.sort(children, Comparator.comparing(File::getPath));
        for (File child : children) {
            if (child.isDirectory()) {
                files.addAll(findJavaFiles(child));
            } else if (child.getName().endsWith(".java")
                    && !child.getName().startsWith("Abstract")
                    && !child.getName().startsWith("Custom")
                    && !child.getName().startsWith("EXAMPLE-")) {
                files.add(child);
            }
        }
        return files;
    }

    public String getGeneratedClassName(File javaFile) {
        String relative;
        try {
            relative = itemsDir.relativize(javaFile.toPath()).toString();
        } catch (Exception e) {
            relative = javaFile.getName();
        }
        return relative.replace('\\', '_').replace('/', '_')
            .replace(".java", "").replace("-", "_").replace(" ", "_") + "Item";
    }

    private String getCacheKey(File javaFile) {
        try {
            return itemsDir.relativize(javaFile.toPath()).toString().replace('\\', '/');
        } catch (Exception e) {
            return javaFile.getName();
        }
    }

    // ===== КЭШИРОВАНИЕ =====

    private String getFileHash(File file) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(file.lastModified());
        }
    }

    private void loadFileHashesCache() {
        try {
            Path cacheFile = cacheDir.resolve("file_hashes.properties");
            if (Files.exists(cacheFile)) {
                Properties props = new Properties();
                props.load(Files.newInputStream(cacheFile));
                for (String key : props.stringPropertyNames()) {
                    fileHashes.put(key, props.getProperty(key));
                }
            }
        } catch (Exception ignored) {}
    }

    private void saveFileHashesCache() {
        try {
            Path cacheFile = cacheDir.resolve("file_hashes.properties");
            Properties props = new Properties();
            props.putAll(fileHashes);
            props.store(Files.newOutputStream(cacheFile), "DC-CustomItems File Hashes Cache");
        } catch (Exception ignored) {}
    }

    private Map<String, byte[]> loadClassBundleFromCache(String mainClassName, String sourceCode) {
        Map<String, byte[]> bundle = new LinkedHashMap<>();
        try {
            Path mainClassPath = cacheDir.resolve(mainClassName.replace('.', '/') + ".class");
            if (!Files.exists(mainClassPath)) return bundle;

            String classPrefix = mainClassName + "$";
            Path packageDirectory = mainClassPath.getParent();
            if (packageDirectory == null || !Files.exists(packageDirectory)) return bundle;

            try (java.util.stream.Stream<Path> paths = Files.walk(packageDirectory)) {
                paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".class"))
                    .forEach(path -> {
                        String relative = cacheDir.relativize(path).toString()
                            .replace(File.separatorChar, '.')
                            .replace('/', '.');
                        String className = relative.substring(0, relative.length() - ".class".length());
                        if (className.equals(mainClassName) || className.startsWith(classPrefix)) {
                            try {
                                bundle.put(className, Files.readAllBytes(path));
                            } catch (IOException ignored) {
                                // The whole bundle will be treated as unavailable below.
                            }
                        }
                    });
            }

            // A legacy cache can contain Outer.class but miss Outer$Inner.class.
            // Validate every named nested declaration before accepting Skipped.
            java.util.regex.Matcher nestedMatcher = java.util.regex.Pattern
                .compile("\\b(?:class|interface|enum|record)\\s+(\\w+)")
                .matcher(sourceCode);
            boolean firstDeclaration = true;
            while (nestedMatcher.find()) {
                if (firstDeclaration) {
                    firstDeclaration = false;
                    continue;
                }
                String nestedClassName = mainClassName + "$" + nestedMatcher.group(1);
                if (!bundle.containsKey(nestedClassName)) {
                    bundle.clear();
                    return bundle;
                }
            }
        } catch (Exception ignored) {
            bundle.clear();
        }
        return bundle;
    }

    private void saveToCache(String className, byte[] bytes) {
        try {
            Path cacheFile = cacheDir.resolve(className.replace('.', '/') + ".class");
            Files.createDirectories(cacheFile.getParent());
            Files.write(cacheFile, bytes);
        } catch (Exception ignored) {}
    }

    private void saveToCompiledDir(String className, byte[] bytes) {
        try {
            Path classFile = compiledDir.resolve(className.replace('.', '/') + ".class");
            Files.createDirectories(classFile.getParent());
            Files.write(classFile, bytes);
        } catch (Exception ignored) {}
    }

    // ===== КЛАССПАС =====

    private String getClasspath() {
        StringBuilder classpath = new StringBuilder();

        try {
            File pluginFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            classpath.append(pluginFile.getAbsolutePath());
        } catch (Exception ignored) {
            classpath.append("plugins/DC-CustomItems.jar");
        }

        // Серверный jar: Paper/Spigot/Folia/Purpur и т.д. Ищем в корне сервера
        // (plugins/..), в текущей папке и по относительным путям.
        File serverRoot = plugin.getDataFolder().getParentFile() != null
                ? plugin.getDataFolder().getParentFile().getParentFile() : null;
        List<String> candidates = new ArrayList<>();
        for (String name : new String[]{"server.jar", "paper.jar", "spigot.jar", "purpur.jar", "folia.jar", "paperclip.jar"}) {
            candidates.add(name);
            candidates.add("../" + name);
            if (serverRoot != null) {
                candidates.add(new File(serverRoot, name).getAbsolutePath());
            }
        }
        for (String path : candidates) {
            File serverJar = new File(path);
            if (serverJar.exists()) {
                classpath.append(File.pathSeparator).append(serverJar.getAbsolutePath());
                break;
            }
        }

        String[] libPaths = {"libraries", "../libraries"};
        for (String libPath : libPaths) {
            File libsDir = new File(libPath);
            if (libsDir.exists() && libsDir.isDirectory()) {
                addJarsFromClassDir(libsDir, classpath);
            }
        }

        // Папка plugins/DC-CustomItems/libs/ — любые сторонние библиотеки
        // (jars), которые нужны вашим Java-модулям. Кладёшь jar туда —
        // компилятор подхватит его на следующем /ci reload.
        File dataLibsDir = new File(plugin.getDataFolder(), "libs");
        if (dataLibsDir.exists() && dataLibsDir.isDirectory()) {
            addJarsFromClassDir(dataLibsDir, classpath);
        }

        // Jars установленных плагинов (VaultAPI, PlaceholderAPI, DeluxeMenus и т.д.).
        // Даёт модулям компилироваться против API других плагинов. Если нужного
        // плагина нет — скомпилируется только его модуль и ошибка ограничится им.
        File pluginsDir = plugin.getDataFolder().getParentFile(); // .../plugins
        if (pluginsDir != null && pluginsDir.exists() && pluginsDir.isDirectory()) {
            addJarsFromClassDir(pluginsDir, classpath);
        }

        classpath.append(File.pathSeparator).append(compiledDir.toString());
        return classpath.toString();
    }

    private void addJarsFromClassDir(File dir, StringBuilder classpath) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    addJarsFromClassDir(file, classpath);
                } else if (file.getName().endsWith(".jar")) {
                    classpath.append(File.pathSeparator).append(file.getAbsolutePath());
                }
            }
        }
    }

    // ===== ЗАГРУЗКА КЛАССОВ =====

    public Class<?> loadClass(String fullClassName) {
        if (classLoader == null) return null;
        try {
            return classLoader.findClass(fullClassName);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public AbstractCustomItem createItemInstance(String fullClassName) {
        try {
            Class<?> clazz = loadClass(fullClassName);
            if (clazz != null && AbstractCustomItem.class.isAssignableFrom(clazz)) {
                return (AbstractCustomItem) clazz.getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[JavaCompiler] Error creating item: " + fullClassName);
        }
        return null;
    }

    public CustomCommand createCommandInstance(String fullClassName) {
        try {
            Class<?> clazz = loadClass(fullClassName);
            if (clazz != null && CustomCommand.class.isAssignableFrom(clazz)) {
                CustomCommand cmd = (CustomCommand) clazz.getDeclaredConstructor().newInstance();
                cmd.setPlugin(plugin);
                return cmd;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[JavaCompiler] Error creating command: " + fullClassName);
        }
        return null;
    }

    public me.dcplugin.dcustomitems.api.modules.Module createModuleInstance(String fullClassName) {
        try {
            Class<?> clazz = loadClass(fullClassName);
            if (clazz != null && me.dcplugin.dcustomitems.api.modules.Module.class.isAssignableFrom(clazz)) {
                File moduleFolder = moduleSourceFolders.get(fullClassName);
                if (moduleFolder == null) {
                    moduleFolder = new File(plugin.getDataFolder(), "items/" + moduleIdFromClassName(fullClassName));
                }
                String moduleId = moduleFolder.getName();
                return (me.dcplugin.dcustomitems.api.modules.Module) clazz
                    .getDeclaredConstructor(Main.class, String.class, File.class)
                    .newInstance(plugin, moduleId, moduleFolder);
            }
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                "[JavaCompiler] Error creating module: " + fullClassName + " - " + e.getMessage(), e);
        }
        return null;
    }

    private String moduleIdFromClassName(String fullClassName) {
        String simpleName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
        if (simpleName.endsWith("_Item")) simpleName = simpleName.substring(0, simpleName.length() - 5);
        int separator = simpleName.indexOf('_');
        return separator > 0 ? simpleName.substring(0, separator) : simpleName;
    }

    public CustomPlaceholder createPlaceholderInstance(String fullClassName) {
        try {
            Class<?> clazz = loadClass(fullClassName);
            if (clazz != null && CustomPlaceholder.class.isAssignableFrom(clazz)) {
                CustomPlaceholder ph = (CustomPlaceholder) clazz.getDeclaredConstructor().newInstance();
                ph.setPlugin(plugin);
                return ph;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[JavaCompiler] Error creating placeholder: " + fullClassName);
        }
        return null;
    }

    public void clear() {
        compiledClasses.clear();
        fileHashes.clear();
        sourceClassAliases.clear();
        moduleSourceFolders.clear();
        classLoader = null;
    }

    public boolean isCompiling() {
        return compiling.get();
    }

    // ===== КЛАССЫ =====

    public enum ClassType { ITEM, COMMAND, PLACEHOLDER, MODULE, UTILITY }

    public static class CompileResult {
        public int compiled = 0;
        public List<String> items = new ArrayList<>();
        public List<String> commands = new ArrayList<>();
        public List<String> placeholders = new ArrayList<>();
        public List<String> modules = new ArrayList<>();
        public List<String> errors = new ArrayList<>();
    }

    private static class CustomClassLoader extends ClassLoader {
        private final Map<String, byte[]> classBytes = new HashMap<>();

        CustomClassLoader(ClassLoader parent) { super(parent); }

        void addClass(String name, byte[] bytes) {
            classBytes.put(name, bytes);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = classBytes.get(name);
            if (bytes != null) {
                return defineClass(name, bytes, 0, bytes.length);
            }
            throw new ClassNotFoundException(name);
        }
    }
}
