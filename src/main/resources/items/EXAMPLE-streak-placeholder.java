import me.dcplugin.dcustomitems.api.placeholders.CustomPlaceholder;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Плейсхолдер %streak% - серия ежедневных входов
 *
 * Форматы:
 *   %streak%           → "5" (количество дней подряд)
 *   %streak_best%      → "12" (лучшая серия)
 *   %streak_status%    → "🔥 Активна" / "❌ Прервана"
 *
 * Положи этот файл в plugins/DC-CustomItems/items/
 * Сделай /ci reload
 *
 * Примеры:
 *   "Серия входов: %streak% дней подряд"
 *   "Лучшая серия: %streak_best%"
 *   Scoreboard: "&7Серия: &e%streak% &7дней &c%streak_status%"
 */
public class StreakPlaceholder extends CustomPlaceholder {

    // Текущая серия
    private static final Map<UUID, Integer> currentStreak = new HashMap<>();
    // Лучшая серия
    private static final Map<UUID, Integer> bestStreak = new HashMap<>();
    // Дата последнего входа
    private static final Map<UUID, LocalDate> lastJoinDate = new HashMap<>();

    public StreakPlaceholder() {
        super("streak"); // %streak%
    }

    @Override
    public String getValue(Player player) {
        if (player == null) return "0";
        return String.valueOf(currentStreak.getOrDefault(player.getUniqueId(), 0));
    }

    /**
     * Вызывать при входе игрока на сервер
     */
    public static void onJoin(Player player) {
        UUID uuid = player.getUniqueId();
        LocalDate today = LocalDate.now();
        LocalDate lastJoin = lastJoinDate.get(uuid);

        if (lastJoin == null) {
            // Первый вход — начинаем серию
            currentStreak.put(uuid, 1);
        } else {
            long daysBetween = ChronoUnit.DAYS.between(lastJoin, today);
            if (daysBetween == 1) {
                // Вход на следующий день — продолжаем серию
                currentStreak.merge(uuid, 1, Integer::sum);
            } else if (daysBetween > 1) {
                // Пропустил день — сбрасываем серию
                currentStreak.put(uuid, 1);
            }
            // daysBetween == 0 — уже входил сегодня, не меняем
        }

        lastJoinDate.put(uuid, today);

        // Обновляем лучшую серию
        int current = currentStreak.getOrDefault(uuid, 0);
        int best = bestStreak.getOrDefault(uuid, 0);
        if (current > best) {
            bestStreak.put(uuid, current);
        }
    }

    public static int getCurrentStreak(Player player) {
        return currentStreak.getOrDefault(player.getUniqueId(), 0);
    }

    public static int getBestStreak(Player player) {
        return bestStreak.getOrDefault(player.getUniqueId(), 0);
    }

    public static String getStatus(Player player) {
        int streak = getCurrentStreak(player);
        if (streak >= 7) return "🔥 Огненная серия!";
        if (streak >= 3) return "🔥 Активна";
        if (streak >= 1) return "✅ Начата";
        return "❌ Прервана";
    }
}
