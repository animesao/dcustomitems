package me.dcplugin.dcustomitems.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Реестр кастомных предметов, загружаемых из JAR файлов.
 *
 * Предметы загружаются из папки plugins/YourPlugin/items/
 * Каждый JAR файл сканируется на наличие классов, наследующих AbstractCustomItem.
 *
 * Пример структуры:
 * plugins/DC-CustomItems/
 *   items/
 *     my-items.jar          <-- JAR с кастомными предметами
 *       com/example/
 *         FireSword.class   <-- extends AbstractCustomItem
 *         IceStaff.class    <-- extends AbstractCustomItem
 */
public class ItemRegistry {

    private final JavaPlugin plugin;
    private final Map<String, AbstractCustomItem> registeredItems;
    private final List<Class<? extends AbstractCustomItem>> itemClasses;

    public ItemRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.registeredItems = new LinkedHashMap<>();
        this.itemClasses = new ArrayList<>();
    }

    /**
     * Загружает все предметы из папки items/.
     */
    public void loadAll() {
        File itemsDir = new File(plugin.getDataFolder(), "items");
        if (!itemsDir.exists()) {
            itemsDir.mkdirs();
            plugin.getLogger().info("[API] Создана папка items/ для Java API предметов");
            return;
        }

        File[] jarFiles = itemsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            plugin.getLogger().info("[API] Папка items/ пуста. Добавьте JAR файлы с предметами.");
            return;
        }

        for (File jarFile : jarFiles) {
            loadFromJar(jarFile);
        }

        plugin.getLogger().info("[API] Загружено " + registeredItems.size() + " Java API предметов из " + jarFiles.length + " JAR файлов");
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

            // Используем URLClassLoader для загрузки классов из JAR
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
                        itemClasses.add(itemClass);

                        plugin.getLogger().info("[API] Загружен предмет: " + item.getId() + " (" + item.getDisplayName() + ")");
                    }
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | java.lang.reflect.InvocationTargetException | NoSuchMethodException e) {
                    plugin.getLogger().warning("[API] Не удалось загрузить класс " + className + ": " + e.getMessage());
                }
            }

        } catch (IOException e) {
            plugin.getLogger().severe("[API] Ошибка при чтении JAR файла " + jarFile.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Регистрирует предмет программно (без JAR файла).
     */
    public void register(AbstractCustomItem item) {
        registeredItems.put(item.getId(), item);
        plugin.getLogger().info("[API] Зарегистрирован предмет: " + item.getId());
    }

    /**
     * Получает предмет по ID.
     */
    public AbstractCustomItem getItem(String id) {
        return registeredItems.get(id);
    }

    /**
     * Получает все зарегистрированные предметы.
     */
    public Map<String, AbstractCustomItem> getAllItems() {
        return Collections.unmodifiableMap(registeredItems);
    }

    /**
     * Получает список всех ID предметов.
     */
    public Set<String> getAllIds() {
        return registeredItems.keySet();
    }

    /**
     * Проверяет, зарегистрирован ли предмет.
     */
    public boolean isRegistered(String id) {
        return registeredItems.containsKey(id);
    }

    /**
     * Получает количество загруженных предметов.
     */
    public int getCount() {
        return registeredItems.size();
    }
}
