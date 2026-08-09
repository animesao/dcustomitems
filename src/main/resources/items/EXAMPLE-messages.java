import me.dcplugin.dcustomitems.api.config.MessagesConfig;

/**
 * 🔧 КОНФИГУРАЦИЯ ВСЕХ СООБЩЕНИЙ ПЛАГИНА
 * 
 * Положи этот файл в plugins/DC-CustomItems/items/
 * Сделай /ci reload
 * Все сообщения обновятся!
 * 
 * Формат:
 * - &0-9, &a-f - цвета
 * - &l - жирный
 * - &n - подчёркнутый
 * - &o - курсив
 * - &r - сброс
 * 
 * Плейсхолдеры:
 * - {player} - имя игрока
 * - {item} - имя предмета
 * - {error} - текст ошибки
 * - {count} - количество
 * - {version} - версия
 * - {amount} - количество
 * - {health} - здоровье
 * - {max} - максимум
 * - {food} - еда
 * - {level} - уровень
 * - {world} - мир
 * - {x}, {y}, {z} - координаты
 * - {gamemode} - режим игры
 * - {seconds} - секунды
 * - {uses} - использования
 * - {bonus} - бонус
 */
public class EXAMPLEmessages {

    public static void load() {
        // ══════════════════════════════════════════════════════════════
        // ПРЕФИКС
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.PREFIX = "&8[&6MyPlugin&8] &r";

        // ══════════════════════════════════════════════════════════════
        // ОБЩИЕ
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.NO_PERMISSION = MessagesConfig.PREFIX + "&cНет прав!";
        MessagesConfig.PLAYER_NOT_FOUND = MessagesConfig.PREFIX + "&cИгрок не найден!";
        MessagesConfig.ITEM_NOT_FOUND = MessagesConfig.PREFIX + "&cПредмет не найден!";

        // ══════════════════════════════════════════════════════════════
        // /ci КОМАНДЫ
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.CI_HEADER = "&6&l=== Мои Предметы ===";
        MessagesConfig.CI_HELP_GIVE = "&e/ci give <id> [игрок] &7- Выдать предмет";
        MessagesConfig.CI_HELP_LIST = "&e/ci list &7- Список";
        MessagesConfig.CI_HELP_RELOAD = "&e/ci reload &7- Перезагрузить";

        MessagesConfig.CI_GIVE_SELF = MessagesConfig.PREFIX + "&aВыдан: &e{item}";
        MessagesConfig.CI_GIVE_OTHER = MessagesConfig.PREFIX + "&aВыдан &e{item} &aигроку &e{player}";
        MessagesConfig.CI_GIVE_RECEIVED = MessagesConfig.PREFIX + "&aВы получили: &e{item}";

        // ══════════════════════════════════════════════════════════════
        // СПИСОК
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.LIST_HEADER = MessagesConfig.PREFIX + "&6=== Список ===";
        MessagesConfig.LIST_ITEM = MessagesConfig.PREFIX + "&e{item} &7- &f{type}";
        MessagesConfig.LIST_EMPTY = MessagesConfig.PREFIX + "&cПусто";

        // ══════════════════════════════════════════════════════════════
        // ПЕРЕЗАГРУЗКА
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.RELOAD_SUCCESS = MessagesConfig.PREFIX + "&a&lГотово!";
        MessagesConfig.RELOAD_YAML = MessagesConfig.PREFIX + "&7YAML: &e{count}";
        MessagesConfig.RELOAD_JAVA_ITEMS = MessagesConfig.PREFIX + "&7Items: &e{count}";
        MessagesConfig.RELOAD_JAVA_COMMANDS = MessagesConfig.PREFIX + "&7Commands: &e{count}";
        MessagesConfig.RELOAD_JAVA_PLACEHOLDERS = MessagesConfig.PREFIX + "&7Placeholders: &e{count}";

        // ══════════════════════════════════════════════════════════════
        // ОБНОВЛЕНИЯ
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.UPDATE_LATEST = MessagesConfig.PREFIX + "&aПоследняя версия! ({version})";
        MessagesConfig.UPDATE_AVAILABLE = MessagesConfig.PREFIX + "&cНовая версия: {version}";

        // ══════════════════════════════════════════════════════════════
        // /api-item
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.API_HEADER = MessagesConfig.PREFIX + "&6=== API ===";
        MessagesConfig.API_GIVE_SELF = MessagesConfig.PREFIX + "&aВыдан: &e{item}";
        MessagesConfig.API_INFO_HEADER = MessagesConfig.PREFIX + "&6=== Инфо ===";

        // ══════════════════════════════════════════════════════════════
        // КУЛДАУНЫ
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.COOLDOWN = MessagesConfig.PREFIX + "&cПодождите {seconds} сек!";

        // ══════════════════════════════════════════════════════════════
        // ДЕЙСТВИЯ
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.ACTION_HEAL = "&a❤ +{amount} HP";
        MessagesConfig.ACTION_TELEPORT = "&d✨ Телепорт!";
        MessagesConfig.ACTION_GIVE = "&a+{amount} {material}";
        MessagesConfig.ACTION_REMOVE = "&c-{amount} {material}";

        // ══════════════════════════════════════════════════════════════
        // БЕРСЕРК
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.BERSERK_ENABLED = MessagesConfig.PREFIX + "&4🔥 БЕРСЕРК!";
        MessagesConfig.BERSERK_DISABLED = MessagesConfig.PREFIX + "&7Выкл.";
        MessagesConfig.BERSERK_ALREADY = MessagesConfig.PREFIX + "&cУже активен!";
        MessagesConfig.BERSERK_NOT_ACTIVE = MessagesConfig.PREFIX + "&cНе активен!";

        // ══════════════════════════════════════════════════════════════
        // ИСЦЕЛЕНИЕ
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.HEAL_SELF = MessagesConfig.PREFIX + "&a❤ Исцелён!";
        MessagesConfig.HEAL_OTHER = MessagesConfig.PREFIX + "&aИсцелил {player}";
        MessagesConfig.HEAL_AMOUNT = MessagesConfig.PREFIX + "&a+{amount} HP";

        // ══════════════════════════════════════════════════════════════
        // ТЕЛЕПОРТ
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.TELEPORT_SELF = MessagesConfig.PREFIX + "&bТелепорт к {player}";

        // ══════════════════════════════════════════════════════════════
        // ПОЛЁТ
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.FLY_ON = MessagesConfig.PREFIX + "&aПолёт вкл";
        MessagesConfig.FLY_OFF = MessagesConfig.PREFIX + "&cПолёт выкл";

        // ══════════════════════════════════════════════════════════════
        // ГЕЙМОД
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.GAMEMODE_SURVIVAL = MessagesConfig.PREFIX + "&aВыживание";
        MessagesConfig.GAMEMODE_CREATIVE = MessagesConfig.PREFIX + "&aКреатив";
        MessagesConfig.GAMEMODE_ADVENTURE = MessagesConfig.PREFIX + "&aПриключение";
        MessagesConfig.GAMEMODE_SPECTATOR = MessagesConfig.PREFIX + "&aНаблюдатель";

        // ══════════════════════════════════════════════════════════════
        // БЕССМЕРТИЕ
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.GOD_ON = MessagesConfig.PREFIX + "&6Бессмертие на {seconds}с!";
        MessagesConfig.GOD_OFF = MessagesConfig.PREFIX + "&7Бессмертие выкл.";

        // ══════════════════════════════════════════════════════════════
        // НЕВИДИМОСТЬ
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.INVISIBLE_ON = MessagesConfig.PREFIX + "&7Невидимость вкл";
        MessagesConfig.INVISIBLE_OFF = MessagesConfig.PREFIX + "&7Невидимость выкл";

        // ══════════════════════════════════════════════════════════════
        // СКОРОСТЬ
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.SPEED_ON = MessagesConfig.PREFIX + "&bСкорость вкл";
        MessagesConfig.SPEED_OFF = MessagesConfig.PREFIX + "&7Скорость выкл";

        // ══════════════════════════════════════════════════════════════
        // ГОЛОД
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.FEED_SELF = MessagesConfig.PREFIX + "&aВы накормлены!";
        MessagesConfig.FEED_OTHER = MessagesConfig.PREFIX + "&aНакормил {player}";

        // ══════════════════════════════════════════════════════════════
        // ОПЫТ
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.XP_GIVEN = MessagesConfig.PREFIX + "&a+{amount} уровней";

        // ══════════════════════════════════════════════════════════════
        // ОЧИСТКА ЧАТА
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.CHAT_CLEARED = MessagesConfig.PREFIX + "&aЧат очищен";

        // ══════════════════════════════════════════════════════════════
        // СТАТИСТИКА
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.STATS_HEADER = "&6=== Статистика ===";
        MessagesConfig.STATS_HEALTH = "&7HP: &c{health}/{max}";
        MessagesConfig.STATS_FOOD = "&7Еда: &e{food}";
        MessagesConfig.STATS_LEVEL = "&7Уровень: &a{level}";
        MessagesConfig.STATS_WORLD = "&7Мир: &b{world}";
        MessagesConfig.STATS_POSITION = "&7Позиция: &f{x} {y} {z}";
        MessagesConfig.STATS_GAMEMODE = "&7Режим: &d{gamemode}";
        MessagesConfig.STATS_FOOTER = "&6=========";

        // ══════════════════════════════════════════════════════════════
        // ОШИБКИ
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.ERROR_COMMAND = MessagesConfig.PREFIX + "&cОшибка: {error}";
        MessagesConfig.ERROR_ITEM = MessagesConfig.PREFIX + "&cОшибка предмета: {error}";
    }
}
