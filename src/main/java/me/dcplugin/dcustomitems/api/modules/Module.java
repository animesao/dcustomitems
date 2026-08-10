package me.dcplugin.dcustomitems.api.modules;

import me.dcplugin.dcustomitems.Main;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * Базовый класс для модуля (плагина внутри плагина)
 * 
 * Структура модуля:
 * items/
 * └── shop/
 *     ├── config.yml
 *     ├── items.yml
 *     └── shop.java
 * 
 * В config.yml:
 * name: "Shop"
 * version: "1.0"
 * commands:
 *   - shop
 * permissions:
 *   - shop.admin
 */
public abstract class Module {

    protected final Main plugin;
    protected final String id;
    protected final File folder;
    protected YamlConfiguration config;
    protected YamlConfiguration itemsConfig;
    protected boolean enabled = false;
    
    protected final List<String> commands = new ArrayList<>();
    protected final List<String> permissions = new ArrayList<>();

    public Module(Main plugin, String id, File folder) {
        this.plugin = plugin;
        this.id = id;
        this.folder = folder;
        
        // Загружаем конфиги
        File configFile = new File(folder, "config.yml");
        File itemsFile = new File(folder, "items.yml");
        
        this.config = configFile.exists() ? YamlConfiguration.loadConfiguration(configFile) : new YamlConfiguration();
        this.itemsConfig = itemsFile.exists() ? YamlConfiguration.loadConfiguration(itemsFile) : new YamlConfiguration();
        
        // Загружаем настройки из конфига
        loadConfig();
    }

    /**
     * Загрузить настройки из config.yml
     */
    protected void loadConfig() {
        commands.clear();
        permissions.clear();
        
        if (config.contains("commands")) {
            commands.addAll(config.getStringList("commands"));
        }
        if (config.contains("permissions")) {
            permissions.addAll(config.getStringList("permissions"));
        }
    }

    /**
     * Включить модуль
     */
    public void enable() {
        if (enabled) return;
        enabled = true;
        onEnable();
        plugin.getLogger().info("[Module] Enabled: " + id);
    }

    /**
     * Выключить модуль
     */
    public void disable() {
        if (!enabled) return;
        enabled = false;
        onDisable();
        plugin.getLogger().info("[Module] Disabled: " + id);
    }

    /**
     * Перезагрузить модуль
     */
    public void reload() {
        disable();
        loadConfig();
        enable();
    }

    // ===== АБСТРАКТНЫЕ МЕТОДЫ =====

    /**
     * Вызывается при включении модуля
     */
    protected abstract void onEnable();

    /**
     * Вызывается при выключении модуля
     */
    protected abstract void onDisable();

    // ===== УТИЛИТЫ =====

    /**
     * Получить значение из config.yml
     */
    protected String getConfig(String path, String def) {
        return config.getString(path, def);
    }

    /**
     * Получить int из config.yml
     */
    protected int getConfigInt(String path, int def) {
        return config.getInt(path, def);
    }

    /**
     * Получить boolean из config.yml
     */
    protected boolean getConfigBoolean(String path, boolean def) {
        return config.getBoolean(path, def);
    }

    /**
     * Получить список из config.yml
     */
    protected List<String> getConfigList(String path) {
        return config.getStringList(path);
    }

    /**
     * Получить предмет из items.yml
     */
    protected Map<String, Object> getItem(String itemId) {
        Map<String, Object> item = new HashMap<>();
        String path = "items." + itemId;
        
        if (itemsConfig.contains(path)) {
            for (String key : itemsConfig.getConfigurationSection(path).getKeys(false)) {
                item.put(key, itemsConfig.get(path + "." + key));
            }
        }
        
        return item;
    }

    /**
     * Получить все предметы из items.yml
     */
    protected Set<String> getAllItemIds() {
        if (itemsConfig.contains("items")) {
            return itemsConfig.getConfigurationSection("items").getKeys(false);
        }
        return Collections.emptySet();
    }

    // ===== ГЕТТЕРЫ =====

    public String getId() { return id; }
    public File getFolder() { return folder; }
    public YamlConfiguration getConfig() { return config; }
    public YamlConfiguration getItemsConfig() { return itemsConfig; }
    public boolean isEnabled() { return enabled; }
    public List<String> getCommands() { return Collections.unmodifiableList(commands); }
    public List<String> getPermissions() { return Collections.unmodifiableList(permissions); }
}
