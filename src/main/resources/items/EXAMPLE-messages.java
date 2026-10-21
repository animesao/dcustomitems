import me.dcplugin.dcustomitems.api.config.MessagesConfig;

/**
 * 🔧 CONFIGURATION OF ALL PLUGIN MESSAGES (SAMPLE)
 * 
 * A file with the EXAMPLE- prefix is copied into items/ on first launch,
 * but is not loaded. To apply your own messages — rename it
 * to messages.java (or overwrite the existing items/messages.java).
 * After /ci reload all messages will be updated!
 * 
 * Format:
 * - &0-9, &a-f - colors
 * - &l - bold
 * - &n - underlined
 * - &o - italic
 * - &r - reset
 * 
 * Placeholders:
 * - {player} - the player's name
 * - {item} - the item name
 * - {error} - the error text
 * - {count} - the count
 * - {version} - the version
 * - {amount} - the amount
 * - {health} - health
 * - {max} - maximum
 * - {food} - food
 * - {level} - level
 * - {world} - world
 * - {x}, {y}, {z} - coordinates
 * - {gamemode} - game mode
 * - {seconds} - seconds
 * - {uses} - uses
 * - {bonus} - bonus
 */
public class EXAMPLEmessages {

    public static void load() {
        // ══════════════════════════════════════════════════════════════
        // PREFIX
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.PREFIX = "&8[&6MyPlugin&8] &r";

        // ══════════════════════════════════════════════════════════════
        // GENERAL
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.NO_PERMISSION = MessagesConfig.PREFIX + "&cNo permission!";
        MessagesConfig.PLAYER_NOT_FOUND = MessagesConfig.PREFIX + "&cPlayer not found!";
        MessagesConfig.ITEM_NOT_FOUND = MessagesConfig.PREFIX + "&cItem not found!";

        // ══════════════════════════════════════════════════════════════
        // /ci COMMANDS
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.CI_HEADER = "&6&l=== My Items ===";
        MessagesConfig.CI_HELP_GIVE = "&e/ci give <id> [player] &7- Give an item";
        MessagesConfig.CI_HELP_LIST = "&e/ci list &7- List";
        MessagesConfig.CI_HELP_RELOAD = "&e/ci reload &7- Reload";

        MessagesConfig.CI_GIVE_SELF = MessagesConfig.PREFIX + "&aGiven: &e{item}";
        MessagesConfig.CI_GIVE_OTHER = MessagesConfig.PREFIX + "&aGave &e{item} &ato the player &e{player}";
        MessagesConfig.CI_GIVE_RECEIVED = MessagesConfig.PREFIX + "&aYou received: &e{item}";

        // ══════════════════════════════════════════════════════════════
        // LIST
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.LIST_HEADER = MessagesConfig.PREFIX + "&6=== List ===";
        MessagesConfig.LIST_ITEM = MessagesConfig.PREFIX + "&e{item} &7- &f{type}";
        MessagesConfig.LIST_EMPTY = MessagesConfig.PREFIX + "&cEmpty";

        // ══════════════════════════════════════════════════════════════
        // RELOAD
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.RELOAD_SUCCESS = MessagesConfig.PREFIX + "&a&lDone!";
        MessagesConfig.RELOAD_YAML = MessagesConfig.PREFIX + "&7YAML: &e{count}";
        MessagesConfig.RELOAD_JAVA_ITEMS = MessagesConfig.PREFIX + "&7Items: &e{count}";
        MessagesConfig.RELOAD_JAVA_COMMANDS = MessagesConfig.PREFIX + "&7Commands: &e{count}";
        MessagesConfig.RELOAD_JAVA_PLACEHOLDERS = MessagesConfig.PREFIX + "&7Placeholders: &e{count}";

        // ══════════════════════════════════════════════════════════════
        // UPDATES
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.UPDATE_LATEST = MessagesConfig.PREFIX + "&aLatest version! ({version})";
        MessagesConfig.UPDATE_AVAILABLE = MessagesConfig.PREFIX + "&cNew version: {version}";

        // ══════════════════════════════════════════════════════════════
        // /api-item
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.API_HEADER = MessagesConfig.PREFIX + "&6=== API ===";
        MessagesConfig.API_GIVE_SELF = MessagesConfig.PREFIX + "&aGiven: &e{item}";
        MessagesConfig.API_INFO_HEADER = MessagesConfig.PREFIX + "&6=== Info ===";

        // ══════════════════════════════════════════════════════════════
        // COOLDOWNS
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.COOLDOWN = MessagesConfig.PREFIX + "&cWait {seconds} sec!";

        // ══════════════════════════════════════════════════════════════
        // ACTIONS
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.ACTION_HEAL = "&a❤ +{amount} HP";
        MessagesConfig.ACTION_TELEPORT = "&d✨ Teleport!";
        MessagesConfig.ACTION_GIVE = "&a+{amount} {material}";
        MessagesConfig.ACTION_REMOVE = "&c-{amount} {material}";

        // ══════════════════════════════════════════════════════════════
        // BERSERK
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.BERSERK_ENABLED = MessagesConfig.PREFIX + "&4🔥 BERSERK!";
        MessagesConfig.BERSERK_DISABLED = MessagesConfig.PREFIX + "&7Off.";
        MessagesConfig.BERSERK_ALREADY = MessagesConfig.PREFIX + "&cAlready active!";
        MessagesConfig.BERSERK_NOT_ACTIVE = MessagesConfig.PREFIX + "&cNot active!";

        // ══════════════════════════════════════════════════════════════
        // HEALING
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.HEAL_SELF = MessagesConfig.PREFIX + "&a❤ Healed!";
        MessagesConfig.HEAL_OTHER = MessagesConfig.PREFIX + "&aHealed {player}";
        MessagesConfig.HEAL_AMOUNT = MessagesConfig.PREFIX + "&a+{amount} HP";

        // ══════════════════════════════════════════════════════════════
        // TELEPORT
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.TELEPORT_SELF = MessagesConfig.PREFIX + "&bTeleport to {player}";

        // ══════════════════════════════════════════════════════════════
        // FLIGHT
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.FLY_ON = MessagesConfig.PREFIX + "&aFlight on";
        MessagesConfig.FLY_OFF = MessagesConfig.PREFIX + "&cFlight off";

        // ══════════════════════════════════════════════════════════════
        // GAMEMODE
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.GAMEMODE_SURVIVAL = MessagesConfig.PREFIX + "&aSurvival";
        MessagesConfig.GAMEMODE_CREATIVE = MessagesConfig.PREFIX + "&aCreative";
        MessagesConfig.GAMEMODE_ADVENTURE = MessagesConfig.PREFIX + "&aAdventure";
        MessagesConfig.GAMEMODE_SPECTATOR = MessagesConfig.PREFIX + "&aSpectator";

        // ══════════════════════════════════════════════════════════════
        // GOD MODE
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.GOD_ON = MessagesConfig.PREFIX + "&6God mode for {seconds}s!";
        MessagesConfig.GOD_OFF = MessagesConfig.PREFIX + "&7God mode off.";

        // ══════════════════════════════════════════════════════════════
        // INVISIBILITY
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.INVISIBLE_ON = MessagesConfig.PREFIX + "&7Invisibility on";
        MessagesConfig.INVISIBLE_OFF = MessagesConfig.PREFIX + "&7Invisibility off";

        // ══════════════════════════════════════════════════════════════
        // SPEED
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.SPEED_ON = MessagesConfig.PREFIX + "&bSpeed on";
        MessagesConfig.SPEED_OFF = MessagesConfig.PREFIX + "&7Speed off";

        // ══════════════════════════════════════════════════════════════
        // FOOD
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.FEED_SELF = MessagesConfig.PREFIX + "&aYou have been fed!";
        MessagesConfig.FEED_OTHER = MessagesConfig.PREFIX + "&aFed {player}";

        // ══════════════════════════════════════════════════════════════
        // XP
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.XP_GIVEN = MessagesConfig.PREFIX + "&a+{amount} levels";

        // ══════════════════════════════════════════════════════════════
        // CHAT CLEAR
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.CHAT_CLEARED = MessagesConfig.PREFIX + "&aChat cleared";

        // ══════════════════════════════════════════════════════════════
        // STATISTICS
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.STATS_HEADER = "&6=== Statistics ===";
        MessagesConfig.STATS_HEALTH = "&7HP: &c{health}/{max}";
        MessagesConfig.STATS_FOOD = "&7Food: &e{food}";
        MessagesConfig.STATS_LEVEL = "&7Level: &a{level}";
        MessagesConfig.STATS_WORLD = "&7World: &b{world}";
        MessagesConfig.STATS_POSITION = "&7Position: &f{x} {y} {z}";
        MessagesConfig.STATS_GAMEMODE = "&7Mode: &d{gamemode}";
        MessagesConfig.STATS_FOOTER = "&6=========";

        // ══════════════════════════════════════════════════════════════
        // ERRORS
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.ERROR_COMMAND = MessagesConfig.PREFIX + "&cError: {error}";
        MessagesConfig.ERROR_ITEM = MessagesConfig.PREFIX + "&cItem error: {error}";
    }
}
