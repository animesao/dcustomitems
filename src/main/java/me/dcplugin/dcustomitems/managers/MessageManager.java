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
 * Менеджер сообщений плагина.
 *
 * Сообщения хранятся в {@link MessagesConfig} (их можно переопределить
 * из items/messages.java). Раньше параметр path в getMessage() полностью
 * игнорировался и всегда возвращался переданный default — теперь путь
 * действительно резолвится, а default остаётся запасным вариантом.
 */
public class MessageManager {

    private final Main plugin;

    /** Встроенные сообщения по путям, которые используются в коде плагина. */
    private static final Map<String, String> BUILT_IN_MESSAGES = Map.of(
            "items.no-permission", "&cУ вас нет прав на использование этого предмета.",
            "items.uses-depleted", "&cПредмет использован до конца и пропал!",
            "plugin.update-available", "&e[CustomItems] &fДоступна новая версия: &b{version}",
            "plugin.update-download", "&e[CustomItems] &fСкачать обновление на сайте плагина"
    );

    /** Соответствие путей сообщений полям MessagesConfig. */
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
     * Сообщение по пути.
     *
     * @param path          путь сообщения (например "plugin.no-permission")
     * @param defaultMessage значение по умолчанию, если путь не найден
     */
    public String getMessage(String path, String defaultMessage) {
        String resolved = resolve(path);
        if (resolved != null) return ColorUtils.colorize(resolved);
        return ColorUtils.colorize(defaultMessage);
    }

    /**
     * Сообщение по пути с подстановкой PlaceholderAPI для игрока.
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
     * Разрешает путь в текст сообщения (null, если путь неизвестен).
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
     * Читает публичное статическое поле MessagesConfig по имени.
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
