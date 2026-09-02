package me.dcplugin.dcustomitems.api.modules;

import me.dcplugin.dcustomitems.Main;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.*;

/**
 * Менеджер модулей
 * 
 * Загружает модули из подпапок в items/
 * 
 * Структура:
 * items/
 * ├── shop/
 * │   ├── config.yml
 * │   ├── items.yml
 * │   └── shop.java
 * └── arena/
 *     ├── config.yml
 *     └── arena.java
 */
public class ModuleManager {

    private final Main plugin;
    private final File modulesDir;
    private final Map<String, Module> modules = new LinkedHashMap<>();

    public ModuleManager(Main plugin) {
        this.plugin = plugin;
        this.modulesDir = new File(plugin.getDataFolder(), "items");
    }

    /**
     * Загрузить все модули из подпапок
     */
    public void loadAll() {
        unloadAll();
        
        if (!modulesDir.exists()) {
            modulesDir.mkdirs();
            return;
        }

        // Ищем подпапки (каждая подпапка = модуль)
        File[] subfolders = modulesDir.listFiles(File::isDirectory);
        if (subfolders == null || subfolders.length == 0) {
            return;
        }

        plugin.getLogger().info("[Modules] Found " + subfolders.length + " potential modules");

        for (File folder : subfolders) {
            try {
                loadModule(folder);
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "[Modules] Error loading module: " + folder.getName() + " - " + e.getMessage(), e);
            }
        }

        plugin.getLogger().info("[Modules] Loaded " + modules.size() + " modules");
    }

    /**
     * Загрузить модуль из папки
     */
    private void loadModule(File folder) {
        String moduleId = folder.getName();
        
        // Проверяем есть ли config.yml
        File configFile = new File(folder, "config.yml");
        if (!configFile.exists()) {
            // Создаём дефолтный конфиг
            createDefaultConfig(folder, moduleId);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String moduleName = config.getString("name", moduleId);
        
        plugin.getLogger().info("[Modules] Loading module: " + moduleName + " (" + moduleId + ")");

        // Java-модули уже скомпилированы ItemRegistry вместе с командами.
        Module javaModule = plugin.getApiItemRegistry() == null
            ? null : plugin.getApiItemRegistry().getModule(moduleId);
        if (javaModule != null) {
            modules.put(moduleId.toLowerCase(), javaModule);
            if (config.getBoolean("enabled", true)) javaModule.enable();
            return;
        }

        // Если Java-модуля нет, поддерживаем простой YAML-модуль.
        loadSimpleModule(folder, moduleId, config);
    }

    /**
     * Загрузить простой YAML модуль
     */
    private void loadSimpleModule(File folder, String moduleId, YamlConfiguration config) {
        SimpleModule module = new SimpleModule(plugin, moduleId, folder, config);
        modules.put(moduleId, module);
        module.enable();
    }

    /**
     * Создать дефолтный конфиг
     */
    private void createDefaultConfig(File folder, String moduleId) {
        try {
            File configFile = new File(folder, "config.yml");
            YamlConfiguration config = new YamlConfiguration();
            
            config.set("name", moduleId);
            config.set("version", "1.0");
            config.set("enabled", true);
            config.set("commands", Collections.emptyList());
            config.set("permissions", Collections.emptyList());
            
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                "[Modules] Error creating config: " + e.getMessage(), e);
        }
    }

    /**
     * Выгрузить все модули
     */
    public void unloadAll() {
        for (Module module : modules.values()) {
            try {
                module.disable();
            } catch (Exception e) {
                plugin.getLogger().warning("[Modules] Error disabling: " + module.getId());
            }
        }
        modules.clear();
    }

    /**
     * Перезагрузить все модули
     */
    public void reloadAll() {
        unloadAll();
        loadAll();
    }

    /**
     * Получить модуль по ID
     */
    public Module getModule(String id) {
        return modules.get(id.toLowerCase());
    }

    /**
     * Получить все модули
     */
    public Map<String, Module> getAllModules() {
        return Collections.unmodifiableMap(modules);
    }

    /**
     * Количество модулей
     */
    public int getModuleCount() {
        return modules.size();
    }
}
