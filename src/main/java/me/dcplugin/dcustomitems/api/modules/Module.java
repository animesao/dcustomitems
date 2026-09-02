package me.dcplugin.dcustomitems.api.modules;

import me.dcplugin.dcustomitems.Main;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

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

    /**
     * Динамически зарегистрированные команды модуля (CommandMap).
     * Удаляются при disable(), чтобы при удалении/перезагрузке модуля
     * в CommandMap не оставались "мёртвые" команды.
     */
    private final List<org.bukkit.command.Command> dynamicCommands = new ArrayList<>();

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
        try {
            onDisable();
        } finally {
            unregisterDynamicCommands();
        }
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

    /**
     * Открыть меню модуля. Меню-модули могут переопределить этот метод.
     */
    public boolean openMenu(Player player, String menuId) {
        return false;
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

    // ===== ДИНАМИЧЕСКАЯ РЕГИСТРАЦИЯ КОМАНД =====

    /**
     * Зарегистрировать команду в CommandMap (право по умолчанию: "dci.<name>").
     * Команда автоматически удалится при disable() модуля.
     */
    protected void registerDynamicCommand(String name, org.bukkit.command.CommandExecutor executor) {
        registerDynamicCommand(name, executor, "dci." + name.toLowerCase());
    }

    /**
     * Зарегистрировать команду с явным правом.
     */
    protected void registerDynamicCommand(String name, org.bukkit.command.CommandExecutor executor, String permission) {
        try {
            org.bukkit.command.CommandMap commandMap = getCommandMap();
            if (commandMap == null) return;

            org.bukkit.command.Command command = new org.bukkit.command.Command(name.toLowerCase()) {
                @Override
                public boolean execute(org.bukkit.command.CommandSender sender, String label, String[] args) {
                    return executor.onCommand(sender, this, label, args);
                }

                @Override
                public java.util.List<String> tabComplete(org.bukkit.command.CommandSender sender, String alias, String[] args) {
                    if (executor instanceof org.bukkit.command.TabCompleter) {
                        return ((org.bukkit.command.TabCompleter) executor).onTabComplete(sender, this, alias, args);
                    }
                    return java.util.Collections.emptyList();
                }
            };
            command.setPermission(permission);
            command.setDescription("Module " + id + ": " + name);
            command.setUsage("/" + name);

            commandMap.register("dcustomitems", command);
            dynamicCommands.add(command);
            plugin.getLogger().info("[Module " + id + "] Registered command: /" + name);
        } catch (Exception e) {
            plugin.getLogger().warning("[Module " + id + "] Failed to register /" + name + ": " + e.getMessage());
        }
    }

    /**
     * Удалить все динамические команды модуля из CommandMap.
     */
    @SuppressWarnings("unchecked")
    private void unregisterDynamicCommands() {
        if (dynamicCommands.isEmpty()) return;
        try {
            org.bukkit.command.CommandMap commandMap = getCommandMap();
            if (commandMap == null) return;

            Map<String, org.bukkit.command.Command> knownCommands = getKnownCommands(commandMap);
            for (org.bukkit.command.Command cmd : dynamicCommands) {
                knownCommands.remove(cmd.getName().toLowerCase());
                for (String alias : cmd.getAliases()) {
                    knownCommands.remove(alias.toLowerCase());
                }
            }
            dynamicCommands.clear();
        } catch (Exception e) {
            plugin.getLogger().warning("[Module " + id + "] Error unregistering commands: " + e.getMessage());
        }
    }

    private org.bukkit.command.CommandMap getCommandMap() {
        // Paper 1.21+: Bukkit.getCommandMap()
        try {
            java.lang.reflect.Method method = org.bukkit.Bukkit.class.getMethod("getCommandMap");
            return (org.bukkit.command.CommandMap) method.invoke(null);
        } catch (Exception ignored) {}
        // Fallback: поле commandMap
        try {
            java.lang.reflect.Field field = org.bukkit.Bukkit.class.getDeclaredField("commandMap");
            field.setAccessible(true);
            return (org.bukkit.command.CommandMap) field.get(null);
        } catch (Exception ignored) {}
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, org.bukkit.command.Command> getKnownCommands(org.bukkit.command.CommandMap commandMap) {
        for (java.lang.reflect.Field field : commandMap.getClass().getDeclaredFields()) {
            if (Map.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(commandMap);
                    if (value instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) value;
                        if (!map.isEmpty() && map.keySet().iterator().next() instanceof String) {
                            return (Map<String, org.bukkit.command.Command>) value;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return new HashMap<>();
    }

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
