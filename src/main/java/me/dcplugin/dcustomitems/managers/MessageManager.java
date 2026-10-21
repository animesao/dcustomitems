package me.dcplugin.dcustomitems.managers;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.config.MessagesConfig;
import me.dcplugin.dcustomitems.utils.ColorUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Plugin message manager.
 *
 * The messages are stored in {@link MessagesConfig} (they can be overridden
 * from items/messages.java). Previously the path parameter of getMessage() was
 * completely ignored and the passed default was always returned — now the path
 * is really resolved, and the default remains the fallback.
 */
public class MessageManager {

    private final Main plugin;

    /** The built-in messages for the paths used in the plugin code. */
    private static final Map<String, String> BUILT_IN_MESSAGES = Map.of(
            "items.no-permission", "&cYou don't have permission to use this item.",
            "items.uses-depleted", "&cThe item was used up and is gone!",
            "plugin.update-available", "&e[CustomItems] &fA new version is available: &b{version}",
            "plugin.update-download", "&e[CustomItems] &fDownload the update from the plugin page"
    );

    /** The mapping of message paths to the MessagesConfig fields. */
    private static final Map<String, String> MESSAGES_CONFIG_ALIASES = Map.of(
            "plugin.no-permission", "NO_PERMISSION",
            "plugin.player-not-found", "PLAYER_NOT_FOUND",
            "plugin.item-not-found", "ITEM_NOT_FOUND",
            "plugin.unknown-command", "UNKNOWN_COMMAND",
            "plugin.only-for-players", "ONLY_FOR_PLAYERS"
    );

    public MessageManager(Main plugin) {
        this.plugin = plugin;
        // No longer loading messages.yml - use MessagesConfig.java instead
    }

    public void reloadMessages() {
        // Messages are now managed via MessagesConfig.java in items/
    }

    /**
     * The message for the given path.
     *
     * @param path          the message path (for example "plugin.no-permission")
     * @param defaultMessage the default value if the path is not found
     */
    public String getMessage(String path, String defaultMessage) {
        String resolved = resolve(path);
        if (resolved != null) return ColorUtils.colorize(resolved);
        return ColorUtils.colorize(defaultMessage);
    }

    /**
     * The message for the given path with PlaceholderAPI substitution for the player.
     */
    public String getMessage(String path, String defaultMessage, org.bukkit.entity.Player player) {
        String resolved = resolve(path);
        if (resolved != null) return ColorUtils.processMessage(player, resolved);
        return ColorUtils.processMessage(player, defaultMessage);
    }

    public String getMessage(String path) {
        return getMessage(path, "");
    }

    /**
     * Resolves the path into the message text (null if the path is unknown).
     */
    private String resolve(String path) {
        if (path == null || path.isEmpty()) return null;

        String builtIn = BUILT_IN_MESSAGES.get(path);
        if (builtIn != null) return builtIn;

        String alias = MESSAGES_CONFIG_ALIASES.get(path);
        if (alias != null) {
            String value = readMessagesConfigField(alias);
            if (value != null && !value.isEmpty()) return value;
        }
        return null;
    }

    /**
     * Reads a public static field of MessagesConfig by name.
     */
    private String readMessagesConfigField(String fieldName) {
        try {
            java.lang.reflect.Field field = MessagesConfig.class.getField(fieldName);
            Object value = field.get(null);
            return value instanceof String string ? string : null;
        } catch (Exception e) {
            return null;
        }
    }

    public void saveMessages() {
        // No longer needed
    }
}
