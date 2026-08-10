package me.dcplugin.dcustomitems;

import me.dcplugin.dcustomitems.api.ApiCommand;
import me.dcplugin.dcustomitems.api.ApiEventListener;
import me.dcplugin.dcustomitems.api.ItemAPI;
import me.dcplugin.dcustomitems.api.ItemRegistry;
import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import me.dcplugin.dcustomitems.api.commands.CustomCommandExecutor;
import me.dcplugin.dcustomitems.api.modules.ModuleManager;
import me.dcplugin.dcustomitems.api.config.MessagesConfig;
import me.dcplugin.dcustomitems.api.database.DatabaseManager;
import me.dcplugin.dcustomitems.api.database.YamlStorage;
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
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public class Main extends JavaPlugin {

    private static Main instance;

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
    private ModuleManager moduleManager;
    private final Set<Command> dynamicCommands = Collections.newSetFromMap(new IdentityHashMap<>());

    @Override
    public void onEnable() {
        instance = this;
        try {
            getLogger().info("CustomItems starting...");

            // Save default config. Built-in items stay inside config.yml;
            // do not extract bundled item examples into the user's items/ folder.
            saveDefaultConfig();
            
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
            moduleManager = new ModuleManager(this);
            apiItemRegistry.loadAll();
            
            moduleManager.loadAll();

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
        if (databaseManager != null) databaseManager.shutdownAsync();
        if (databaseManager != null) databaseManager.disconnect();
        if (effectManager != null) {
            getServer().getOnlinePlayers().forEach(effectManager::stopPeriodicEffects);
        }
        if (attributeManager != null) attributeManager.cleanup();
        unregisterCustomCommands();
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
               " * ============================================\n" +
               " * DC-CustomItems - Настройка сообщений\n" +
               " * ============================================\n" +
               " *\n" +
               " * Редактируй этот файл чтобы изменить ЛЮБОЕ сообщение плагина!\n" +
               " * После изменений выполни /ci reload\n" +
               " *\n" +
               " * Формат:\n" +
               " * - &0-9, &a-f - цвета\n" +
               " * - &l - жирный\n" +
               " * - &n - подчёркнутый\n" +
               " * - &o - курсив\n" +
               " * - &k - обфусцированный\n" +
               " * - &r - сброс\n" +
               " * - {player}, {item}, {error}, {count}, {version} - плейсхолдеры\n" +
               " */\n" +
               "public class messages {\n" +
               "\n" +
               "    public static void load() {\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        // ПРЕФИКС\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        MessagesConfig.PREFIX = \"&8[&6DCI&8] &r\";\n" +
               "\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        // ОБЩИЕ СООБЩЕНИЯ\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        MessagesConfig.NO_PERMISSION = MessagesConfig.PREFIX + \"&cУ вас нет прав!\";\n" +
               "        MessagesConfig.PLAYER_NOT_FOUND = MessagesConfig.PREFIX + \"&cИгрок не найден!\";\n" +
               "        MessagesConfig.ITEM_NOT_FOUND = MessagesConfig.PREFIX + \"&cПредмет '{item}' не найден!\";\n" +
               "        MessagesConfig.UNKNOWN_COMMAND = MessagesConfig.PREFIX + \"&cНеизвестная команда!\";\n" +
               "        MessagesConfig.ONLY_FOR_PLAYERS = MessagesConfig.PREFIX + \"&cТолько для игроков!\";\n" +
               "\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        // КОМАНДЫ /ci\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        MessagesConfig.CI_HEADER = \"&6=== DC-CustomItems ===\";\n" +
               "        MessagesConfig.CI_HELP_GIVE = \"&e/ci give <предмет> [игрок] &7- Выдать предмет\";\n" +
               "        MessagesConfig.CI_HELP_LIST = \"&e/ci list &7- Список предметов\";\n" +
               "        MessagesConfig.CI_HELP_RELOAD = \"&e/ci reload &7- Перезагрузить\";\n" +
               "        MessagesConfig.CI_HELP_UPDATE = \"&e/ci update &7- Обновления\";\n" +
               "        MessagesConfig.CI_GIVE_SELF = MessagesConfig.PREFIX + \"&aВыдан: &e{item}\";\n" +
               "        MessagesConfig.CI_GIVE_OTHER = MessagesConfig.PREFIX + \"&aВыдан &e{item} &aигроку &e{player}\";\n" +
               "        MessagesConfig.CI_GIVE_RECEIVED = MessagesConfig.PREFIX + \"&aВы получили: &e{item}\";\n" +
               "        MessagesConfig.CI_GIVE_NO_PERM = MessagesConfig.PREFIX + \"&cНет прав на этот предмет!\";\n" +
               "\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        // СПИСОК\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        MessagesConfig.LIST_HEADER = MessagesConfig.PREFIX + \"&6=== Список предметов ===\";\n" +
               "        MessagesConfig.LIST_ITEM = MessagesConfig.PREFIX + \"&e{item} &7- &f{type}\";\n" +
               "        MessagesConfig.LIST_EMPTY = MessagesConfig.PREFIX + \"&cНет предметов\";\n" +
               "\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        // ПЕРЕЗАГРУЗКА\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        MessagesConfig.RELOAD_SUCCESS = MessagesConfig.PREFIX + \"&a&lПлагин перезагружен!\";\n" +
               "        MessagesConfig.RELOAD_YAML = MessagesConfig.PREFIX + \"&7YAML: &e{count}\";\n" +
               "        MessagesConfig.RELOAD_JAVA_ITEMS = MessagesConfig.PREFIX + \"&7Java предметов: &e{count}\";\n" +
               "        MessagesConfig.RELOAD_JAVA_COMMANDS = MessagesConfig.PREFIX + \"&7Java команд: &e{count}\";\n" +
               "        MessagesConfig.RELOAD_JAVA_PLACEHOLDERS = MessagesConfig.PREFIX + \"&7Java плейсхолдеров: &e{count}\";\n" +
               "        MessagesConfig.RELOAD_ERROR = MessagesConfig.PREFIX + \"&cОшибка: {error}\";\n" +
               "\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        // ОБНОВЛЕНИЯ\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        MessagesConfig.UPDATE_CHECKING = MessagesConfig.PREFIX + \"&eПроверка обновлений...\";\n" +
               "        MessagesConfig.UPDATE_LATEST = MessagesConfig.PREFIX + \"&aПоследняя версия! ({version})\";\n" +
               "        MessagesConfig.UPDATE_AVAILABLE = MessagesConfig.PREFIX + \"&cНовая версия: {version}\";\n" +
               "        MessagesConfig.UPDATE_DOWNLOAD = MessagesConfig.PREFIX + \"&bСкачать: https://github.com/animesao/dcustomitems/releases\";\n" +
               "\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        // JAVA API\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        MessagesConfig.API_HEADER = MessagesConfig.PREFIX + \"&6=== Java API ===\";\n" +
               "        MessagesConfig.API_GIVE_SELF = MessagesConfig.PREFIX + \"&aВыдан: &e{item}\";\n" +
               "        MessagesConfig.API_LIST_EMPTY = MessagesConfig.PREFIX + \"&cНет Java предметов\";\n" +
               "\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        // КУЛДАУНЫ\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        MessagesConfig.COOLDOWN = MessagesConfig.PREFIX + \"&cПодождите {seconds} сек!\";\n" +
               "        MessagesConfig.USES_DEPLETED = MessagesConfig.PREFIX + \"&cПредмет использован!\";\n" +
               "\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        // ДЕЙСТВИЯ\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        MessagesConfig.ACTION_HEAL = \"&a❤ +{amount} HP\";\n" +
               "        MessagesConfig.ACTION_TELEPORT = \"&d✨ Телепорт!\";\n" +
               "        MessagesConfig.ACTION_GIVE = \"&a+{amount} {material}\";\n" +
               "        MessagesConfig.ACTION_REMOVE = \"&c-{amount} {material}\";\n" +
               "\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        // ИСЦЕЛЕНИЕ\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        MessagesConfig.HEAL_SELF = MessagesConfig.PREFIX + \"&a❤ Вы исцелены!\";\n" +
               "        MessagesConfig.HEAL_OTHER = MessagesConfig.PREFIX + \"&aВы исцелили &e{player}\";\n" +
               "        MessagesConfig.HEAL_AMOUNT = MessagesConfig.PREFIX + \"&a❤ +{amount} HP\";\n" +
               "\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        // ТЕЛЕПОРТАЦИЯ\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        MessagesConfig.TELEPORT_SELF = MessagesConfig.PREFIX + \"&bТелепорт к &e{player}\";\n" +
               "        MessagesConfig.TELEPORT_TARGET = MessagesConfig.PREFIX + \"&b{player} телепортировался к вам!\";\n" +
               "\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        // ПОЛЁТ\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        MessagesConfig.FLY_ON = MessagesConfig.PREFIX + \"&a✈ Полёт включён!\";\n" +
               "        MessagesConfig.FLY_OFF = MessagesConfig.PREFIX + \"&c✈ Полёт выключен!\";\n" +
               "\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        // ГЕЙМОД\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        MessagesConfig.GAMEMODE_SURVIVAL = MessagesConfig.PREFIX + \"&aВыживание\";\n" +
               "        MessagesConfig.GAMEMODE_CREATIVE = MessagesConfig.PREFIX + \"&aКреатив\";\n" +
               "        MessagesConfig.GAMEMODE_ADVENTURE = MessagesConfig.PREFIX + \"&aПриключение\";\n" +
               "        MessagesConfig.GAMEMODE_SPECTATOR = MessagesConfig.PREFIX + \"&aНаблюдатель\";\n" +
               "\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        // БЕССМЕРТИЕ\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        MessagesConfig.GOD_ON = MessagesConfig.PREFIX + \"&6Бессмертие на {seconds} сек!\";\n" +
               "        MessagesConfig.GOD_OFF = MessagesConfig.PREFIX + \"&7Бессмертие выкл.\";\n" +
               "\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        // НЕВИДИМОСТЬ\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        MessagesConfig.INVISIBLE_ON = MessagesConfig.PREFIX + \"&7Невидимость!\";\n" +
               "        MessagesConfig.INVISIBLE_OFF = MessagesConfig.PREFIX + \"&7Видимость.\";\n" +
               "\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        // СКОРОСТЬ\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        MessagesConfig.SPEED_ON = MessagesConfig.PREFIX + \"&bСкорость включена!\";\n" +
               "        MessagesConfig.SPEED_OFF = MessagesConfig.PREFIX + \"&7Скорость выкл.\";\n" +
               "\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        // ГОЛОД\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        MessagesConfig.FEED_SELF = MessagesConfig.PREFIX + \"&aВы накормлены!\";\n" +
               "        MessagesConfig.FEED_OTHER = MessagesConfig.PREFIX + \"&aНакормлен &e{player}\";\n" +
               "\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        // ОПЫТ\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        MessagesConfig.XP_GIVEN = MessagesConfig.PREFIX + \"&a+{amount} уровней!\";\n" +
               "\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        // ЧАТ\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        MessagesConfig.CHAT_CLEARED = MessagesConfig.PREFIX + \"&aЧат очищен!\";\n" +
               "\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        // СТАТИСТИКА\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        MessagesConfig.STATS_HEADER = \"&6=== Статистика ===\";\n" +
               "        MessagesConfig.STATS_HEALTH = \"&7HP: &c{health}/{max}\";\n" +
               "        MessagesConfig.STATS_FOOD = \"&7Еда: &e{food}\";\n" +
               "        MessagesConfig.STATS_LEVEL = \"&7Уровень: &a{level}\";\n" +
               "        MessagesConfig.STATS_WORLD = \"&7Мир: &b{world}\";\n" +
               "        MessagesConfig.STATS_POSITION = \"&7Позиция: &f{x} {y} {z}\";\n" +
               "        MessagesConfig.STATS_GAMEMODE = \"&7Режим: &d{gamemode}\";\n" +
               "        MessagesConfig.STATS_FOOTER = \"&6========================\";\n" +
               "\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        // ОШИБКИ\n" +
               "        // ══════════════════════════════════════════════════════════════\n" +
               "        MessagesConfig.ERROR_COMMAND = MessagesConfig.PREFIX + \"&cОшибка: {error}\";\n" +
               "        MessagesConfig.ERROR_ITEM = MessagesConfig.PREFIX + \"&cОшибка предмета: {error}\";\n" +
               "        MessagesConfig.ERROR_LOAD = MessagesConfig.PREFIX + \"&cОшибка загрузки: {error}\";\n" +
               "    }\n" +
               "}\n";
    }

    public static Main getInstance() { return instance; }

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

    /** Create a small YAML storage file under plugins/DC-CustomItems/storage/. */
    public YamlStorage createYamlStorage(String fileName) {
        return new YamlStorage(this, fileName);
    }
    public PlaceholderManager getPlaceholderManager() { return placeholderManager; }
    public ModuleManager getModuleManager() { return moduleManager; }

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
                    if (!cmd.getAliases().isEmpty()) pluginCmd.setAliases(cmd.getAliases());
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
            
            CustomCommand registeredCommand = apiItemRegistry.getCommand(name);
            command.setPermission(registeredCommand != null && registeredCommand.getPermission() != null
                ? registeredCommand.getPermission() : "dci.command." + name);
            command.setDescription("Custom command: " + name);
            command.setUsage("/" + name);
            if (registeredCommand != null && !registeredCommand.getAliases().isEmpty()) {
                command.setAliases(registeredCommand.getAliases());
            }
            
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
                Object registrationResult = registerMethod.invoke(commandMap, "dcustomitems", command);
                if (registrationResult instanceof Boolean && (Boolean) registrationResult) {
                    dynamicCommands.add(command);
                    getLogger().info("[API] ✅ Registered command: /" + name);
                } else {
                    getLogger().warning("[API] ❌ Command name is already occupied: /" + name);
                }

            } catch (Exception e) {
                getLogger().warning("[API] ❌ Failed to register /" + name + ": " + e.getMessage());
                e.printStackTrace();
            }
            
        } catch (Exception e) {
            getLogger().warning("[API] ❌ Reflection registration failed for /" + name + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Remove commands registered dynamically by the Java API before a reload. */
    public void unregisterCustomCommands() {
        if (dynamicCommands.isEmpty()) return;

        CommandMap commandMap = null;
        java.util.Map<?, ?> knownCommands = null;
        try {
            java.lang.reflect.Method getCommandMapMethod = getServer().getClass().getMethod("getCommandMap");
            Object commandMapObject = getCommandMapMethod.invoke(getServer());
            if (commandMapObject instanceof CommandMap) {
                commandMap = (CommandMap) commandMapObject;
                java.lang.reflect.Field knownCommandsField = null;
                Class<?> type = commandMap.getClass();
                while (type != null && knownCommandsField == null) {
                    try {
                        knownCommandsField = type.getDeclaredField("knownCommands");
                    } catch (NoSuchFieldException ignored) {
                        type = type.getSuperclass();
                    }
                }
                if (knownCommandsField != null) {
                    knownCommandsField.setAccessible(true);
                    Object value = knownCommandsField.get(commandMap);
                    if (value instanceof java.util.Map) {
                        knownCommands = (java.util.Map<?, ?>) value;
                    }
                }
            }
        } catch (Exception e) {
            getLogger().warning("[API] Could not inspect dynamic commands: " + e.getMessage());
        }

        Set<Command> failed = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Command command : new java.util.ArrayList<>(dynamicCommands)) {
            boolean removed = false;
            try {
                if (knownCommands != null) {
                    removed = knownCommands.entrySet().removeIf(entry -> entry.getValue() == command);
                }
                if (commandMap != null) {
                    removed = command.unregister(commandMap) || removed;
                }
            } catch (Exception e) {
                removed = false;
                getLogger().warning("[API] Failed to unregister dynamic command: " + e.getMessage());
            }
            if (!removed) failed.add(command);
        }

        dynamicCommands.clear();
        dynamicCommands.addAll(failed);
    }
}
