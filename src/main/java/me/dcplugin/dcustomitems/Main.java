package me.dcplugin.dcustomitems;

import me.dcplugin.dcustomitems.api.ApiCommand;
import me.dcplugin.dcustomitems.api.ApiEventListener;
import me.dcplugin.dcustomitems.api.ItemAPI;
import me.dcplugin.dcustomitems.api.ItemRegistry;
import me.dcplugin.dcustomitems.api.database.DatabaseManager;
import me.dcplugin.dcustomitems.api.placeholders.PlaceholderManager;
import me.dcplugin.dcustomitems.commands.CustomItemsCommand;
import me.dcplugin.dcustomitems.handlers.CustomItemHandler;
import me.dcplugin.dcustomitems.listeners.PlayerListener;
import me.dcplugin.dcustomitems.listeners.TriggerListener;
import me.dcplugin.dcustomitems.managers.ConfigManager;
import me.dcplugin.dcustomitems.managers.MessageManager;
import me.dcplugin.dcustomitems.managers.EffectManager;
import me.dcplugin.dcustomitems.managers.AttributeManager;
import me.dcplugin.dcustomitems.managers.ArmorSetManager;
import me.dcplugin.dcustomitems.utils.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Main extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private EffectManager effectManager;
    private AttributeManager attributeManager;
    private CustomItemHandler itemHandler;
    private PlayerListener playerListener;
    private TriggerListener triggerListener;
    private ArmorSetManager armorSetManager;
    private UpdateChecker updateChecker;
    private ItemRegistry apiItemRegistry;
    private DatabaseManager databaseManager;
    private PlaceholderManager placeholderManager;

    @Override
    public void onEnable() {
        try {
            getLogger().info("CustomItems starting...");

            saveDefaultConfig();
            saveResourceItems();

            configManager = new ConfigManager(this);
            messageManager = new MessageManager(this);
            effectManager = new EffectManager(this);
            attributeManager = new AttributeManager(this);
            itemHandler = new CustomItemHandler(this);
            armorSetManager = new ArmorSetManager(this);

            itemHandler.loadCustomItems();

            ItemAPI.init(this);
            apiItemRegistry = new ItemRegistry(this);
            apiItemRegistry.loadAll();

            Bukkit.getScheduler().runTaskLater(this, () -> {
                CustomItemsCommand command = new CustomItemsCommand(this);
                getCommand("customitems").setExecutor(command);
                getCommand("customitems").setTabCompleter(command);

                ApiCommand apiCommand = new ApiCommand(apiItemRegistry);
                getCommand("api-item").setExecutor(apiCommand);
                getCommand("api-item").setTabCompleter(apiCommand);
            }, 1L);

            playerListener = new PlayerListener(this);
            triggerListener = new TriggerListener(this);
            getServer().getPluginManager().registerEvents(playerListener, this);
            getServer().getPluginManager().registerEvents(new me.dcplugin.dcustomitems.listeners.BlockPlaceListener(this), this);
            getServer().getPluginManager().registerEvents(triggerListener, this);
            getServer().getPluginManager().registerEvents(new ApiEventListener(apiItemRegistry), this);

            databaseManager = new DatabaseManager(this);
            databaseManager.connect();

            placeholderManager = new PlaceholderManager(this);

            getLogger().info("[API] Database connected");
            getLogger().info("[API] PlaceholderManager initialized");

            updateChecker = new UpdateChecker(this);
            updateChecker.checkForUpdates(message -> {
                String cleanMessage = message.replaceAll("\u00a7[0-9a-fk-or]", "");
                if (cleanMessage.contains("latest version")) {
                    getLogger().info(cleanMessage);
                } else if (cleanMessage.contains("new version")) {
                    getLogger().warning(cleanMessage);
                }
            });

            getLogger().info("CustomItems enabled!");
            getLogger().info("Java API: " + apiItemRegistry.getCount() + " items, " + 
                apiItemRegistry.getCommandCount() + " commands, " +
                apiItemRegistry.getPlaceholderCount() + " placeholders");

        } catch (Exception e) {
            getLogger().severe("Error enabling plugin: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("CustomItems disabling...");

        if (databaseManager != null) databaseManager.disconnect();

        if (effectManager != null) {
            getServer().getOnlinePlayers().forEach(effectManager::stopPeriodicEffects);
        }

        if (attributeManager != null) attributeManager.cleanup();

        getLogger().info("CustomItems disabled!");
    }

    private void saveResourceItems() {
        File itemsFolder = new File(getDataFolder(), "items");
        if (!itemsFolder.exists()) itemsFolder.mkdirs();
        
        try {
            java.util.jar.JarFile jar = new java.util.jar.JarFile(getFile());
            java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                java.util.jar.JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("items/") && name.endsWith(".yml")) {
                    File destFile = new File(getDataFolder(), name);
                    if (!destFile.exists()) saveResource(name, false);
                }
            }
            jar.close();
        } catch (Exception e) {}
    }

    public ConfigManager getConfigManager() { return configManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public EffectManager getEffectManager() { return effectManager; }
    public CustomItemHandler getItemHandler() { return itemHandler; }
    public PlayerListener getPlayerListener() { return playerListener; }
    public ArmorSetManager getArmorSetManager() { return armorSetManager; }
    public AttributeManager getAttributeManager() { return attributeManager; }
    public TriggerListener getTriggerListener() { return triggerListener; }
    public UpdateChecker getUpdateChecker() { return updateChecker; }
    public ItemRegistry getApiItemRegistry() { return apiItemRegistry; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public PlaceholderManager getPlaceholderManager() { return placeholderManager; }
}
