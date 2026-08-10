import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.database.DatabaseManager;
import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Пример промокодов с хранением в SQLite или MySQL.
 *
 * Файл можно скопировать в plugins/DC-CustomItems/items/ как promo-code.java.
 * После этого выполните /ci reload и используйте /promo DEMO2026.
 *
 * В config.yml:
 * database.type: sqlite  -> локальный data.db
 * database.type: mysql   -> MySQL из database.mysql.*
 */
public class PromoCodeCommand extends CustomCommand {

    public PromoCodeCommand() {
        super("promo", "Активировать промокод", "/promo <код>", "dci.command.promo");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            msg(sender, "&cЭта команда только для игроков.");
            return true;
        }
        if (args.length == 0) {
            msg(sender, "&eИспользование: /promo <код>");
            return true;
        }

        Player player = (Player) sender;
        String code = args[0].trim().toUpperCase(Locale.ROOT);
        DatabaseManager database = Main.getInstance().getDatabaseManager();

        // Таблица создаётся одинаково в SQLite и MySQL.
        database.executeAsync(
            "CREATE TABLE IF NOT EXISTS promo_codes (" +
                "code VARCHAR(64) PRIMARY KEY, " +
                "reward_command VARCHAR(255) NOT NULL, " +
                "max_uses INT NOT NULL DEFAULT 1, " +
                "uses INT NOT NULL DEFAULT 0)"
        ).thenCompose(created -> database.queryAllAsync(
            "SELECT reward_command, max_uses, uses FROM promo_codes WHERE code = ?",
            code
        )).thenAccept(rows -> {
            if (rows.isEmpty()) {
                sendOnMain(player, "&cТакого промокода нет.");
                return;
            }

            var row = rows.get(0);
            int maxUses = ((Number) row.get("max_uses")).intValue();
            int uses = ((Number) row.get("uses")).intValue();
            if (uses >= maxUses) {
                sendOnMain(player, "&cЭтот промокод уже закончился.");
                return;
            }

            int updated = database.executeUpdate(
                "UPDATE promo_codes SET uses = uses + 1 WHERE code = ? AND uses < max_uses",
                code
            );
            if (updated != 1) {
                sendOnMain(player, "&cНе удалось активировать промокод. Попробуйте позже.");
                return;
            }

            String reward = String.valueOf(row.get("reward_command"))
                .replace("%player%", player.getName());
            Main.getInstance().getServer().getScheduler().runTask(Main.getInstance(), () -> {
                Main.getInstance().getServer().dispatchCommand(
                    Main.getInstance().getServer().getConsoleSender(), reward
                );
                player.sendMessage(colorize("&aПромокод активирован!"));
            });
        }).exceptionally(error -> {
            sendOnMain(player, "&cОшибка базы данных. Проверьте консоль.");
            Main.getInstance().getLogger().warning("Promo code error: " + error.getMessage());
            return null;
        });
        return true;
    }

    private void sendOnMain(Player player, String message) {
        Main.getInstance().getServer().getScheduler().runTask(
            Main.getInstance(), () -> msg(player, message)
        );
    }
}
