import me.dcplugin.dcustomitems.api.placeholders.CustomPlaceholder;
import org.bukkit.entity.Player;

/**
 * Плейсхолдер %kills% - количество убийств
 * 
 * Положи этот файл в plugins/DC-CustomItems/items/
 * Сделай /ci reload
 * Используй %kills% в любом тексте!
 * 
 * Примеры использования:
 * - "Ваши убийства: %kills%"
 * - В title: "Убийства: %kills%"
 * - ВActionBar: "Убийств: %kills%"
 */
public class KillsPlaceholder extends CustomPlaceholder {

    // Временное хранилище (в реальном плагине - база данных)
    private static final java.util.Map<java.util.UUID, Integer> kills = new java.util.HashMap<>();

    public KillsPlaceholder() {
        super("kills"); // %kills%
    }

    @Override
    public String getValue(Player player) {
        if (player == null) return "0";
        return String.valueOf(kills.getOrDefault(player.getUniqueId(), 0));
    }

    /**
     * Добавить убийство (вызывать при убийстве моба/игрока)
     */
    public static void addKill(Player player) {
        kills.merge(player.getUniqueId(), 1, Integer::sum);
    }

    /**
     * Получить количество убийств
     */
    public static int getKills(Player player) {
        return kills.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * Сбросить убийства
     */
    public static void resetKills(Player player) {
        kills.remove(player.getUniqueId());
    }
}
