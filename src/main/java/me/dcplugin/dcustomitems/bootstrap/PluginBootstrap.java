package me.dcplugin.dcustomitems.bootstrap;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.ApiEventListener;
import me.dcplugin.dcustomitems.api.ItemAPI;
import me.dcplugin.dcustomitems.api.ItemRegistry;
import me.dcplugin.dcustomitems.api.config.MessagesConfig;
import me.dcplugin.dcustomitems.api.database.DatabaseManager;
import me.dcplugin.dcustomitems.api.modules.ModuleManager;
import me.dcplugin.dcustomitems.api.placeholders.PlaceholderManager;
import me.dcplugin.dcustomitems.commands.CustomItemsCommand;
import me.dcplugin.dcustomitems.handlers.CustomItemHandler;
import me.dcplugin.dcustomitems.handlers.EquippedItemsChecker;
import me.dcplugin.dcustomitems.listeners.PlayerListener;
import me.dcplugin.dcustomitems.listeners.TriggerListener;
import me.dcplugin.dcustomitems.managers.*;
import me.dcplugin.dcustomitems.utils.UpdateChecker;
import org.bukkit.Bukkit;

import java.util.logging.Level;

/**
 * Централизованная инициализация всех компонентов плагина.
 * Вызывается из Main.onEnable() и Main.onDisable().
 */
public class PluginBootstrap {

    private final Main plugin;
    private final CommandRegistrar commandRegistrar;
    private final ConfigMigrator configMigrator;

    // Managers
    private ConfigManager configManager;
    private MessageManager messageManager;
    private EffectManager effectManager;
    private AttributeManager attributeManager;
    private CustomItemHandler itemHandler;
    private ArmorSetManager armorSetManager;

    // Listeners
    private PlayerListener playerListener;
    private TriggerListener triggerListener;
    private EquippedItemsChecker equippedItemsChecker;

    // API
    private ItemRegistry apiItemRegistry;
    private ModuleManager moduleManager;
    private DatabaseManager databaseManager;
    private PlaceholderManager placeholderManager;
    private UpdateChecker updateChecker;

    public PluginBootstrap(Main plugin) {
        this.plugin = plugin;
        this.commandRegistrar = new CommandRegistrar(plugin);
        this.configMigrator = new ConfigMigrator(plugin);
    }

    /**
     * Полная инициализация плагина
     */
    public void enable() {
        plugin.getLogger().info("CustomItems starting...");

        plugin.saveDefaultConfig();

        // Миграция конфига и создание messages.java
        configMigrator.createDefaultMessagesFile();
        configMigrator.checkConfigMigration();

        // Инициализация менеджеров
        initManagers();

        // Загрузка предметов
        itemHandler.loadCustomItems();

        // Инициализация API
        ItemAPI.init(plugin);
        apiItemRegistry = new ItemRegistry(plugin);
        moduleManager = new ModuleManager(plugin);
        apiItemRegistry.loadAll();
        moduleManager.loadAll();

        // Регистрация команд (с задержкой 1 тик)
        registerBukkitCommand();

        // Регистрация событий
        registerEvents();

        // База данных
        initDatabase();

        // Плейсхолдеры
        initPlaceholders();

        // Глобальный таск проверки экипировки
        equippedItemsChecker = new EquippedItemsChecker(plugin);
        equippedItemsChecker.start();

        // Динамические команды из API
        commandRegistrar.registerAll(apiItemRegistry);

        // Проверка обновлений
        checkUpdates();

        logStartupInfo();
    }

    /**
     * Корректное отключение плагина
     */
    public void disable() {
        plugin.getLogger().info("CustomItems disabling...");

        if (databaseManager != null) databaseManager.shutdownAsync();
        if (databaseManager != null) databaseManager.disconnect();

        if (effectManager != null) {
            plugin.getServer().getOnlinePlayers().forEach(effectManager::stopPeriodicEffects);
        }
        if (attributeManager != null) attributeManager.cleanup();

        if (equippedItemsChecker != null) equippedItemsChecker.cancel();
        try { commandRegistrar.unregisterAll(); } catch (Exception ignored) {}

        plugin.getLogger().info("CustomItems disabled!");
    }

