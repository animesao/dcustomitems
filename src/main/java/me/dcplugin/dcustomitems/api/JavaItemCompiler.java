package me.dcplugin.dcustomitems.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime компилятор для .java файлов.
 *
 * Позволяет загружать Java предметы без Maven/JAR.
 * Просто положи .java файл в папку items/ и сделай /ci reload.
 *
 * Пример файла items/my_sword.java:
 * <pre>
 * import me.dcplugin.dcustomitems.api.AbstractCustomItem;
 * import me.dcplugin.dcustomitems.api.ItemAPI;
 * import org.bukkit.Material;
 * import org.bukkit.Particle;
 * import org.bukkit.Sound;
 * import org.bukkit.entity.Player;
 * import org.bukkit.event.player.PlayerInteractEvent;
 *
 * public class MySword extends AbstractCustomItem {
 *     public String getId() { return "my_sword"; }
 *     public String getDisplayName() { return "&6Мой Меч"; }
 *     public Material getMaterial() { return Material.DIAMOND_SWORD; }
 *     public void onRightClick(PlayerInteractEvent e, Player p) {
 *         ItemAPI.heal(p, 5);
 *     }
 * }
 * </pre>
 */
public class JavaItemCompiler {

    private final JavaPlugin plugin;
    private final Path itemsDir;
    private final Path compiledDir;
    private final Map<String, byte[]> compiledClasses = new ConcurrentHashMap<>();

    // Класс-загрузчик для скомпилированных классов
    private CustomClassLoader classLoader;

