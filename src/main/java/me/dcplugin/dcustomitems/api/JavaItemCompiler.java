package me.dcplugin.dcustomitems.api;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import me.dcplugin.dcustomitems.api.placeholders.CustomPlaceholder;
import org.bukkit.plugin.java.JavaPlugin;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime компилятор для .java файлов
 * 
 * Поддерживает:
 * - AbstractCustomItem (предметы)
 * - CustomCommand (команды)
 * - CustomPlaceholder (плейсхолдеры)
 */
public class JavaItemCompiler {

    private final Main plugin;
    private final Path itemsDir;
    private final Path compiledDir;
    private final Map<String, byte[]> compiledClasses = new ConcurrentHashMap<>();
    private CustomClassLoader classLoader;
    
    // Package для компиляции
    private static final String PACKAGE = "items";

    public JavaItemCompiler(Main plugin) {
        this.plugin = plugin;
        this.itemsDir = plugin.getDataFolder().toPath().resolve("items");
        this.compiledDir = plugin.getDataFolder().toPath().resolve("compiled");
    }

    /**
     * Компилирует все .java файлы из папки items/
     */
    public CompileResult compileAll() {
        compiledClasses.clear();
        CompileResult result = new CompileResult();

        try {
            Files.createDirectories(compiledDir);
        } catch (IOException e) {
            plugin.getLogger().severe("[JavaCompiler] Cannot create compiled/: " + e.getMessage());
            return result;
        }

        // Ищем все .java файлы (кроме базовых классов и EXAMPLE)
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

        for (File javaFile : javaFiles) {
            if (compileFile(javaFile, result)) {
                result.compiled++;
            }
        }

        // Загружаем классы с правильным classloader
        if (result.compiled > 0) {
            try {
                classLoader = new CustomClassLoader(getClass().getClassLoader());
                for (Map.Entry<String, byte[]> entry : compiledClasses.entrySet()) {
                    classLoader.addClass(entry.getKey(), entry.getValue());
                }
                plugin.getLogger().info("[JavaCompiler] Loaded " + compiledClasses.size() + " classes into classloader");
            } catch (Exception e) {
                plugin.getLogger().severe("[JavaCompiler] Error loading classes: " + e.getMessage());
            }
        }

        return result;
    }

    private boolean compileFile(File javaFile, CompileResult result) {
        try {
            String sourceCode = new String(Files.readAllBytes(javaFile.toPath()), StandardCharsets.UTF_8);
            String fileName = javaFile.getName().replace(".java", "");

            // Добавляем импорты если нет
            if (!sourceCode.contains("import me.dcplugin.dcustomitems.api")) {
                sourceCode = "import me.dcplugin.dcustomitems.api.AbstractCustomItem;\n" +
                             "import me.dcplugin.dcustomitems.api.ItemAPI;\n" +
                             "import me.dcplugin.dcustomitems.api.commands.CustomCommand;\n" +
                             "import me.dcplugin.dcustomitems.api.placeholders.CustomPlaceholder;\n" +
                             sourceCode;
            }

            // Извлекаем имя класса
            String className = extractClassName(sourceCode);
            if (className == null) {
                plugin.getLogger().warning("[JavaCompiler] Cannot find class name in " + javaFile.getName());
                result.errors.add(javaFile.getName() + ": no class found");
                return false;
            }

            // Проверяем тип класса
            ClassType classType = detectClassType(sourceCode);

            // Генерируем имя класса на основе имени файла
            String generatedClassName = fileName.replace("-", "_").replace(" ", "_") + "Item";
            
            // Переименовываем класс в исходнике
            sourceCode = replaceClassName(sourceCode, generatedClassName);
            
            // Добавляем package
            final String finalSourceCode = "package " + PACKAGE + ";\n" + sourceCode;
            
            // Полное имя класса с package
            final String fullClassName = PACKAGE + "." + generatedClassName;

            plugin.getLogger().info("[JavaCompiler] Compiling " + javaFile.getName() + " -> " + fullClassName);

            // Компилируем
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
                                    plugin.getLogger().info("[JavaCompiler] Compiled class: " + className);
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
                plugin.getLogger().info("[JavaCompiler] Compiled classes: " + classBytesHolder.keySet());
                compiledClasses.putAll(classBytesHolder);

                // Определяем тип и добавляем в результат (с полным именем package.class)
                switch (classType) {
                    case ITEM:
                        result.items.add(fullClassName);
                        plugin.getLogger().info("[JavaCompiler] ✅ Item: " + javaFile.getName() + " -> " + fullClassName);
                        break;
                    case COMMAND:
                        result.commands.add(fullClassName);
                        plugin.getLogger().info("[JavaCompiler] ✅ Command: " + javaFile.getName() + " -> " + fullClassName);
                        break;
                    case PLACEHOLDER:
                        result.placeholders.add(fullClassName);
                        plugin.getLogger().info("[JavaCompiler] ✅ Placeholder: " + javaFile.getName() + " -> " + fullClassName);
                        break;
                }

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

    private ClassType detectClassType(String sourceCode) {
        if (sourceCode.contains("extends AbstractCustomItem")) return ClassType.ITEM;
        if (sourceCode.contains("extends CustomCommand")) return ClassType.COMMAND;
        if (sourceCode.contains("extends CustomPlaceholder")) return ClassType.PLACEHOLDER;
        return ClassType.ITEM;
    }

    private String extractClassName(String sourceCode) {
        String[] lines = sourceCode.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("public class ")) {
                String afterClass = line.substring("public class ".length()).trim();
                int spaceIdx = afterClass.indexOf(' ');
                int braceIdx = afterClass.indexOf('{');
                int endIdx = Math.min(
                    spaceIdx > 0 ? spaceIdx : afterClass.length(),
                    braceIdx > 0 ? braceIdx : afterClass.length()
                );
                return afterClass.substring(0, endIdx).trim();
            }
        }
        return null;
    }

