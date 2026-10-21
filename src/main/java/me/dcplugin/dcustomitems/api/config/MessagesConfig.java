package me.dcplugin.dcustomitems.api.config;

import org.bukkit.ChatColor;

/**
 * Message configuration of the plugin.
 *
 * Used for:
 * - Messages of the /ci reload command
 * - Common messages (errors, permissions)
 * - Messages from modules (via format())
 */
public class MessagesConfig {

    // ══════════════════════════════════════════════════════════════
    // PREFIX
    // ══════════════════════════════════════════════════════════════
    public static String PREFIX = "&8[&6DCI&8] &r";

    // ══════════════════════════════════════════════════════════════
    // COMMON MESSAGES
    // ══════════════════════════════════════════════════════════════
    public static String NO_PERMISSION = PREFIX + "&cYou don't have permission!";
    public static String PLAYER_NOT_FOUND = PREFIX + "&cPlayer not found!";
    public static String ITEM_NOT_FOUND = PREFIX + "&cItem '{item}' not found!";
    public static String UNKNOWN_COMMAND = PREFIX + "&cUnknown command!";
    public static String ONLY_FOR_PLAYERS = PREFIX + "&cPlayers only!";

    // ══════════════════════════════════════════════════════════════
    // /ci reload COMMAND
    // ══════════════════════════════════════════════════════════════
    public static String CI_HEADER = "&6=== DC-CustomItems ===";
    public static String CI_HELP_RELOAD = "&e/ci reload &7- Reload the plugin";
    public static String RELOAD_SUCCESS = PREFIX + "&a&lPlugin reloaded!";
    public static String RELOAD_YAML = PREFIX + "&7YAML items: &e{count}";
    public static String RELOAD_JAVA_ITEMS = PREFIX + "&7Java items: &e{count}";
    public static String RELOAD_JAVA_COMMANDS = PREFIX + "&7Java commands: &e{count}";
    public static String RELOAD_JAVA_PLACEHOLDERS = PREFIX + "&7Java placeholders: &e{count}";
    public static String RELOAD_ERROR = PREFIX + "&cError: {error}";

    // ══════════════════════════════════════════════════════════════
    // FORMATTING
    // ══════════════════════════════════════════════════════════════

    /**
     * Formats a message with placeholders.
     *
     * Example:
     *   format("Hello, {player}!", "{player}", "Steve")
     *   → "Hello, Steve!"
     */
    public static String format(String message, String... replacements) {
        if (message == null) return "";
        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Replaces colour codes (& → §).
     */
    public static String colorize(String message) {
        return message == null ? "" : ChatColor.translateAlternateColorCodes('&', message);
    }
}
