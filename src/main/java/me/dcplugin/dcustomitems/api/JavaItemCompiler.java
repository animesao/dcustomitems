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
            plugin.getLogger().severe("[JavaCompiler] Cannot create directories: " + e.getMessage());
            return result;
        }

        // Загружаем кэш хэшей файлов
        loadFileHashesCache();

        // Ищем все .java файлы
        File[] javaFiles = itemsDir.toFile().listFiles((dir, name) ->
            name.endsWith(".java") &&
            !name.startsWith("Abstract") &&
            !name.startsWith("Custom") &&
            !name.startsWith("EXAMPLE-")
        );

        if (javaFiles == null || javaFiles.length == 0) {
            return result;
        }

        plugin.getLogger().info("[JavaCompiler] Found " + javaFiles.length + " .java files");

        int skipped = 0;
        int compiled = 0;

        for (File javaFile : javaFiles) {
            // Проверяем, изменился ли файл
            String currentHash = getFileHash(javaFile);
            String cachedHash = fileHashes.get(javaFile.getName());
            
            if (currentHash.equals(cachedHash)) {
                // Файл не изменился - пропускаем компиляцию
                String generatedClassName = javaFile.getName().replace(".java", "").replace("-", "_").replace(" ", "_") + "Item";
                String fullClassName = PACKAGE + "." + generatedClassName;
                
                // Пытаемся загрузить из кэша
                byte[] cachedBytes = loadFromCache(fullClassName);
                if (cachedBytes != null) {
                    compiledClasses.put(fullClassName, cachedBytes);
                    ClassType classType = detectClassType(readFileContent(javaFile));
                    addToResult(result, fullClassName, classType);
                    skipped++;
                    continue;
                }
            }

            // Компилируем файл
            if (compileFile(javaFile, result)) {
                compiled++;
                fileHashes.put(javaFile.getName(), currentHash);
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
                plugin.getLogger().severe("[JavaCompiler] Error loading classes: " + e.getMessage());
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
            String generatedClassName = fileName.replace("-", "_").replace(" ", "_") + "Item";
            sourceCode = replaceClassName(sourceCode, generatedClassName);

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
            plugin.getLogger().severe("[JavaCompiler] Error: " + javaFile.getName() + ": " + e.getMessage());
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
        if (sourceCode.contains("extends AbstractCustomItem")) return ClassType.ITEM;
        if (sourceCode.contains("extends CustomCommand")) return ClassType.COMMAND;
        if (sourceCode.contains("extends CustomPlaceholder")) return ClassType.PLACEHOLDER;
        return ClassType.ITEM;
    }

    private String replaceClassName(String sourceCode, String newClassName) {
        return sourceCode.replaceAll("public\\s+class\\s+\\w+", "public class " + newClassName);
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

    private byte[] loadFromCache(String className) {
        try {
            Path cacheFile = cacheDir.resolve(className.replace('.', '/') + ".class");
            if (Files.exists(cacheFile)) {
                return Files.readAllBytes(cacheFile);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void saveToCache(String className, byte[] bytes) {
        try {
            Path cacheFile = cacheDir.resolve(className.replace('.', '/') + ".class");
            Files.createDirectories(cacheFile.getParent());
            Files.write(cacheFile, bytes);
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

        String[] serverJarPaths = {"server.jar", "paper.jar", "../server.jar", "../paper.jar"};
        for (String path : serverJarPaths) {
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
        classLoader = null;
    }

    public boolean isCompiling() {
        return compiling.get();
    }

    // ===== КЛАССЫ =====

    public enum ClassType { ITEM, COMMAND, PLACEHOLDER }

    public static class CompileResult {
        public int compiled = 0;
        public List<String> items = new ArrayList<>();
        public List<String> commands = new ArrayList<>();
        public List<String> placeholders = new ArrayList<>();
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