    // ===== Internal init methods =====

    private void initManagers() {
        configManager = new ConfigManager(plugin);
        messageManager = new MessageManager(plugin);
        effectManager = new EffectManager(plugin);
        attributeManager = new AttributeManager(plugin);
        itemHandler = new CustomItemHandler(plugin);
        armorSetManager = new ArmorSetManager(plugin);
    }

    private void registerBukkitCommand() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            var cmd = plugin.getCommand("customitems");
            if (cmd == null) {
                plugin.getLogger().severe("[FATAL] Command 'customitems' not found in plugin.yml!");
                return;
            }
            CustomItemsCommand command = new CustomItemsCommand(plugin);
            cmd.setExecutor(command);
            cmd.setTabCompleter(command);
            plugin.getLogger().info("[Command] /ci reload registered successfully");
        }, 1L);
    }

    private void registerEvents() {
        playerListener = new PlayerListener(plugin);
        triggerListener = new TriggerListener(plugin);

        var pm = plugin.getServer().getPluginManager();
        pm.registerEvents(playerListener, plugin);
        pm.registerEvents(new me.dcplugin.dcustomitems.listeners.BlockPlaceListener(plugin), plugin);
        pm.registerEvents(triggerListener, plugin);
        pm.registerEvents(new ApiEventListener(apiItemRegistry), plugin);
    }

    private void initDatabase() {
        databaseManager = new DatabaseManager(plugin);
        databaseManager.connect();
    }

    private void initPlaceholders() {
        placeholderManager = new PlaceholderManager(plugin);
        placeholderManager.registerExpansion(plugin);
    }

    private void checkUpdates() {
        updateChecker = new UpdateChecker(plugin);
        updateChecker.checkForUpdates(message -> {
            String cleanMessage = message.replaceAll("\u00a7[0-9a-fk-or]", "");
            if (cleanMessage.contains("latest version")) {
                plugin.getLogger().info(cleanMessage);
            } else if (cleanMessage.contains("new version")) {
                plugin.getLogger().warning(cleanMessage);
            }
        });
    }

    private void logStartupInfo() {
        plugin.getLogger().info("CustomItems enabled!");
        plugin.getLogger().info("[API] Database connected");
        plugin.getLogger().info("[API] PlaceholderManager initialized");
        plugin.getLogger().info("[API] Custom commands registered: " + apiItemRegistry.getCommandCount());
        plugin.getLogger().info("Java API: " + apiItemRegistry.getCount() + " items, " +
                apiItemRegistry.getCommandCount() + " commands, " +
                apiItemRegistry.getPlaceholderCount() + " placeholders");
    }

    // ===== Getters =====

    public ConfigManager getConfigManager() { return configManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public EffectManager getEffectManager() { return effectManager; }
    public AttributeManager getAttributeManager() { return attributeManager; }
    public CustomItemHandler getItemHandler() { return itemHandler; }
    public ArmorSetManager getArmorSetManager() { return armorSetManager; }
    public PlayerListener getPlayerListener() { return playerListener; }
    public TriggerListener getTriggerListener() { return triggerListener; }
    public ItemRegistry getApiItemRegistry() { return apiItemRegistry; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public PlaceholderManager getPlaceholderManager() { return placeholderManager; }
    public UpdateChecker getUpdateChecker() { return updateChecker; }
    public EquippedItemsChecker getEquippedItemsChecker() { return equippedItemsChecker; }
    public CommandRegistrar getCommandRegistrar() { return commandRegistrar; }
    public ConfigMigrator getConfigMigrator() { return configMigrator; }
}
