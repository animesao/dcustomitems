import me.dcplugin.dcustomitems.api.placeholders.CustomPlaceholder;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Плейсхолдер %rank% - ранг игрока
 *
 * Форматы:
 *   %rank%             → "VIP" (название ранга)
 *   %rank_prefix%      → "&6[VIP] &r" (префикс для чата)
 *   %rank_color%       → "&6" (цвет ранга)
 *   %rank_level%       → "3" (числовой уровень ранга)
 *   %rank_next%        → "Premium" (следующий ранг)
 *   %rank_progress%    → "75%" (прогресс до следующего ранга)
 *
 * Ранги: Новичок(1) → Игрок(2) → VIP(3) → Premium(4) → Legendary(5)
 *
 * Положи этот файл в plugins/DC-CustomItems/items/
 * Сделай /ci reload
 *
 * Примеры:
 *   Chat: "%rank_prefix%%player%&7: %message%"
 *   Scoreboard: "&7Ранг: %rank_color%%rank%"
 *   Tablist: "%rank_prefix%%player_name%"
 *
 * Установить ранг:
 *   /api-item setrank <player> <level>
 */
public class StatusPlaceholder extends CustomPlaceholder {

    private static final String[] RANK_NAMES = {
        "Новичок", "Игрок", "VIP", "Premium", "Legendary"
    };
    private static final String[] RANK_COLORS = {
        "&7", "&a", "&6", "&b", "&d"
    };
    private static final String[] RANK_PREFIXES = {
        "&7[Новичок] &r",
        "&a[Игрок] &r",
        "&6[VIP] &r",
        "&b[Premium] &r",
        "&d[Legendary] &r"
    };

    private static final Map<UUID, Integer> playerRanks = new HashMap<>();

    public StatusPlaceholder() {
        super("rank"); // %rank%
    }

    @Override
    public String getValue(Player player) {
        if (player == null) return "Новичок";
        return getRankName(player);
    }

    // ===== Основные методы =====

    public static int getRankLevel(Player player) {
        return playerRanks.getOrDefault(player.getUniqueId(), 1);
    }

    public static void setRank(Player player, int level) {
        level = Math.max(1, Math.min(level, RANK_NAMES.length));
        playerRanks.put(player.getUniqueId(), level);
    }

    public static void addRank(Player player) {
        int current = getRankLevel(player);
        if (current < RANK_NAMES.length) {
            setRank(player, current + 1);
        }
    }

    public static String getRankName(Player player) {
        int level = getRankLevel(player);
        return RANK_NAMES[Math.min(level - 1, RANK_NAMES.length - 1)];
    }

    public static String getRankColor(Player player) {
        int level = getRankLevel(player);
        return RANK_COLORS[Math.min(level - 1, RANK_COLORS.length - 1)];
    }

    public static String getRankPrefix(Player player) {
        int level = getRankLevel(player);
        return RANK_PREFIXES[Math.min(level - 1, RANK_PREFIXES.length - 1)];
    }

    public static String getNextRank(Player player) {
        int level = getRankLevel(player);
        if (level >= RANK_NAMES.length) return "Максимум";
        return RANK_NAMES[level]; // level-1 + 1 = level
    }

    /**
     * Прогресс до следующего ранга (на основе времени/активности)
     * Здесь просто пример — в реальном плагине берите из БД
     */
    public static int getProgress(Player player) {
        int level = getRankLevel(player);
        if (level >= RANK_NAMES.length) return 100;
        // Пример: каждый ранг требует больше времени
        // В реальном плагине — запрос к БД
        return Math.min(100, level * 25);
    }
}
