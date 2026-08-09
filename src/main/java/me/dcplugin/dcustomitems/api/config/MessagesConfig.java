package me.dcplugin.dcustomitems.api.config;

import org.bukkit.ChatColor;

/**
 * ПОЛНАЯ КОНФИГУРАЦИЯ СООБЩЕНИЙ ПЛАГИНА
 * 
 * Редактируй этот файл чтобы изменить ЛЮБОЕ сообщение!
 * 
 * Формат:
 * - &0-9, &a-f - цвета
 * - &l - жирный
 * - &n - подчёркнутый
 * - &o - курсив
 * - &k - обфусцированный
 * - &r - сброс
 * - {player}, {item}, {error}, {count}, {version} - плейсхолдеры
 */
public class MessagesConfig {

    // ══════════════════════════════════════════════════════════════
    // ПРЕФИКС
    // ══════════════════════════════════════════════════════════════
    public static String PREFIX = "&8[&6DCI&8] &r";

    // ══════════════════════════════════════════════════════════════
    // ОБЩИЕ СООБЩЕНИЯ
    // ══════════════════════════════════════════════════════════════
    public static String NO_PERMISSION = PREFIX + "&cУ вас нет прав на использование этой команды!";
    public static String PLAYER_NOT_FOUND = PREFIX + "&cИгрок не найден!";
    public static String ITEM_NOT_FOUND = PREFIX + "&cПредмет с ID '{item}' не найден!";
    public static String UNKNOWN_COMMAND = PREFIX + "&cНеизвестная команда!";
    public static String ONLY_FOR_PLAYERS = PREFIX + "&cЭта команда только для игроков!";

    // ══════════════════════════════════════════════════════════════
    // КОМАНДЫ /ci
    // ══════════════════════════════════════════════════════════════
    public static String CI_HEADER = "&6=== DC-CustomItems ===";
    public static String CI_HELP_GIVE = "&e/ci give <предмет> [игрок] &7- Выдать предмет";
    public static String CI_HELP_LIST = "&e/ci list &7- Список предметов";
    public static String CI_HELP_RELOAD = "&e/ci reload &7- Перезагрузить плагин";
    public static String CI_HELP_UPDATE = "&e/ci update &7- Проверить обновления";
    public static String CI_GIVE_USAGE = PREFIX + "&cИспользование: /ci give <предмет> [игрок]";
    public static String CI_GIVE_SELF = PREFIX + "&aПредмет &e{item} &aвыдан вам!";
    public static String CI_GIVE_OTHER = PREFIX + "&aПредмет &e{item} &aвыдан игроку &e{player}&a!";
    public static String CI_GIVE_RECEIVED = PREFIX + "&aВы получили кастомный предмет: &e{item}";
    public static String CI_GIVE_NO_PERM = PREFIX + "&cУ вас нет прав на этот предмет!";

    // ══════════════════════════════════════════════════════════════
    // КОМАНДЫ /ci list
    // ══════════════════════════════════════════════════════════════
    public static String LIST_HEADER = PREFIX + "&6=== Список кастомных предметов ===";
    public static String LIST_ITEM = PREFIX + "&e{item} &7- &f{type} &7({slot})";
    public static String LIST_EMPTY = PREFIX + "&cНет загруженных предметов";

    // ══════════════════════════════════════════════════════════════
    // КОМАНДЫ /ci reload
    // ══════════════════════════════════════════════════════════════
    public static String RELOAD_START = PREFIX + "&eПерезагрузка...";
    public static String RELOAD_SUCCESS = PREFIX + "&a&lПлагин перезагружен!";
    public static String RELOAD_YAML = PREFIX + "&7YAML предметов: &e{count}";
    public static String RELOAD_JAVA_ITEMS = PREFIX + "&7Java предметов: &e{count}";
    public static String RELOAD_JAVA_COMMANDS = PREFIX + "&7Java команд: &e{count}";
    public static String RELOAD_JAVA_PLACEHOLDERS = PREFIX + "&7Java плейсхолдеров: &e{count}";
    public static String RELOAD_ERROR = PREFIX + "&cОшибка при перезагрузке: {error}";

    // ══════════════════════════════════════════════════════════════
    // ОБНОВЛЕНИЯ
    // ══════════════════════════════════════════════════════════════
    public static String UPDATE_CHECKING = PREFIX + "&eПроверка обновлений...";
    public static String UPDATE_LATEST = PREFIX + "&aВы используете последнюю версию! ({version})";
    public static String UPDATE_AVAILABLE = PREFIX + "&cДоступна новая версия: {version}";
    public static String UPDATE_DOWNLOAD = PREFIX + "&bСкачать: https://github.com/animesao/dcustomitems/releases";
    public static String UPDATE_ERROR = PREFIX + "&cОшибка при проверке обновлений: {error}";
    public static String UPDATE_TITLE = "&6&lDC-CustomItems";
    public static String UPDATE_SUBTITLE = "&7Доступно обновление!";
    public static String UPDATE_CURRENT = "&7Текущая версия: &e{current}";
    public static String UPDATE_NEW = "&7Новая версия: &a{new}";
    public static String UPDATE_LINK = "&7Скачать: &b{url}";

