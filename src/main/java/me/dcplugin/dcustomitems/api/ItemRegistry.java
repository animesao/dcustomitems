package me.dcplugin.dcustomitems.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Реестр кастомных предметов.
 *
 * Поддерживает 3 типа загрузки:
 * 1. YAML файлы (.yml) — через CustomItemHandler
 * 2. JAR файлы (.jar) — через API
 * 3. Java файлы (.java) — runtime компиляция!
 *
 * Пример структуры:
 * plugins/DC-CustomItems/
 *   items/
 *     my-items.jar          <-- JAR с предметами
 *     vampire-blade.yml     <-- YAML предмет
 *     dark-sword.java       <-- Java предмет (runtime компиляция!)
 *     ice-staff.java        <-- Ещё один Java предмет
 */
public class ItemRegistry {

    private final JavaPlugin plugin;
    private final Map<String, AbstractCustomItem> registeredItems;
    private final JavaItemCompiler compiler;

    public ItemRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.registeredItems = new LinkedHashMap<>();
        this.compiler = new JavaItemCompiler(plugin);
    }

    /**
     * Загружает все предметы из папки items/.
     */
    public void loadAll() {
        registeredItems.clear();

        File itemsDir = new File(plugin.getDataFolder(), "items");
        if (!itemsDir.exists()) {
            itemsDir.mkdirs();
            plugin.getLogger().info("[API] Создана папка items/ для кастомных предметов");
            return;
        }

        // 1. Загружаем JAR файлы
        loadJarFiles(itemsDir);

        // 2. Компилируем и загружаем .java файлы
        loadJavaFiles(itemsDir);

        plugin.getLogger().info("[API] Загружено " + registeredItems.size() + " Java API предметов");
    }

    /**
     * Перезагружает все предметы (для /ci reload).
     */
    public void reload() {
        registeredItems.clear();
        compiler.clear();
        loadAll();
    }

    /**
     * Загружает JAR файлы из папки.
     */
    private void loadJarFiles(File itemsDir) {
        File[] jarFiles = itemsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) return;

        for (File jarFile : jarFiles) {
            loadFromJar(jarFile);
        }
    }

    /**
     * Загружает .java файлы из папки.
     */
    private void loadJavaFiles(File itemsDir) {
        File[] javaFiles = itemsDir.listFiles((dir, name) ->
            name.endsWith(".java") && !name.startsWith("_")
        );
        if (javaFiles == null || javaFiles.length == 0) return;

        plugin.getLogger().info("[API] Найдено " + javaFiles.length + " .java файлов, компиляция...");

        int compiled = compiler.compileAll();

        if (compiled > 0) {
            // Все скомпилированные классы в пакете items
            // Ищем классы наследующие AbstractCustomItem
            for (Map.Entry<String, byte[]> entry : compiler.getCompiledClasses().entrySet()) {
                String fullClassName = entry.getKey();
                try {
                    Class<?> clazz = compiler.loadClass(fullClassName);
                    if (clazz != null && AbstractCustomItem.class.isAssignableFrom(clazz)) {
                        AbstractCustomItem item = (AbstractCustomItem) clazz.getDeclaredConstructor().newInstance();
                        registeredItems.put(item.getId(), item);
                        plugin.getLogger().info("[API] ✅ Загружен .java предмет: " + item.getId() +
                            " (" + item.getDisplayName() + ")");
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[API] ❌ Не удалось загрузить " + fullClassName + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Определяет пакет из расположения файла.
     * Если файл в items/sword/dark.java → package items.sword
     */
    private String inferPackageName(File javaFile) {
        Path itemsPath = new File(plugin.getDataFolder(), "items").toPath();
        Path filePath = javaFile.toPath();
        Path relativePath = itemsPath.relativize(filePath.getParent() != null ? filePath.getParent() : filePath);

        String packageName = relativePath.toString()
            .replace(File.separator, ".")
            .replace("\\", ".");

        // Убираем начальную точку
        if (packageName.startsWith(".")) {
            packageName = packageName.substring(1);
        }

        return packageName.isEmpty() ? "items" : "items." + packageName;
    }

    /**
     * Загружает предметы из одного JAR файла.
     */
    private void loadFromJar(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            List<String> classNames = new ArrayList<>();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    String className = entry.getName()
                        .replace("/", ".")
                        .replace("\\", ".")
                        .replace(".class", "");
                    classNames.add(className);
                }
            }

            URL jarUrl = jarFile.toURI().toURL();
            ClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl}, plugin.getClass().getClassLoader());

            for (String className : classNames) {
                try {
                    Class<?> clazz = Class.forName(className, false, classLoader);
                    if (AbstractCustomItem.class.isAssignableFrom(clazz) && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                        @SuppressWarnings("unchecked")
                        Class<? extends AbstractCustomItem> itemClass = (Class<? extends AbstractCustomItem>) clazz;

                        AbstractCustomItem item = itemClass.getDeclaredConstructor().newInstance();
                        registeredItems.put(item.getId(), item);

                        plugin.getLogger().info("[API] Загружен JAR предмет: " + item.getId());
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[API] Не удалось загрузить " + className + ": " + e.getMessage());
                }
            }

        } catch (IOException e) {
            plugin.getLogger().severe("[API] Ошибка чтения JAR " + jarFile.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Регистрирует предмет программно.
     */
    public void register(AbstractCustomItem item) {
        registeredItems.put(item.getId(), item);
        plugin.getLogger().info("[API] Зарегистрирован: " + item.getId());
    }

    /**
     * Получает предмет по ID.
     */
    public AbstractCustomItem getItem(String id) {
        return registeredItems.get(id);
    }

    /**
     * Получает все предметы.
     */
    public Map<String, AbstractCustomItem> getAllItems() {
        return Collections.unmodifiableMap(registeredItems);
    }

    /**
     * Получает все ID.
     */
    public Set<String> getAllIds() {
        return registeredItems.keySet();
    }

    /**
     * Проверяет регистрацию.
     */
    public boolean isRegistered(String id) {
        return registeredItems.containsKey(id);
    }

    /**
     * Количество предметов.
     */
    public int getCount() {
        return registeredItems.size();
    }

    /**
     * Получает компилятор.
     */
    public JavaItemCompiler getCompiler() {
        return compiler;
    }
}
