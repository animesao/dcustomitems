package me.dcplugin.dcustomitems.api.config;

import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;

/**
 * Конфигурация сообщений плагина
 * 
 * Редактируй этот файл чтобы изменить все сообщения!
 * 
 * Формат:
 * - Используй & для цветов
 * - {player} - имя игрока
 * - {item} - имя предмета
 * - {error} - текст ошибки
 * 
 * Пример:
 *   PREFIX = "&8[&6DCI&8] &r";
 *   ITEM_GIVEN = PREFIX + "&aПредмет {item} выдан игроку {player}";
 */
public class MessagesConfig {

    // ===== ПРЕФИКС =====
    public static String PREFIX = "&8[&6DCI&8] &r";

    // ===== ОБЩИЕ =====
    public static String NO_PERMISSION = PREFIX + "&cУ вас нет прав!";
    public static String PLAYER_NOT_FOUND = PREFIX + "&cИгрок {player} не найден!";
    public static String ITEM_NOT_FOUND = PREFIX + "&cПредмет {item} не найден!";

    // ===== ВЫДАЧА ПРЕДМЕТОВ =====
    public static String ITEM_GIVEN = PREFIX + "&aПредмет &e{item} &aвыдан игроку &e{player}&a!";
    public static String ITEM_RECEIVED = PREFIX + "&aВы получили: &e{item}";
    public static String SPECIFY_PLAYER = PREFIX + "&cУкажите игрока!";

    // ===== СПИСОК =====
    public static String LIST_HEADER = PREFIX + "&6=== Кастомные предметы ===";
    public static String LIST_ITEM = PREFIX + "&e{item} &7- &f{type}";
    public static String LIST_EMPTY = PREFIX + "&cНет загруженных предметов";
    public static String LIST_JAVA_API = PREFIX + "&7Java API: &e{count} предметов";

    // ===== ПЕРЕЗАГРУЗКА =====
    public static String RELOAD_START = PREFIX + "&eПерезагрузка...";
    public static String RELOAD_SUCCESS = PREFIX + "&a&lПлагин перезагружен!";
    public static String RELOAD_YAML = PREFIX + "&7YAML предметов: &e{count}";
    public static String RELOAD_JAVA_ITEMS = PREFIX + "&7Java предметов: &e{count}";
    public static String RELOAD_JAVA_COMMANDS = PREFIX + "&7Java команд: &e{count}";
    public static String RELOAD_JAVA_PLACEHOLDERS = PREFIX + "&7Java плейсхолдеров: &e{count}";
    public static String RELOAD_ERROR = PREFIX + "&cОшибка: {error}";

    // ===== ОБНОВЛЕНИЕ =====
    public static String UPDATE_CHECKING = PREFIX + "&eПроверка обновлений...";
    public static String UPDATE_LATEST = PREFIX + "&aВы используете последнюю версию! ({version})";
    public static String UPDATE_AVAILABLE = PREFIX + "&cДоступна новая версия: {version}";

    // ===== JAVА API =====
    public static String API_ITEMS_LOADED = PREFIX + "&7Java API: &e{items} предметов, {commands} команд, {placeholders} плейсхолдеров";

    // ===== БЕРСЕРК =====
    public static String BERSERK_ENABLED = PREFIX + "&4Вы в режиме БЕРСЕРКА!";
    public static String BERSERK_DISABLED = PREFIX + "&7Режим берсерка отключен.";
    public static String BERSERK_ALREADY = PREFIX + "&cВы уже в берсерке!";
    public static String BERSERK_NOT_ACTIVE = PREFIX + "&cВы не в берсерке!";

    // ===== ИСЦЕЛЕНИЕ =====
    public static String HEAL_SELF = PREFIX + "&aВы полностью исцелены!";
    public static String HEAL_OTHER = PREFIX + "&aВы исцелили {player}!";

    // ===== ТЕЛЕПОРТ =====
    public static String TP_SELF = PREFIX + "&bВы телепортированы к {player}!";

    // ===== ПОЛЁТ =====
    public static String FLY_ON = PREFIX + "&aПолёт включён!";
    public static String FLY_OFF = PREFIX + "&cПолёт выключен!";

    // ===== КОМАНДЫ =====
    public static String CMD_HEAL_DESCRIPTION = "Исцелить себя или игрока";
    public static String CMD_HEAL_USAGE = "/heal [игрок]";
    public static String CMD_HEAL_PERM = "dci.command.heal";

    public static String CMD_BERSERK_DESCRIPTION = "Режим берсерка";
    public static String CMD_BERSERK_USAGE = "/berserk";
    public static String CMD_BERSERK_PERM = "dci.command.berserk";

    public static String CMD_FLY_DESCRIPTION = "Включить полёт";
    public static String CMD_FLY_USAGE = "/fly";
    public static String CMD_FLY_PERM = "dci.command.fly";

    // ===== ПОМОЩЬ =====
    public static String HELP_HEADER = "&6=== DC-CustomItems Помощь ===";
    public static String HELP_CI = "&e/ci give <id> [игрок] &7- Выдать предмет";
    public static String HELP_CI_LIST = "&e/ci list &7- Список предметов";
    public static String HELP_CI_RELOAD = "&e/ci reload &7- Перезагрузить плагин";
    public static String HELP_API = "&e/api-item give <id> [игрок] &7- Выдать API предмет";
    public static String HELP_API_LIST = "&e/api-item list &7- Список API предметов";
    public static String HELP_CUSTOM_CMDS = "&eДоступные команды: {commands}";

    /**
     * Заменяет плейсхолдеры в сообщении
     */
    public static String format(String message, String... replacements) {
        if (message == null) return "";
        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Получить все сообщения (для вывода в консоль)
     */
    public static Map<String, String> getAllMessages() {
        Map<String, String> msgs = new HashMap<>();
        msgs.put("PREFIX", PREFIX);
        msgs.put("NO_PERMISSION", NO_PERMISSION);
        msgs.put("ITEM_GIVEN", ITEM_GIVEN);
        msgs.put("RELOAD_SUCCESS", RELOAD_SUCCESS);
        return msgs;
    }
}
