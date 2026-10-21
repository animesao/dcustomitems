package me.dcplugin.dcustomitems.bootstrap;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.config.MessagesConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Configuration migration and creation of default files.
 * Extracted from Main to separate responsibilities.
 */
public class ConfigMigrator {

    private final Main plugin;

    public ConfigMigrator(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Create items/messages.java if it does not exist
     */
    public void createDefaultMessagesFile() {
        File itemsDir = new File(plugin.getDataFolder(), "items");
        if (!itemsDir.exists()) itemsDir.mkdirs();

        File messagesFile = new File(itemsDir, "messages.java");
        if (messagesFile.exists()) return;

        try {
            // Explicit UTF-8: FileWriter used the system encoding,
            // and on Windows with cp1251 the Russian texts in messages.java
            // turned into garbled characters.
            java.nio.file.Files.write(messagesFile.toPath(),
                    generateDefaultMessagesContent().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            plugin.getLogger().info("[Config] Created default messages.java");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create messages.java: " + e.getMessage(), e);
        }
    }

    /**
     * Check for items in config.yml and suggest a migration
     */
    public void checkConfigMigration() {
        Set<String> configKeys = plugin.getConfig().getKeys(false);
        Set<String> settingKeys = Set.of(
                "debug-mode", "database", "set-bonuses",
                "update-available", "latest-version",
                "disable-worlds", "whitelist-blocks",
                "java-compilation", "backup", "extract-default-modules"
        );
        List<String> itemKeys = new ArrayList<>();
        for (String key : configKeys) {
            if (!settingKeys.contains(key)) {
                itemKeys.add(key);
            }
        }
        if (!itemKeys.isEmpty()) {
            plugin.getLogger().info("=========================================");
            plugin.getLogger().info("[Migration] Found " + itemKeys.size() + " item(s) in config.yml:");
            for (String key : itemKeys) {
                plugin.getLogger().info("  - " + key);
            }
            plugin.getLogger().info("");
            plugin.getLogger().info("[Migration] Items are now loaded from items/ folder.");
            plugin.getLogger().info("[Migration] To migrate, move each item to its own YAML file:");
            plugin.getLogger().info("  1. Create plugins/DC-CustomItems/items/<item-name>.yml");
            plugin.getLogger().info("  2. Copy the item definition from config.yml");
            plugin.getLogger().info("  3. Remove the item from config.yml");
            plugin.getLogger().info("[Migration] Items in config.yml will still work for now.");
            plugin.getLogger().info("=========================================");
        }
    }

    /**
     * The default content of the messages.java file
     */
    private String generateDefaultMessagesContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("import me.dcplugin.dcustomitems.api.config.MessagesConfig;\n");
        sb.append("\n");
        sb.append("/**\n");
        sb.append(" * DC-CustomItems - Plugin messages\n");
        sb.append(" * Run /ci reload after editing\n");
        sb.append(" */\n");
        sb.append("public class messages {\n");
        sb.append("\n");
        sb.append("    public static void load() {\n");
        sb.append("        MessagesConfig.PREFIX = \"&8[&6DCI&8] &r\";\n");
        sb.append("        MessagesConfig.NO_PERMISSION = MessagesConfig.PREFIX + \"&cNo permission!\";\n");
        sb.append("        MessagesConfig.PLAYER_NOT_FOUND = MessagesConfig.PREFIX + \"&cPlayer not found!\";\n");
        sb.append("        MessagesConfig.ITEM_NOT_FOUND = MessagesConfig.PREFIX + \"&cItem '{item}' not found!\";\n");
        sb.append("        MessagesConfig.CI_HEADER = \"&6=== DC-CustomItems ===\";\n");
        sb.append("        MessagesConfig.CI_HELP_RELOAD = \"&e/ci reload &7- Reload\";\n");
        sb.append("        MessagesConfig.RELOAD_SUCCESS = MessagesConfig.PREFIX + \"&a&lReloaded!\";\n");
        sb.append("        MessagesConfig.RELOAD_YAML = MessagesConfig.PREFIX + \"&7YAML: &e{count}\";\n");
        sb.append("        MessagesConfig.RELOAD_JAVA_ITEMS = MessagesConfig.PREFIX + \"&7Java: &e{count}\";\n");
        sb.append("        MessagesConfig.RELOAD_ERROR = MessagesConfig.PREFIX + \"&cError: {error}\";\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }
}