    public JavaItemCompiler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.itemsDir = plugin.getDataFolder().toPath().resolve("items");
        this.compiledDir = plugin.getDataFolder().toPath().resolve("compiled");
        this.classLoader = new CustomClassLoader(getClass().getClassLoader());
    }

    /**
     * Компилирует все .java файлы из папки items/.
     * Возвращает количество успешно скомпилированных классов.
     */
    public int compileAll() {
        compiledClasses.clear();

        // Создаём папку для скомпилированных классов
        try {
            Files.createDirectories(compiledDir);
        } catch (IOException e) {
            plugin.getLogger().severe("[JavaCompiler] Не удалось создать папку compiled/: " + e.getMessage());
            return 0;
        }

        // Ищем все .java файлы
        File[] javaFiles = itemsDir.toFile().listFiles((dir, name) ->
            name.endsWith(".java") && !name.startsWith("Abstract") && !name.startsWith("Item")
        );

        if (javaFiles == null || javaFiles.length == 0) {
            return 0;
        }

        plugin.getLogger().info("[JavaCompiler] Найдено " + javaFiles.length + " .java файлов");

        int compiled = 0;
        for (File javaFile : javaFiles) {
            if (compileFile(javaFile)) {
                compiled++;
            }
        }

        // Создаём новый ClassLoader со скомпилированными классами
        if (compiled > 0) {
            try {
                classLoader = new CustomClassLoader(getClass().getClassLoader());
                for (Map.Entry<String, byte[]> entry : compiledClasses.entrySet()) {
                    classLoader.addClass(entry.getKey(), entry.getValue());
                }
                plugin.getLogger().info("[JavaCompiler] Загружено " + compiled + " классов");
            } catch (Exception e) {
                plugin.getLogger().severe("[JavaCompiler] Ошибка загрузки классов: " + e.getMessage());
            }
        }

        return compiled;
    }

    /**
     * Компилирует один .java файл.
     */
    private boolean compileFile(File javaFile) {
        try {
            String sourceCode = new String(Files.readAllBytes(javaFile.toPath()), StandardCharsets.UTF_8);
            String fileName = javaFile.getName().replace(".java", "");

            // Добавляем импорт API если нет
            if (!sourceCode.contains("import me.dcplugin.dcustomitems.api")) {
                sourceCode = "import me.dcplugin.dcustomitems.api.AbstractCustomItem;\n" +
                             "import me.dcplugin.dcustomitems.api.ItemAPI;\n" +
                             sourceCode;
            }

            // Извлекаем имя класса из файла
            String className = extractClassName(sourceCode);
            if (className == null) {
                plugin.getLogger().warning("[JavaCompiler] Не удалось найти имя класса в " + javaFile.getName());
                return false;
            }

            // Если имя класса не совпадает с именем файла, добавляем package
            String packageName = "items";
            if (!fileName.equals(className.toLowerCase())) {
                // Переименовываем класс чтобы избежать конфликта имён
                // Используем fileName как основу для уникального имени
                className = fileName.replace("-", "_").replace(" ", "_") + "Item";
                sourceCode = replaceClassName(sourceCode, className);
            }

            final String finalClassName = className;
            final String fullClassName = packageName + "." + className;
            final String finalSourceCode = "package " + packageName + ";\n" + sourceCode;

            // Компилируем
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                plugin.getLogger().severe("[JavaCompiler] JavaCompiler не найден! Убедитесь что JDK установлен.");
                return false;
            }

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

            // Виртуальный файл с исходным кодом
            JavaFileObject source = new SimpleJavaFileObject(
                URI.create("string:///" + className + ".java"),
                JavaFileObject.Kind.SOURCE
            ) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    return finalSourceCode;
                }
            };

            // Виртуальный файл для записи скомпилированных байтов
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

            // Настройки компиляции
            List<String> options = List.of(
                "-classpath", getClasspath(),
                "-d", compiledDir.toString()
            );

            // Компилируем
            JavaCompiler.CompilationTask task = compiler.getTask(
                null, virtualFileManager, diagnostics, options, null, List.of(source)
            );

            boolean success = task.call();
            fileManager.close();

            if (success) {
                // Сохраняем байты классов
                compiledClasses.putAll(classBytesHolder);
                plugin.getLogger().info("[JavaCompiler] ✅ Скомпилирован: " + javaFile.getName() + " -> " + fullClassName);
                return true;
            } else {
                // Выводим ошибки
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    plugin.getLogger().warning("[JavaCompiler] ❌ " + javaFile.getName() +
                        ":" + diagnostic.getLineNumber() + " - " + diagnostic.getMessage(null));
                }
                return false;
            }

        } catch (Exception e) {
            plugin.getLogger().severe("[JavaCompiler] Ошибка компиляции " + javaFile.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Извлекает имя public класса из исходного кода.
     */
    private String extractClassName(String sourceCode) {
        // Ищем public class ...
        String[] lines = sourceCode.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("public class ")) {
                String afterClass = line.substring("public class ".length()).trim();
                // Берём имя до пробела или скобки
                int spaceIdx = afterClass.indexOf(' ');
                int braceIdx = afterClass.indexOf('{');
                int endIdx = Math.min(
                    spaceIdx > 0 ? spaceIdx : afterClass.length(),
                    braceIdx > 0 ? braceIdx : afterClass.length()
                );
                return afterClass.substring(0, endIdx).trim();
            }
        }
        // Ищем просто class ...
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("class ") && !line.contains("extends")) {
                String afterClass = line.substring("class ".length()).trim();
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

    /**
     * Заменяет имя класса в исходном коде.
     */
    private String replaceClassName(String sourceCode, String newClassName) {
        // Заменяем public class OldName на public class NewName
        return sourceCode.replaceAll("public class \\w+", "public class " + newClassName)
                       .replaceAll("class \\w+ extends", "class " + newClassName + " extends");
    }

    /**
     * Получает classpath для компиляции.
     */
    private String getClasspath() {
        StringBuilder classpath = new StringBuilder();

        // Добавляем текущий JAR плагина (содержит AbstractCustomItem, ItemAPI и т.д)
        try {
            File pluginFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            classpath.append(pluginFile.getAbsolutePath()).append(File.pathSeparator);
        } catch (Exception ignored) {}

        // Добавляем серверный класспасс (Paper/Spigot)
        // Ищем в разных местах
        String[] serverJarPaths = {
            "server.jar",
            "paper.jar",
            "../server.jar",
            "../paper.jar",
            "plugins/DC-CustomItems/../../server.jar",
            "/data/server.jar"
        };
        for (String path : serverJarPaths) {
            File serverJar = new File(path);
            if (serverJar.exists()) {
                classpath.append(serverJar.getAbsolutePath()).append(File.pathSeparator);
                break;
            }
        }

        // Добавляем папку libraries/ (Paper хранит зависимости здесь)
        String[] libPaths = {"libraries", "../libraries", "/data/libraries"};
        for (String libPath : libPaths) {
            File libsDir = new File(libPath);
            if (libsDir.exists() && libsDir.isDirectory()) {
                addJarsFromClassDir(libsDir, classpath);
            }
        }

        // Добавляем папку versions/ (Paper хранит версии здесь)
        String[] versionsPaths = {"versions", "../versions", "/data/versions"};
        for (String versionsPath : versionsPaths) {
            File versionsDir = new File(versionsPath);
            if (versionsDir.exists() && versionsDir.isDirectory()) {
                addJarsFromClassDir(versionsDir, classpath);
            }
        }

        // Добавляем все JAR файлы из текущей папки
        File currentDir = new File(".");
        File[] rootJars = currentDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (rootJars != null) {
            for (File jar : rootJars) {
                classpath.append(jar.getAbsolutePath()).append(File.pathSeparator);
            }
        }

        return classpath.toString();
    }

    private void addJarsFromClassDir(File dir, StringBuilder classpath) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    addJarsFromClassDir(file, classpath);
                } else if (file.getName().endsWith(".jar")) {
                    classpath.append(file.getAbsolutePath()).append(File.pathSeparator);
                }
            }
        }
    }

    /**
     * Загружает класс по имени.
     */
    public Class<?> loadClass(String className) {
        try {
            return classLoader.findClass(className);
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("[JavaCompiler] Класс не найден: " + className);
            return null;
        }
    }

    /**
     * Получает экземпляр AbstractCustomItem по имени класса.
     */
    public AbstractCustomItem createItemInstance(String className) {
        try {
            Class<?> clazz = loadClass(className);
            if (clazz != null && AbstractCustomItem.class.isAssignableFrom(clazz)) {
                return (AbstractCustomItem) clazz.getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[JavaCompiler] Ошибка создания экземпляра " + className + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Получает все скомпилированные классы.
     */
    public Map<String, byte[]> getCompiledClasses() {
        return Collections.unmodifiableMap(compiledClasses);
    }

    /**
     * Очищает скомпилированные классы.
     */
    public void clear() {
        compiledClasses.clear();
    }

    /**
     * Кастомный ClassLoader для загрузки скомпилированных классов.
     */
    private static class CustomClassLoader extends ClassLoader {
        private final Map<String, byte[]> classBytes = new HashMap<>();

        CustomClassLoader(ClassLoader parent) {
            super(parent);
        }

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
