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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final long MAX_FILE_BYTES = 512 * 1024;
    private static final int MAX_FILES = 300;
    private static final String[] DANGEROUS_TOKENS = {
        "System.exit", "Runtime.getRuntime().exec", "ProcessBuilder", ".setAccessible("
    };

    /**
     * Импорты, добавляемые автоматически, если их нет в исходнике.
     * Проверяются поимённо: раньше блок импортов добавлялся только если в
     * файле вообще не было строки "import me.dcplugin.dcustomitems.api",
     * и файл с одним импортом (например RecipeDef) не компилировался.
     */
    private static final String[] DEFAULT_IMPORTS = {
        "me.dcplugin.dcustomitems.api.AbstractCustomItem",
        "me.dcplugin.dcustomitems.api.ItemAPI",
        "me.dcplugin.dcustomitems.api.commands.CustomCommand",
        "me.dcplugin.dcustomitems.api.placeholders.CustomPlaceholder"
    };

    /** Скомпилированные шаблоны замены имён классов (по одному на алиас). */
    private final List<Map.Entry<Pattern, String>> aliasPatterns = new ArrayList<>();

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
        moduleSourceFolders.clear();
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
        if (javaFiles.size() > MAX_FILES) {
            plugin.getLogger().severe("[JavaCompiler] Слишком много .java файлов: " + javaFiles.size() + " (лимит " + MAX_FILES + "). Проверьте items/.");
            result.errors.add("Too many .java files: " + javaFiles.size());
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

        // Шаблоны замены имён компилируются один раз, а не на каждый файл
        // (раньше Pattern.compile вызывался для каждого алиаса в каждом файле).
        buildAliasPatterns();

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
        // Новый загрузчик создаём всегда: иначе после удаления всех .java
        // файлов оставался старый classLoader со старыми классами.
        CustomClassLoader loader = new CustomClassLoader(getClass().getClassLoader());
        for (Map.Entry<String, byte[]> entry : compiledClasses.entrySet()) {
            loader.addClass(entry.getKey(), entry.getValue());
        }
        classLoader = loader;

        long elapsed = System.currentTimeMillis() - startTime;
        plugin.getLogger().info("[JavaCompiler] Compiled: " + compiled + ", Skipped: " + skipped + " (" + elapsed + "ms)");

        return result;
    }

    private boolean compileFile(File javaFile, CompileResult result) {
        try {
            if (javaFile.length() > MAX_FILE_BYTES) {
                String err = javaFile.getName() + ": файл >512KB — отклонён (защита от перегрузки компилятора)";
                plugin.getLogger().warning("[JavaCompiler] ❌ " + err);
                result.errors.add(err);
                return false;
            }
            String sourceCode = readFileContent(javaFile);
            // Компилятор добавляет своё объявление package — собственное
            // объявление пользователя нужно убрать, иначе в исходнике
            // окажется два package и javac откажется его собирать.
            sourceCode = stripPackageDeclaration(sourceCode);
            for (String token : DANGEROUS_TOKENS) {
                if (sourceCode.contains(token)) {
                    plugin.getLogger().warning("[JavaCompiler] ⚠ " + javaFile.getName() + " содержит опасный вызов '" + token + "' — проверьте файл вручную (java-compilation исполняет произвольный код).");
                    break;
                }
            }
            String fileName = javaFile.getName().replace(".java", "");

            // Добавляем недостающие импорты API плагина
            sourceCode = injectDefaultImports(sourceCode);

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

            try {
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
            } finally {
                // FileManager держит ресурсы — закрываем всегда, иначе при
                // исключении внутри компиляции оставались открытые дескрипторы.
                try {
                    fileManager.close();
                } catch (Exception ignored) {
                    // Закрытие не должно ломать компиляцию
                }
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
        if (aliasPatterns.isEmpty()) return sourceCode;
        String result = sourceCode;
        for (Map.Entry<Pattern, String> alias : aliasPatterns) {
            result = alias.getKey().matcher(result).replaceAll(Matcher.quoteReplacement(alias.getValue()));
        }
        return result;
    }

    /**
     * Компилирует шаблоны замены имён классов (длинные имена — первыми,
     * чтобы "Foo" не съел "FooBar").
     */
    private void buildAliasPatterns() {
        aliasPatterns.clear();
        List<Map.Entry<String, String>> aliases = new ArrayList<>(sourceClassAliases.entrySet());
        aliases.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
        for (Map.Entry<String, String> alias : aliases) {
            aliasPatterns.add(Map.entry(
                    Pattern.compile("\\b" + Pattern.quote(alias.getKey()) + "\\b"),
                    alias.getValue()));
        }
    }

    /**
     * Добавляет недостающие импорты API плагина.
     *
     * Проверка идёт поимённо: если в файле есть любой импорт из пакета
     * api, старый код не добавлял остальные — и файл не компилировался.
     *
     * Импорты вставляются ПОСЛЕ объявления package (если оно есть):
     * в начало файла они попасть не могут — javac требует, чтобы
     * package был первым оператором.
     */
    private String injectDefaultImports(String sourceCode) {
        List<String> imports = new ArrayList<>();
        for (String fqcn : DEFAULT_IMPORTS) {
            String simpleName = fqcn.substring(fqcn.lastIndexOf('.') + 1);
            // Если пользователь объявил одноимённый класс, импорт API ему не нужен
            // (и после подмены имён он бы сломался).
            if (sourceClassAliases.containsKey(simpleName)) continue;
            if (sourceCode.contains("import " + fqcn + ";")) continue;
            imports.add(fqcn);
        }
        return insertImports(sourceCode, imports);
    }

    /**
     * Вставляет импорты в исходник.
     *
     * Импорты вставляются ПОСЛЕ объявления package (если оно есть):
     * в начало файла они попасть не могут — javac требует, чтобы
     * package был первым оператором.
     */
    static String insertImports(String sourceCode, List<String> imports) {
        if (imports == null || imports.isEmpty()) return sourceCode;

        StringBuilder block = new StringBuilder();
        for (String fqcn : imports) {
            block.append("import ").append(fqcn).append(";\n");
        }

        int insertAt = findPackageDeclarationEnd(sourceCode);
        return sourceCode.substring(0, insertAt) + block + sourceCode.substring(insertAt);
    }

    /**
     * Удаляет объявление package из исходника (компилятор добавляет своё).
     *
     * Учитывает BOM (файл в UTF-8 с BOM начинается с U+FEFF) и не задевает
     * строки в блочных комментариях: package должен быть первой строкой
     * файла, а не случайным словом в тексте.
     */
    static String stripPackageDeclaration(String sourceCode) {
        String source = sourceCode;
        if (!source.isEmpty() && source.charAt(0) == '\uFEFF') {
            source = source.substring(1);
        }
        // Якорь начала строки + только пробелы/табы до "package":
        // многострочный \s* мог бы захватить строку внутри комментария.
        return source.replaceFirst("(?m)^[ \\t]*package[ \\t]+[\\w.]+[ \\t]*;[^\\S\\n]*\\n?", "");
    }

    /**
     * Возвращает индекс символа сразу после объявления package (и его перевода строки).
     * Если объявления нет — 0.
     */
    static int findPackageDeclarationEnd(String sourceCode) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?m)^\\s*package\\s+[\\w.]+\\s*;")
                .matcher(sourceCode);
        if (!matcher.find()) return 0;
        int end = matcher.end();
        // Переносим вставку на следующую строку, чтобы не приклеивать импорт
        // к точке с запятой.
        int newline = sourceCode.indexOf('\n', end);
        return newline >= 0 ? newline + 1 : end;
    }

    private List<File> findJavaFiles(File directory) {
        List<File> files = new ArrayList<>();
        File[] children = directory.listFiles();
        if (children == null) return files;
        Arrays.sort(children, Comparator.comparing(File::getPath));
        for (File child : children) {
            if (child.isDirectory()) {
                // Образцы и справочные папки (EXAMPLE-*, EXAMPLES/, _template/)
                // не компилируются — это справочный материал.
                String name = child.getName();
                if (name.startsWith("EXAMPLE-")
                        || name.equalsIgnoreCase("EXAMPLES")
                        || name.equalsIgnoreCase("_template")) continue;
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

    /**
     * Имя генерируемого класса по пути файла.
     *
     * Имя должно быть корректным Java-идентификатором: раньше файл
     * "1abc.java" давал класс "1abcItem", а "my.item.java" — "my.itemItem",
     * и такой исходник не компилировался.
     */
    public String getGeneratedClassName(File javaFile) {
        String relative;
        try {
            relative = itemsDir.relativize(javaFile.toPath()).toString();
        } catch (Exception e) {
            relative = javaFile.getName();
        }
        return sanitizeGeneratedClassName(relative);
    }

    /**
     * Превращает относительный путь исходника в корректный Java-идентификатор.
     */
    static String sanitizeGeneratedClassName(String relativePath) {
        String base = relativePath == null ? "" : relativePath.replace('\\', '_').replace('/', '_');
        if (base.toLowerCase(Locale.ROOT).endsWith(".java")) {
            base = base.substring(0, base.length() - ".java".length());
        }

        StringBuilder sb = new StringBuilder(base.length() + 5);
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            boolean valid = Character.isLetterOrDigit(c) || c == '_';
            sb.append(valid ? c : '_');
        }
        sb.append("Item");

        // Java-идентификатор не может начинаться с цифры
        if (sb.length() > 0 && Character.isDigit(sb.charAt(0))) {
            sb.insert(0, '_');
        }
        return sb.toString();
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
        aliasPatterns.clear();
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
