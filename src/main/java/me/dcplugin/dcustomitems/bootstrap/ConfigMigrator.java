package me.dcplugin.dcustomitems.bootstrap;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.config.MessagesConfig;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Миграция конфигурации и создание файлов по умолчанию.
 * Вынесен из Main для разделения ответственности.
 */
public class ConfigMigrator {

    private final Main plugin;

    public ConfigMigrator(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Создать items/messages.java если не существует
     */
    public void createDefaultMessagesFile() {
        File itemsDir = new File(plugin.getDataFolder(), "items");
        if (!itemsDir.exists()) itemsDir.mkdirs();

        File messagesFile = new File(itemsDir, "messages.java");
        if (!messagesFile.exists()) {
            try {
                FileWriter writer = new FileWriter(messagesFile);
                writer.write(generateDefaultMessagesContent());
                writer.close();
                plugin.getLogger().info("[Config] Created default messages.java");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to create messages.java: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Проверить наличие предметов в config.yml и предложить миграцию
     */
    public void checkConfigMigration() {
        Set<String> configKeys = plugin.getConfig().getKeys(false);
        Set<String> settingKeys = Set.of(
                "debug-mode", "database", "set-bonuses",
                "update-available", "latest-version",
                "disable-worlds", "whitelist-blocks"
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
     * Содержимое файла messages.java по умолчанию
     */
    private String generateDefaultMessagesContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("import me.dcplugin.dcustomitems.api.config.MessagesConfig;\n");
        sb.append("\n");
        sb.append("/**\n");
        sb.append(" * DC-CustomItems - Сообщения плагина\n");
        sb.append(" * Используй /ci reload после изменений\n");
        sb.append(" */\n");
        sb.append("public class messages {\n");
        sb.append("\n");
        sb.append("    public static void load() {\n");
        sb.append("        MessagesConfig.PREFIX = \"&8[&6DCI&8] &r\";\n");
        sb.append("        MessagesConfig.NO_PERMISSION = MessagesConfig.PREFIX + \"&cНет прав!\";\n");
        sb.append("        MessagesConfig.PLAYER_NOT_FOUND = MessagesConfig.PREFIX + \"&cИгрок не найден!\";\n");
        sb.append("        MessagesConfig.ITEM_NOT_FOUND = MessagesConfig.PREFIX + \"&cПредмет '{item}' не найден!\";\n");
        sb.append("        MessagesConfig.CI_HEADER = \"&6=== DC-CustomItems ===\";\n");
        sb.append("        MessagesConfig.CI_HELP_RELOAD = \"&e/ci reload &7- Перезагрузить\";\n");
        sb.append("        MessagesConfig.RELOAD_SUCCESS = MessagesConfig.PREFIX + \"&a&lПерезагружено!\";\n");
        sb.append("        MessagesConfig.RELOAD_YAML = MessagesConfig.PREFIX + \"&7YAML: &e{count}\";\n");
        sb.append("        MessagesConfig.RELOAD_JAVA_ITEMS = MessagesConfig.PREFIX + \"&7Java: &e{count}\";\n");
        sb.append("        MessagesConfig.RELOAD_ERROR = MessagesConfig.PREFIX + \"&cОшибка: {error}\";\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }
}
