import me.dcplugin.dcustomitems.api.placeholders.CustomPlaceholder;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Плейсхолдер %balance% - баланс виртуальной валюты
 *
 * Форматы:
 *   %balance%          → "1,250" (с разделителем тысяч)
 *   %balance_raw%      → "1250" (без разделителя)
 *   %balance_short%    → "1.2K" (сокращённый формат)
 *   %balance_formatted%→ "$1,250" (с символом валюты)
 *
 * Положи этот файл в plugins/DC-CustomItems/items/
 * Сделай /ci reload
 *
 * Примеры:
 *   "Ваш баланс: %balance% монет"
 *   Tablist: "&6$%balance%"
 *   Scoreboard: "&7Баланс: &e%balance_formatted%"
 *
 * Для создания монет используй команду:
 *   /api-item give balance-item <player>
 */
public class BalancePlaceholder extends CustomPlaceholder {

    private static final Map<UUID, Double> balances = new HashMap<>();

    public BalancePlaceholder() {
        super("balance"); // %balance%
    }

    @Override
    public String getValue(Player player) {
        if (player == null) return "0";
        double balance = getBalance(player);
        return String.format("%,.0f", balance);
    }

    // ===== Основные методы =====

    public static double getBalance(Player player) {
        return balances.getOrDefault(player.getUniqueId(), 0.0);
    }

    public static void setBalance(Player player, double amount) {
        balances.put(player.getUniqueId(), Math.max(0, amount));
    }

    public static void addBalance(Player player, double amount) {
        balances.merge(player.getUniqueId(), amount, Double::sum);
    }

    public static boolean removeBalance(Player player, double amount) {
        double current = getBalance(player);
        if (current < amount) return false;
        balances.put(player.getUniqueId(), current - amount);
        return true;
    }

    public static boolean hasEnough(Player player, double amount) {
        return getBalance(player) >= amount;
    }

    // ===== Форматирование =====

    /** "1,250" */
    public static String format(double amount) {
        return String.format("%,.0f", amount);
    }

    /** "$1,250" */
    public static String formatWithSymbol(double amount) {
        return "$" + format(amount);
    }

    /** "1.2K" / "3.5M" / "1.2B" */
    public static String formatShort(double amount) {
        if (amount >= 1_000_000_000) {
            return String.format("%.1fB", amount / 1_000_000_000);
        } else if (amount >= 1_000_000) {
            return String.format("%.1fM", amount / 1_000_000);
        } else if (amount >= 1_000) {
            return String.format("%.1fK", amount / 1_000);
        } else {
            return String.format("%.0f", amount);
        }
    }
}
