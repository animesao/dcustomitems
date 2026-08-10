package me.dcplugin.dcustomitems;

import me.dcplugin.dcustomitems.api.ApiCommand;
import me.dcplugin.dcustomitems.api.ApiEventListener;
import me.dcplugin.dcustomitems.api.ItemAPI;
import me.dcplugin.dcustomitems.api.ItemRegistry;
import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import me.dcplugin.dcustomitems.api.commands.CustomCommandExecutor;
import me.dcplugin.dcustomitems.api.config.MessagesConfig;
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
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.Collections;
import java.util.List;

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

            // Save default configs
            saveDefaultConfig();
            saveResourceItems();
            
            // Auto-create messages.java if not exists
            createDefaultMessagesFile();

            // Initialize managers
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

            // Register custom commands with server
            registerCustomCommands();

            getLogger().info("[API] Database connected");
            getLogger().info("[API] PlaceholderManager initialized");
            getLogger().info("[API] Custom commands registered: " + apiItemRegistry.getCommandCount());

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

    /**
     * Auto-create default messages.java if not exists
     */
    private void createDefaultMessagesFile() {
        File itemsDir = new File(getDataFolder(), "items");
        if (!itemsDir.exists()) itemsDir.mkdirs();
        
        File messagesFile = new File(itemsDir, "messages.java");
        if (!messagesFile.exists()) {
            try {
                String defaultContent = generateDefaultMessagesContent();
                FileWriter writer = new FileWriter(messagesFile);
                writer.write(defaultContent);
                writer.close();
                getLogger().info("[Config] Created default messages.java");
            } catch (Exception e) {
                getLogger().warning("Failed to create messages.java: " + e.getMessage());
            }
        }
    }

    /**
     * Generate default messages.java content
     */
    private String generateDefaultMessagesContent() {
        return "import me.dcplugin.dcustomitems.api.config.MessagesConfig;\n" +
               "\n" +
               "/**\n" +
               " * Plugin Messages Configuration\n" +
               " * Edit this file to customize all plugin messages!\n" +
               " * Run /ci reload after changes.\n" +
               " */\n" +
               "public class messages {\n" +
               "\n" +
               "    public static void load() {\n" +
               "        // Prefix\n" +
               "        MessagesConfig.PREFIX = \"&8[&6DCI&8] &r\";\n" +
               "\n" +
               "        // General\n" +
               "        MessagesConfig.NO_PERMISSION = MessagesConfig.PREFIX + \"&cNo permission!\";\n" +
               "        MessagesConfig.PLAYER_NOT_FOUND = MessagesConfig.PREFIX + \"&cPlayer not found!\";\n" +
               "        MessagesConfig.ITEM_NOT_FOUND = MessagesConfig.PREFIX + \"&cItem not found: {item}\";\n" +
               "\n" +
               "        // /ci commands\n" +
               "        MessagesConfig.CI_HEADER = \"&6=== DC-CustomItems ===\";\n" +
               "        MessagesConfig.CI_HELP_GIVE = \"&e/ci give <id> [player] &7- Give item\";\n" +
               "        MessagesConfig.CI_HELP_LIST = \"&e/ci list &7- List items\";\n" +
               "        MessagesConfig.CI_HELP_RELOAD = \"&e/ci reload &7- Reload plugin\";\n" +
               "        MessagesConfig.CI_GIVE_SELF = MessagesConfig.PREFIX + \"&aGiven: &e{item}\";\n" +
               "        MessagesConfig.CI_GIVE_OTHER = MessagesConfig.PREFIX + \"&aGiven &e{item} &ato &e{player}\";\n" +
               "        MessagesConfig.CI_GIVE_RECEIVED = MessagesConfig.PREFIX + \"&aYou received: &e{item}\";\n" +
               "\n" +
               "        // List\n" +
               "        MessagesConfig.LIST_HEADER = MessagesConfig.PREFIX + \"&6=== Items ===\";\n" +
               "        MessagesConfig.LIST_ITEM = MessagesConfig.PREFIX + \"&e{item} &7- &f{type}\";\n" +
               "        MessagesConfig.LIST_EMPTY = MessagesConfig.PREFIX + \"&cNo items loaded\";\n" +
               "\n" +
               "        // Reload\n" +
               "        MessagesConfig.RELOAD_SUCCESS = MessagesConfig.PREFIX + \"&a&lReloaded!\";\n" +
               "        MessagesConfig.RELOAD_YAML = MessagesConfig.PREFIX + \"&7YAML: &e{count}\";\n" +
               "        MessagesConfig.RELOAD_JAVA_ITEMS = MessagesConfig.PREFIX + \"&7Java Items: &e{count}\";\n" +
               "        MessagesConfig.RELOAD_JAVA_COMMANDS = MessagesConfig.PREFIX + \"&7Java Commands: &e{count}\";\n" +
               "        MessagesConfig.RELOAD_JAVA_PLACEHOLDERS = MessagesConfig.PREFIX + \"&7Java Placeholders: &e{count}\";\n" +
               "\n" +
               "        // Updates\n" +
               "        MessagesConfig.UPDATE_LATEST = MessagesConfig.PREFIX + \"&aLatest version! ({version})\";\n" +
               "        MessagesConfig.UPDATE_AVAILABLE = MessagesConfig.PREFIX + \"&cNew version: {version}\";\n" +
               "\n" +
               "        // /api-item\n" +
               "        MessagesConfig.API_HEADER = MessagesConfig.PREFIX + \"&6=== Java API ===\";\n" +
               "        MessagesConfig.API_GIVE_SELF = MessagesConfig.PREFIX + \"&aGiven: &e{item}\";\n" +
               "        MessagesConfig.API_LIST_EMPTY = MessagesConfig.PREFIX + \"&cNo Java API items\";\n" +
               "\n" +
               "        // Cooldowns\n" +
               "        MessagesConfig.COOLDOWN = MessagesConfig.PREFIX + \"&cWait {seconds}s!\";\n" +
               "\n" +
               "        // Actions\n" +
               "        MessagesConfig.ACTION_HEAL = \"&a+{amount} HP\";\n" +
               "        MessagesConfig.ACTION_TELEPORT = \"&dTeleported!\";\n" +
               "        MessagesConfig.ACTION_GIVE = \"&a+{amount} {material}\";\n" +
               "        MessagesConfig.ACTION_REMOVE = \"&c-{amount} {material}\";\n" +
               "\n" +
               "        // Heal\n" +
               "        MessagesConfig.HEAL_SELF = MessagesConfig.PREFIX + \"&aHealed!\";\n" +
               "        MessagesConfig.HEAL_OTHER = MessagesConfig.PREFIX + \"&aHealed {player}\";\n" +
               "\n" +
               "        // Teleport\n" +
               "        MessagesConfig.TELEPORT_SELF = MessagesConfig.PREFIX + \"&bTeleported to {player}\";\n" +
               "\n" +
               "        // Fly\n" +
               "        MessagesConfig.FLY_ON = MessagesConfig.PREFIX + \"&aFly enabled!\";\n" +
               "        MessagesConfig.FLY_OFF = MessagesConfig.PREFIX + \"&cFly disabled!\";\n" +
               "\n" +
               "        // Gamemode\n" +
               "        MessagesConfig.GAMEMODE_SURVIVAL = MessagesConfig.PREFIX + \"&aSurvival mode\";\n" +
               "        MessagesConfig.GAMEMODE_CREATIVE = MessagesConfig.PREFIX + \"&aCreative mode\";\n" +
               "        MessagesConfig.GAMEMODE_ADVENTURE = MessagesConfig.PREFIX + \"&aAdventure mode\";\n" +
               "        MessagesConfig.GAMEMODE_SPECTATOR = MessagesConfig.PREFIX + \"&aSpectator mode\";\n" +
               "\n" +
               "        // Berserk\n" +
               "        MessagesConfig.BERSERK_ENABLED = MessagesConfig.PREFIX + \"&4BERSERK!\";\n" +
               "        MessagesConfig.BERSERK_DISABLED = MessagesConfig.PREFIX + \"&7Berserk off.\";\n" +
               "\n" +
               "        // God mode\n" +
               "        MessagesConfig.GOD_ON = MessagesConfig.PREFIX + \"&6God mode for {seconds}s!\";\n" +
               "        MessagesConfig.GOD_OFF = MessagesConfig.PREFIX + \"&7God mode off.\";\n" +
               "\n" +
               "        // Invisible\n" +
               "        MessagesConfig.INVISIBLE_ON = MessagesConfig.PREFIX + \"&7Invisible!\";\n" +
               "        MessagesConfig.INVISIBLE_OFF = MessagesConfig.PREFIX + \"&7Visible.\";\n" +
               "\n" +
               "        // Speed\n" +
               "        MessagesConfig.SPEED_ON = MessagesConfig.PREFIX + \"&bSpeed up!\";\n" +
               "        MessagesConfig.SPEED_OFF = MessagesConfig.PREFIX + \"&7Normal speed.\";\n" +
               "\n" +
               "        // Feed\n" +
               "        MessagesConfig.FEED_SELF = MessagesConfig.PREFIX + \"&aFed!\";\n" +
               "        MessagesConfig.FEED_OTHER = MessagesConfig.PREFIX + \"&aFed {player}\";\n" +
               "\n" +
               "        // XP\n" +
               "        MessagesConfig.XP_GIVEN = MessagesConfig.PREFIX + \"+{amount} levels\";\n" +
               "\n" +
               "        // Chat\n" +
               "        MessagesConfig.CHAT_CLEARED = MessagesConfig.PREFIX + \"&aChat cleared!\";\n" +
               "\n" +
               "        // Stats\n" +
               "        MessagesConfig.STATS_HEADER = \"&6=== Stats ===\";\n" +
               "        MessagesConfig.STATS_HEALTH = \"&7HP: &c{health}/{max}\";\n" +
               "        MessagesConfig.STATS_FOOD = \"&7Food: &e{food}\";\n" +
               "        MessagesConfig.STATS_LEVEL = \"&7Level: &a{level}\";\n" +
               "        MessagesConfig.STATS_WORLD = \"&7World: &b{world}\";\n" +
               "        MessagesConfig.STATS_POSITION = \"&7Pos: &f{x} {y} {z}\";\n" +
               "        MessagesConfig.STATS_GAMEMODE = \"&7Mode: &d{gamemode}\";\n" +
               "        MessagesConfig.STATS_FOOTER = \"&6=========\";\n" +
               "\n" +
               "        // Errors\n" +
               "        MessagesConfig.ERROR_COMMAND = MessagesConfig.PREFIX + \"&cError: {error}\";\n" +
               "        MessagesConfig.ERROR_ITEM = MessagesConfig.PREFIX + \"&cItem error: {error}\";\n" +
               "    }\n" +
               "}\n";
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

    /**
     * Register all custom commands with the Bukkit server
     */
    public void registerCustomCommands() {
        if (apiItemRegistry == null) return;
        
        CustomCommandExecutor executor = new CustomCommandExecutor(apiItemRegistry);
        
        for (CustomCommand cmd : apiItemRegistry.getAllCommands().values()) {
            try {
                // Register command with server
                org.bukkit.command.PluginCommand pluginCmd = getCommand(cmd.getName());
                if (pluginCmd == null) {
                    // Create command dynamically
                    pluginCmd = Bukkit.getPluginCommand(cmd.getName());
                }
                
                if (pluginCmd != null) {
                    pluginCmd.setExecutor(executor);
                    pluginCmd.setTabCompleter(executor);
                    getLogger().info("[API] Registered command: /" + cmd.getName());
                } else {
                    // Use reflection to register command
                    registerCommandViaReflection(cmd.getName(), executor);
                }
            } catch (Exception e) {
                getLogger().warning("[API] Failed to register command: /" + cmd.getName() + " - " + e.getMessage());
            }
        }
    }

    /**
     * Register command via reflection (for dynamic commands)
     */
    private void registerCommandViaReflection(String name, CommandExecutor executor) {
        try {
            // Get CraftServer class
            Class<?> craftServerClass = getServer().getClass();
            
            // Try to get command map
            java.lang.reflect.Method getCommandMapMethod = craftServerClass.getMethod("getCommandMap");
            Object commandMap = getCommandMapMethod.invoke(getServer());
            
            if (commandMap == null) {
                getLogger().warning("[API] Command map is null for /" + name);
                return;
            }
            
            // Create command with proper implementations
            org.bukkit.command.Command command = new org.bukkit.command.Command(name) {
                @Override
                public boolean execute(CommandSender sender, String label, String[] args) {
                    try {
                        return executor.onCommand(sender, this, label, args);
                    } catch (Exception e) {
                        getLogger().warning("Error executing command: " + e.getMessage());
                        return false;
                    }
                }
                
                @Override
                public java.util.List<String> tabComplete(CommandSender sender, String alias, String[] args) {
                    try {
                        if (executor instanceof TabCompleter) {
                            return ((TabCompleter) executor).onTabComplete(sender, this, alias, args);
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                    return java.util.Collections.emptyList();
                }
            };
            
            command.setPermission("dci.command." + name);
            command.setDescription("Custom command: " + name);
            command.setUsage("/" + name);
            
            // Log all methods of commandMap class
            getLogger().info("[API] CommandMap class: " + commandMap.getClass().getName());
            for (java.lang.reflect.Method m : commandMap.getClass().getMethods()) {
                if (m.getName().equals("register")) {
                    getLogger().info("[API] Found register method: " + m.toString());
                }
            }
            
            // Register using 2-arg method (no namespace prefix)
            try {
                java.lang.reflect.Method registerMethod = commandMap.getClass().getMethod("register", String.class, org.bukkit.command.Command.class);
                registerMethod.invoke(commandMap, "dcustomitems", command);
                getLogger().info("[API] ✅ Registered command: /" + name);
            } catch (Exception e) {
                getLogger().warning("[API] ❌ Failed to register /" + name + ": " + e.getMessage());
                e.printStackTrace();
            }
            
        } catch (Exception e) {
            getLogger().warning("[API] ❌ Reflection registration failed for /" + name + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