    private String replaceClassName(String sourceCode, String newClassName) {
        // Заменяем public class OldName на public class NewName
        return sourceCode.replaceAll("public\\s+class\\s+\\w+", "public class " + newClassName);
    }

    private String getClasspath() {
        StringBuilder classpath = new StringBuilder();

        // Добавляем jar плагина
        try {
            File pluginFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            classpath.append(pluginFile.getAbsolutePath());
        } catch (Exception ignored) {
            classpath.append("plugins/DC-CustomItems.jar");
        }

        // Добавляем server jar
        String[] serverJarPaths = {"server.jar", "paper.jar", "../server.jar", "../paper.jar"};
        for (String path : serverJarPaths) {
            File serverJar = new File(path);
            if (serverJar.exists()) {
                classpath.append(File.pathSeparator).append(serverJar.getAbsolutePath());
                break;
            }
        }

        // Добавляем библиотеки
        String[] libPaths = {"libraries", "../libraries"};
        for (String libPath : libPaths) {
            File libsDir = new File(libPath);
            if (libsDir.exists() && libsDir.isDirectory()) {
                addJarsFromClassDir(libsDir, classpath);
            }
        }

        // Добавляем текущую директорию для скомпилированных классов
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

    // ===== МЕТОДЫ ЗАГРУЗКИ =====

    public Class<?> loadClass(String fullClassName) {
        if (classLoader == null) {
            plugin.getLogger().severe("[JavaCompiler] ClassLoader is null!");
            return null;
        }
        
        plugin.getLogger().info("[JavaCompiler] Loading class: " + fullClassName);
        plugin.getLogger().info("[JavaCompiler] Available classes: " + compiledClasses.keySet());
        
        try {
            return classLoader.findClass(fullClassName);
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("[JavaCompiler] Class not found: " + fullClassName);
            return null;
        }
    }

    public AbstractCustomItem createItemInstance(String fullClassName) {
        try {
            Class<?> clazz = loadClass(fullClassName);
            if (clazz == null) {
                return null;
            }
            if (AbstractCustomItem.class.isAssignableFrom(clazz)) {
                AbstractCustomItem item = (AbstractCustomItem) clazz.getDeclaredConstructor().newInstance();
                plugin.getLogger().info("[JavaCompiler] Created item instance: " + item.getId());
                return item;
            } else {
                plugin.getLogger().warning("[JavaCompiler] " + fullClassName + " does not extend AbstractCustomItem");
                return null;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[JavaCompiler] Error creating item: " + fullClassName + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
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
        classLoader = null;
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
