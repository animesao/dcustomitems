import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import me.dcplugin.dcustomitems.models.CustomItem;
import me.dcplugin.dcustomitems.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Команда /give — выдача кастомных предметов игрокам.
 *
 * Использование:
 *   /give <item_id>             — выдать себе
 *   /give <item_id> <player>    — выдать игроку
 *   /give <item_id> <player> <amount> — выдать N штук
 *   /give <item_id> all         — выдать всем онлайн
 *   /give list                  — список всех предметов
 */
public class GiveCommand extends CustomCommand {

    public GiveCommand() {
        super("give", "Выдать кастомный предмет", "/give <item_id> [player] [amount]", "dcustomitems.give");
        setAliases("g");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Main plugin = Main.getInstance();
        if (plugin == null) return true;

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        // /give list
        if (args[0].equalsIgnoreCase("list")) {
            return handleList(sender);
        }

        String itemId = args[0].toLowerCase();

        // Проверяем существование предмета
        CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
        if (customItem == null) {
            msg(sender, "&cПредмет '&e" + itemId + "&c' не найден! Используй &e/give list");
            return true;
        }

        // Определяем получателя
        Player target;
        int amount = 1;

        if (args.length >= 2) {
            if (args[1].equalsIgnoreCase("all")) {
                return handleGiveAll(sender, customItem, args.length >= 3 ? parseAmount(args[2], 1) : 1);
            }
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                msg(sender, "&cИгрок '&e" + args[1] + "&c' не найден!");
                return true;
            }
            if (args.length >= 3) {
                amount = parseAmount(args[2], 1);
            }
        } else {
            if (!(sender instanceof Player)) {
                msg(sender, "&cУкажите игрока: /give <id> <player>");
                return true;
            }
            target = (Player) sender;
        }

        // Ограничиваем количество
        amount = Math.max(1, Math.min(amount, 64));

        // Выдаём предмет
        giveItems(target, customItem, amount);

        // Сообщение
        String targetName = target.equals(sender) ? "себе" : target.getName();
        msg(sender, "&aВыдано &e" + customItem.getId() + " &ax" + amount + " &7→ &f" + targetName);

        if (!target.equals(sender)) {
            msg(target, "&aВы получили &e" + customItem.getId() + " &ax" + amount);
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, int position) {
        Main plugin = Main.getInstance();
        if (plugin == null) return new ArrayList<>();

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();

            if ("list".startsWith(prefix)) {
                completions.add("list");
            }

            for (String id : plugin.getItemHandler().getAllCustomItems().keySet()) {
                if (id.toLowerCase().startsWith(prefix)) {
                    completions.add(id);
                }
            }

            return completions;
        }

        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            List<String> completions = new ArrayList<>();

            if ("all".startsWith(prefix)) {
                completions.add("all");
            }
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(prefix)) {
                    completions.add(online.getName());
                }
            }

            return completions;
        }

        if (args.length == 3) {
            return Arrays.asList("1", "5", "16", "32", "64");
        }

        return new ArrayList<>();
    }

    // ===== Internal =====

    private boolean handleList(CommandSender sender) {
        Main plugin = Main.getInstance();
        var allItems = plugin.getItemHandler().getAllCustomItems();

        if (allItems.isEmpty()) {
            msg(sender, "&7Нет загруженных кастомных предметов.");
            return true;
        }

        msg(sender, "&6=== Кастомные предметы (" + allItems.size() + ") ===");

        for (var entry : allItems.entrySet()) {
            CustomItem item = entry.getValue();
            String type = item.getType() != null ? item.getType() : "UNKNOWN";
            String price = "";
            if (item.isBuyable()) {
                price = " &7[&e$" + String.format("%.0f", item.getBuyPrice()) + "&7]";
            }
            msg(sender, " &e" + entry.getKey() + " &7(" + type + ")" + price);
        }

        return true;
    }

    private boolean handleGiveAll(CommandSender sender, CustomItem item, int amount) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        int count = 0;

        for (Player target : online) {
            giveItems(target, item, amount);
            msg(target, "&aВы получили &e" + item.getId() + " &ax" + amount);
            count++;
        }

        msg(sender, "&aВыдано &e" + item.getId() + " &ax" + amount + " &7→ &fвсем (" + count + " игроков)");

        return true;
    }

    private void giveItems(Player player, CustomItem item, int amount) {
        ItemStack itemStack = item.getItemStack().clone();
        itemStack.setAmount(amount);
        player.getInventory().addItem(itemStack);
    }

    private int parseAmount(String input, int defaultValue) {
        try {
            return Math.max(1, Math.min(64, Integer.parseInt(input)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void sendHelp(CommandSender sender) {
        msg(sender, "&6=== /give ===");
        msg(sender, "&e/give <id> &7— выдать себе");
        msg(sender, "&e/give <id> <player> &7— выдать игроку");
        msg(sender, "&e/give <id> <player> <amount> &7— выдать N штук");
        msg(sender, "&e/give <id> all &7— выдать всем онлайн");
        msg(sender, "&e/give list &7— список предметов");
    }
}
