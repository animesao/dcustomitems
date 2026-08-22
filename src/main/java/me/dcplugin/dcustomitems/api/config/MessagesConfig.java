package me.dcplugin.dcustomitems.api.config;

import org.bukkit.ChatColor;

/**
 * Конфигурация сообщений плагина.
 *
 * Используется для:
 * - Сообщений команды /ci reload
 * - Общих сообщений (ошибки, права)
 * - Сообщений из модулей (через format())
 */
public class MessagesConfig {

    // ══════════════════════════════════════════════════════════════
    // ПРЕФИКС
    // ══════════════════════════════════════════════════════════════
    public static String PREFIX = "&8[&6DCI&8] &r";

    // ══════════════════════════════════════════════════════════════
    // ОБЩИЕ СООБЩЕНИЯ
    // ══════════════════════════════════════════════════════════════
    public static String NO_PERMISSION = PREFIX + "&cУ вас нет прав!";
    public static String PLAYER_NOT_FOUND = PREFIX + "&cИгрок не найден!";
    public static String ITEM_NOT_FOUND = PREFIX + "&cПредмет '{item}' не найден!";
    public static String UNKNOWN_COMMAND = PREFIX + "&cНеизвестная команда!";
    public static String ONLY_FOR_PLAYERS = PREFIX + "&cТолько для игроков!";

    // ══════════════════════════════════════════════════════════════
    // КОМАНДА /ci reload
    // ══════════════════════════════════════════════════════════════
    public static String CI_HEADER = "&6=== DC-CustomItems ===";
    public static String CI_HELP_RELOAD = "&e/ci reload &7- Перезагрузить плагин";
    public static String RELOAD_SUCCESS = PREFIX + "&a&lПлагин перезагружен!";
    public static String RELOAD_YAML = PREFIX + "&7YAML предметов: &e{count}";
    public static String RELOAD_JAVA_ITEMS = PREFIX + "&7Java предметов: &e{count}";
    public static String RELOAD_JAVA_COMMANDS = PREFIX + "&7Java команд: &e{count}";
    public static String RELOAD_JAVA_PLACEHOLDERS = PREFIX + "&7Java плейсхолдеров: &e{count}";
    public static String RELOAD_ERROR = PREFIX + "&cОшибка: {error}";

    // ══════════════════════════════════════════════════════════════
    // ФОРМАТИРОВАНИЕ
    // ══════════════════════════════════════════════════════════════

    /**
     * Форматирует сообщение с плейсхолдерами.
     *
     * Пример:
     *   format("Привет, {player}!", "{player}", "Steve")
     *   → "Привет, Steve!"
     */
    public static String format(String message, String... replacements) {
        if (message == null) return "";
        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Заменяет цветовые коды (& → §).
     */
    public static String colorize(String message) {
        return message == null ? "" : ChatColor.translateAlternateColorCodes('&', message);
    }
}
