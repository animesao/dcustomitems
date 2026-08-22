import me.dcplugin.dcustomitems.api.placeholders.CustomPlaceholder;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Плейсхолдер %combo% - счётчик комбо убийств
 *
 * Форматы:
 *   %combo%            → "x5" (текущее комбо)
 *   %combo_multiplier% → "2.5x" (множитель урона)
 *   %combo_timer%      → "8" (секунды до сброса)
 *   %combo_max%        → "15" (максимальное комбо за сессию)
 *
 * Комбо сбрасывается через 10 секунд без убийства.
 * Каждое убийство увеличивает комбо на 1.
 * Множитель урона = 1 + (combo * 0.1), максимум 3.0x
 *
 * Положи этот файл в plugins/DC-CustomItems/items/
 * Сделай /ci reload
 *
 * Примеры:
 *   Scoreboard: "&7Комбо: &c%combo% &7| &e%combo_multiplier%"
 *   ActionBar: "&cКомбо x%combo%! Множитель: %combo_multiplier%"
 *
 * Для активации комбо-системы используй триггеры:
 *   trigger-actions:
 *     - 'on_kill:combo_add:1'
 *     - 'on_kill:message:&cКомбо x%combo%! &eМножитель: %combo_multiplier%'
 */
public class ComboPlaceholder extends CustomPlaceholder {

    private static final long COMBO_TIMEOUT_MS = 10_000; // 10 секунд

    // Текущее комбо
    private static final Map<UUID, Integer> comboCount = new HashMap<>();
    // Время последнего убийства
    private static final Map<UUID, Long> lastKillTime = new HashMap<>();
    // Максимальное комбо за сессию
    private static final Map<UUID, Integer> maxCombo = new HashMap<>();

    public ComboPlaceholder() {
        super("combo"); // %combo%
    }

    @Override
    public String getValue(Player player) {
        if (player == null) return "x0";
        return "x" + getCombo(player);
    }

    // ===== Основные методы =====

    /**
     * Добавить комбо (вызывать при убийстве)
     */
    public static void addCombo(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastKill = lastKillTime.get(uuid);

        if (lastKill != null && (now - lastKill) > COMBO_TIMEOUT_MS) {
            // Комбо истекло — начинаем заново
            comboCount.put(uuid, 1);
        } else {
            comboCount.merge(uuid, 1, Integer::sum);
        }

        lastKillTime.put(uuid, now);

        // Обновляем максимум
        int current = comboCount.getOrDefault(uuid, 0);
        int best = maxCombo.getOrDefault(uuid, 0);
        if (current > best) {
            maxCombo.put(uuid, current);
        }
    }

    /**
     * Сбросить комбо
     */
    public static void resetCombo(Player player) {
        comboCount.remove(player.getUniqueId());
        lastKillTime.remove(player.getUniqueId());
    }

    /**
     * Получить текущее комбо (с проверкой таймаута)
     */
    public static int getCombo(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastKill = lastKillTime.get(uuid);

        if (lastKill != null && (now - lastKill) > COMBO_TIMEOUT_MS) {
            comboCount.put(uuid, 0);
            return 0;
        }
        return comboCount.getOrDefault(uuid, 0);
    }

    /**
     * Получить множитель урона
     * Формула: 1 + (combo * 0.1), максимум 3.0
     */
    public static double getMultiplier(Player player) {
        int combo = getCombo(player);
        return Math.min(1.0 + (combo * 0.1), 3.0);
    }

    /**
     * Получить секунды до сброса комбо
     */
    public static int getTimerSeconds(Player player) {
        Long lastKill = lastKillTime.get(player.getUniqueId());
        if (lastKill == null) return 0;
        long elapsed = (System.currentTimeMillis() - lastKill) / 1000;
        long remaining = Math.max(0, (COMBO_TIMEOUT_MS / 1000) - elapsed);
        return (int) remaining;
    }

    /**
     * Получить максимальное комбо за сессию
     */
    public static int getMaxCombo(Player player) {
        return maxCombo.getOrDefault(player.getUniqueId(), 0);
    }
}
