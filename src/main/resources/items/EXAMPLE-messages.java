import me.dcplugin.dcustomitems.api.config.MessagesConfig;

/**
 * 🔧 КОНФИГУРАЦИЯ СООБЩЕНИЙ
 * 
 * Положи этот файл в plugins/DC-CustomItems/items/
 * Сделай /ci reload
 * Все сообщения обновятся!
 * 
 * Формат:
 * - Используй & для цветов
 * - {player}, {item}, {error} - плейсхолдеры
 * 
 * Цвета:
 * - &0 - Чёрный
 * - &1 - Тёмно-синий
 * - &2 - Тёмно-зелёный
 * - &3 - Тёмно-бирюзовый
 * - &4 - Тёмно-красный
 * - &5 - Тёмно-фиолетовый
 * - &6 - Золотой
 * - &7 - Серый
 * - &8 - Тёмно-серый
 * - &9 - Синий
 * - &a - Зелёный
 * - &b - Бирюзовый
 * - &c - Красный
 * - &d - Розовый
 * - &e - Жёлтый
 * - &f - Белый
 * 
 * Форматирование:
 * - &l - Жирный
 * - &n - Подчёркнутый
 * - &o - Курсив
 * - &r - Сброс
 */
public class EXAMPLEmessages {

    public static void load() {
        // ===== ПРЕФИКС =====
        MessagesConfig.PREFIX = "&8[&6DCI&8] &r";

        // ===== ОБЩИЕ =====
        MessagesConfig.NO_PERMISSION = MessagesConfig.PREFIX + "&cУ вас нет прав!";
        MessagesConfig.PLAYER_NOT_FOUND = MessagesConfig.PREFIX + "&cИгрок {player} не найден!";
        MessagesConfig.ITEM_NOT_FOUND = MessagesConfig.PREFIX + "&cПредмет {item} не найден!";

        // ===== ВЫДАЧА ПРЕДМЕТОВ =====
        MessagesConfig.ITEM_GIVEN = MessagesConfig.PREFIX + "&aПредмет &e{item} &aвыдан игроку &e{player}&a!";
        MessagesConfig.ITEM_RECEIVED = MessagesConfig.PREFIX + "&aВы получили: &e{item}";

        // ===== ПЕРЕЗАГРУЗКА =====
        MessagesConfig.RELOAD_SUCCESS = MessagesConfig.PREFIX + "&a&lПлагин перезагружен!";
        MessagesConfig.RELOAD_YAML = MessagesConfig.PREFIX + "&7YAML предметов: &e{count}";
        MessagesConfig.RELOAD_JAVA_ITEMS = MessagesConfig.PREFIX + "&7Java предметов: &e{count}";
        MessagesConfig.RELOAD_JAVA_COMMANDS = MessagesConfig.PREFIX + "&7Java команд: &e{count}";
        MessagesConfig.RELOAD_JAVA_PLACEHOLDERS = MessagesConfig.PREFIX + "&7Java плейсхолдеров: &e{count}";

        // ===== ОБНОВЛЕНИЕ =====
        MessagesConfig.UPDATE_LATEST = MessagesConfig.PREFIX + "&aПоследняя версия! ({version})";
        MessagesConfig.UPDATE_AVAILABLE = MessagesConfig.PREFIX + "&cНовая версия: {version}";

        // ===== БЕРСЕРК =====
        MessagesConfig.BERSERK_ENABLED = MessagesConfig.PREFIX + "&4&l🔥 БЕРСЕРК АКТИВИРОВАН!";
        MessagesConfig.BERSERK_DISABLED = MessagesConfig.PREFIX + "&7Берсерк отключен.";

        // ===== ИСЦЕЛЕНИЕ =====
        MessagesConfig.HEAL_SELF = MessagesConfig.PREFIX + "&a❤ Вы полностью исцелены!";
        MessagesConfig.HEAL_OTHER = MessagesConfig.PREFIX + "&aВы исцелили &e{player}&a!";

        // ===== ПОМОЩЬ =====
        MessagesConfig.HELP_HEADER = "&6&l=== DC-CustomItems ===";
        MessagesConfig.HELP_CI = "&e/ci give <id> [игрок] &7- Выдать предмет";
        MessagesConfig.HELP_CI_LIST = "&e/ci list &7- Список предметов";
        MessagesConfig.HELP_CI_RELOAD = "&e/ci reload &7- Перезагрузить";
    }
}