    // ══════════════════════════════════════════════════════════════
    // КОМАНДЫ /api-item
    // ══════════════════════════════════════════════════════════════
    public static String API_HEADER = "&6=== Java API Предметы ===";
    public static String API_HELP_GIVE = "&e/api-item give <id> [игрок] &7- Выдать предмет";
    public static String API_HELP_LIST = "&e/api-item list &7- Список предметов";
    public static String API_HELP_INFO = "&e/api-item info <id> &7- Информация о предмете";
    public static String API_GIVE_USAGE = PREFIX + "&cИспользование: /api-item give <id> [игрок]";
    public static String API_GIVE_SELF = PREFIX + "&aВыдан предмет: &e{item}";
    public static String API_GIVE_OTHER = PREFIX + "&aВыдан &e{item} &aигроку &e{player}";
    public static String API_INFO_HEADER = PREFIX + "&6=== Информация о предмете ===";
    public static String API_INFO_ID = "&eID: &f{id}";
    public static String API_INFO_NAME = "&eНазвание: {name}";
    public static String API_INFO_MATERIAL = "&eМатериал: &f{material}";
    public static String API_INFO_TYPE = "&eТип: &f{type}";
    public static String API_INFO_SLOT = "&eСлот: &f{slot}";
    public static String API_INFO_COOLDOWN = "&eКулдаун: &f{cooldown}мс";
    public static String API_INFO_UNBREAKABLE = "&eНеломаемость: &f{value}";
    public static String API_INFO_GLOWING = "&eСвечение: &f{value}";
    public static String API_INFO_MODEL = "&eМодель: &f{model}";
    public static String API_INFO_PERMISSION = "&eПраво: &f{perm}";
    public static String API_LIST_EMPTY = PREFIX + "&cНет загруженных Java API предметов";

    // ══════════════════════════════════════════════════════════════
    // КУЛДАУНЫ
    // ══════════════════════════════════════════════════════════════
    public static String COOLDOWN = PREFIX + "&cПодождите {seconds} секунд!";

    // ══════════════════════════════════════════════════════════════
    // ИСПОЛЬЗОВАНИЕ ПРЕДМЕТОВ
    // ══════════════════════════════════════════════════════════════
    public static String USES_DEPLETED = PREFIX + "&cПредмет использован до конца и пропал!";
    public static String USES_LEFT = PREFIX + "&7Осталось использований: &e{uses}";

    // ══════════════════════════════════════════════════════════════
    // ДЕЙСТВИЯ (для YAML предметов)
    // ══════════════════════════════════════════════════════════════
    public static String ACTION_HEAL = "&a❤ Вы исцелены на {amount} сердец!";
    public static String ACTION_TELEPORT = "&d✨ Вы телепортированы!";
    public static String ACTION_GIVE = "&a+{amount} {material}";
    public static String ACTION_REMOVE = "&c-{amount} {material}";
    public static String ACTION_ERROR = PREFIX + "&cОшибка при выполнении действия: {error}";

    // ══════════════════════════════════════════════════════════════
    // БЕРСЕРК (пример команды)
    // ══════════════════════════════════════════════════════════════
    public static String BERSERK_ENABLED = PREFIX + "&4&l🔥 БЕРСЕРК АКТИВИРОВАН!";
    public static String BERSERK_DISABLED = PREFIX + "&7Режим берсерка отключен.";
    public static String BERSERK_ALREADY = PREFIX + "&cВы уже в берсерке!";
    public static String BERSERK_NOT_ACTIVE = PREFIX + "&cВы не в берсерке!";
    public static String BERSERK_STATUS_HEADER = "&6=== БЕРСЕРК ===";
    public static String BERSERK_STATUS_ACTIVE = "&7Статус: &aАктивен";
    public static String BERSERK_STATUS_INACTIVE = "&7Статус: &cВыключен";
    public static String BERSERK_HP = "&7HP: &c{current}/{max} ({percent}%)";
    public static String BERSERK_BONUS = "&7Бонус урона: &c+{bonus}";
    public static String BERSERK_DEATH = "&4💀 {player} взорвался в ярости!";

