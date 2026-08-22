import me.dcplugin.dcustomitems.api.placeholders.CustomPlaceholder;
import org.bukkit.entity.Player;

/**
 * Плейсхолдер %playtime% - время игры игрока
 * 
 * Форматы:
 *   %playtime%          → "2ч 15м 30с"
 *   %playtime_hours%    → "2"
 *   %playtime_minutes%  → "135"
 *   %playtime_seconds%  → "8130"
 *
 * Положи этот файл в plugins/DC-CustomItems/items/
 * Сделай /ci reload
 * Используй в любом тексте!
 *
 * Примеры:
 *   "Время в игре: %playtime%"
 *   "Часов на сервере: %playtime_hours%"
 *   Scoreboard: "&7Время: &e%playtime%"
 */
public class PlaytimePlaceholder extends CustomPlaceholder {

    // Хранилище времени входа (timestamp)
    private static final java.util.Map<java.util.UUID, Long> joinTimes = new java.util.HashMap<>();
    // Накопленное время (в секундах) до текущей сессии
    private static final java.util.Map<java.util.UUID, Long> totalSeconds = new java.util.HashMap<>();

    public PlaytimePlaceholder() {
        super("playtime"); // %playtime%
    }

    @Override
    public String getValue(Player player) {
        if (player == null) return "0с";
        long seconds = getTotalSeconds(player);
        return formatTime(seconds);
    }

    /**
     * Вызывать при входе игрока на сервер
     */
    public static void onJoin(Player player) {
        joinTimes.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Вызывать при выходе игрока с сервера
     */
    public static void onQuit(Player player) {
        long joinTime = joinTimes.remove(player.getUniqueId());
        if (joinTime > 0) {
            long sessionSeconds = (System.currentTimeMillis() - joinTime) / 1000;
            totalSeconds.merge(player.getUniqueId(), sessionSeconds, Long::sum);
        }
    }

    /**
     * Получить общее время в секундах
     */
    public static long getTotalSeconds(Player player) {
        long total = totalSeconds.getOrDefault(player.getUniqueId(), 0L);
        Long joinTime = joinTimes.get(player.getUniqueId());
        if (joinTime != null) {
            total += (System.currentTimeMillis() - joinTime) / 1000;
        }
        return total;
    }

    /**
     * Форматировать секунды в читаемый вид
     */
    private String formatTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return hours + "ч " + minutes + "м " + seconds + "с";
        } else if (minutes > 0) {
            return minutes + "м " + seconds + "с";
        } else {
            return seconds + "с";
        }
    }
}