    // ══════════════════════════════════════════════════════════════
    // ИСЦЕЛЕНИЕ
    // ══════════════════════════════════════════════════════════════
    public static String HEAL_SELF = PREFIX + "&a❤ Вы полностью исцелены!";
    public static String HEAL_OTHER = PREFIX + "&aВы исцелили &e{player}&a!";
    public static String HEAL_AMOUNT = PREFIX + "&a❤ Вы исцелены на &e{amount} &aсердец!";

    // ══════════════════════════════════════════════════════════════
    // ТЕЛЕПОРТАЦИЯ
    // ══════════════════════════════════════════════════════════════
    public static String TELEPORT_SELF = PREFIX + "&bВы телепортированы к &e{player}&b!";
    public static String TELEPORT_TARGET = PREFIX + "&b{player} телепортировался к вам!";

    // ══════════════════════════════════════════════════════════════
    // ПОЛЁТ
    // ══════════════════════════════════════════════════════════════
    public static String FLY_ON = PREFIX + "&aПолёт включён!";
    public static String FLY_OFF = PREFIX + "&cПолёт выключен!";

    // ══════════════════════════════════════════════════════════════
    // ГЕЙМОД
    // ══════════════════════════════════════════════════════════════
    public static String GAMEMODE_SURVIVAL = PREFIX + "&aРежим выживания включён!";
    public static String GAMEMODE_CREATIVE = PREFIX + "&aКреативный режим включён!";
    public static String GAMEMODE_ADVENTURE = PREFIX + "&aРежим приключения включён!";
    public static String GAMEMODE_SPECTATOR = PREFIX + "&aРежим наблюдателя включён!";

    // ══════════════════════════════════════════════════════════════
    // БЕССМЕРТИЕ
    // ══════════════════════════════════════════════════════════════
    public static String GOD_ON = PREFIX + "&6Вы бессмертны на {seconds} секунд!";
    public static String GOD_OFF = PREFIX + "&7Бессмертие отключено.";

    // ══════════════════════════════════════════════════════════════
    // НЕВИДИМОСТЬ
    // ══════════════════════════════════════════════════════════════
    public static String INVISIBLE_ON = PREFIX + "&7Вы стали невидимыми!";
    public static String INVISIBLE_OFF = PREFIX + "&7Невидимость отключена.";

    // ══════════════════════════════════════════════════════════════
    // СКОРОСТЬ
    // ══════════════════════════════════════════════════════════════
    public static String SPEED_ON = PREFIX + "&bСкорость увеличена!";
    public static String SPEED_OFF = PREFIX + "&7Скорость нормальная.";

    // ══════════════════════════════════════════════════════════════
    // ГОЛОД
    // ══════════════════════════════════════════════════════════════
    public static String FEED_SELF = PREFIX + "&aВы накормлены!";
    public static String FEED_OTHER = PREFIX + "&aВы накормили &e{player}&a!";

    // ══════════════════════════════════════════════════════════════
    // ОПЫТ
    // ══════════════════════════════════════════════════════════════
    public static String XP_GIVEN = PREFIX + "&aВыдано &e{amount} &aуровней опыта!";

    // ══════════════════════════════════════════════════════════════
    // ОЧИСТКА ЧАТА
    // ══════════════════════════════════════════════════════════════
    public static String CHAT_CLEARED = PREFIX + "&aЧат очищен!";

    // ══════════════════════════════════════════════════════════════
    // СТАТИСТИКА
    // ══════════════════════════════════════════════════════════════
    public static String STATS_HEADER = "&6=== Ваша статистика ===";
    public static String STATS_HEALTH = "&7Здоровье: &c{health}/{max}";
    public static String STATS_FOOD = "&7Еда: &e{food}";
    public static String STATS_LEVEL = "&7Уровень: &a{level}";
    public static String STATS_WORLD = "&7Мир: &b{world}";
    public static String STATS_POSITION = "&7Позиция: &f{x} {y} {z}";
    public static String STATS_GAMEMODE = "&7Режим: &d{gamemode}";
    public static String STATS_FOOTER = "&6========================";

    // ══════════════════════════════════════════════════════════════
    // ОШИБКИ
    // ══════════════════════════════════════════════════════════════
    public static String ERROR_COMMAND = PREFIX + "&cОшибка при выполнении команды: {error}";
    public static String ERROR_ITEM = PREFIX + "&cОшибка при создании предмета: {error}";
    public static String ERROR_LOAD = PREFIX + "&cОшибка загрузки: {error}";

    /**
     * Форматирует сообщение с плейсхолдерами
     */
    public static String format(String message, String... replacements) {
        if (message == null) return "";
        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Получить сообщение с цветами
     */
    public static String colorize(String message) {
        return message == null ? "" : ChatColor.translateAlternateColorCodes('&', message);
    }
}
