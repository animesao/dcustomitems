package me.dcplugin.dcustomitems.api;

import me.dcplugin.dcustomitems.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Команда /api-item для работы с Java API предметами.
 *
 * Использование:
 *   /api-item give <id> [игрок]  - выдать предмет
 *   /api-item list               - список всех предметов
 *   /api-item info <id>          - информация о предмете
 */
public class ApiCommand implements CommandExecutor, TabCompleter {

    private final ItemRegistry registry;

    public ApiCommand(ItemRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "give":
                return handleGive(sender, args);
            case "list":
                return handleList(sender);
            case "info":
                return handleInfo(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&cИспользование: /api-item give <id> [игрок]"));
            return true;
        }

        String itemId = args[1];
        AbstractCustomItem item = registry.getItem(itemId);
        if (item == null) {
            sender.sendMessage(ColorUtils.colorize("&cПредмет не найден: " + itemId));
            return true;
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(ColorUtils.colorize("&cИгрок не найден: " + args[2]));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(ColorUtils.colorize("&cУкажите игрока: /api-item give <id> <игрок>"));
            return true;
        }

        ItemStack stack = item.createItemStack();
        target.getInventory().addItem(stack);
        target.sendMessage(ColorUtils.colorize("&aВыдан предмет: " + item.getDisplayName()));

        if (!sender.equals(target)) {
            sender.sendMessage(ColorUtils.colorize("&aВыдан " + item.getDisplayName() + " игроку " + target.getName()));
        }

        return true;
    }

    private boolean handleList(CommandSender sender) {
        var allItems = registry.getAllItems();
        if (allItems.isEmpty()) {
            sender.sendMessage(ColorUtils.colorize("&eНет загруженных Java API предметов"));
            return true;
        }

        sender.sendMessage(ColorUtils.colorize("&6=== Java API Предметы (" + allItems.size() + ") ==="));
        for (var entry : allItems.entrySet()) {
            AbstractCustomItem item = entry.getValue();
            sender.sendMessage(ColorUtils.colorize(
                " &e" + item.getId() + " &7- " + item.getDisplayName() + " &7[" + item.getType() + "]"
            ));
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&cИспользование: /api-item info <id>"));
            return true;
        }

        String itemId = args[1];
        AbstractCustomItem item = registry.getItem(itemId);
        if (item == null) {
            sender.sendMessage(ColorUtils.colorize("&cПредмет не найден: " + itemId));
            return true;
        }

        sender.sendMessage(ColorUtils.colorize("&6=== Информация о предмете ==="));
        sender.sendMessage(ColorUtils.colorize(" &eID: &f" + item.getId()));
        sender.sendMessage(ColorUtils.colorize(" &eНазвание: " + item.getDisplayName()));
        sender.sendMessage(ColorUtils.colorize(" &eМатериал: &f" + item.getMaterial().name()));
        sender.sendMessage(ColorUtils.colorize(" &eТип: &f" + item.getType()));
        sender.sendMessage(ColorUtils.colorize(" &eСлот: &f" + item.getActivationSlot()));
        sender.sendMessage(ColorUtils.colorize(" &eКулдаун: &f" + item.getClickCooldown() + "мс"));
        sender.sendMessage(ColorUtils.colorize(" &eНеломаемость: &f" + item.isUnbreakable()));
        sender.sendMessage(ColorUtils.colorize(" &eСвечение: &f" + item.isGlowing()));
        if (item.getItemModel() != null) {
            sender.sendMessage(ColorUtils.colorize(" &eМодель: &f" + item.getItemModel()));
        }
        if (item.getPermission() != null) {
            sender.sendMessage(ColorUtils.colorize(" &eПраво: &f" + item.getPermission()));
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtils.colorize("&6=== Java API Команды ==="));
        sender.sendMessage(ColorUtils.colorize(" &e/api-item give <id> [игрок] &7- выдать предмет"));
        sender.sendMessage(ColorUtils.colorize(" &e/api-item list &7- список предметов"));
        sender.sendMessage(ColorUtils.colorize(" &e/api-item info <id> &7- информация о предмете"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of("give", "list", "info"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("info")) {
                completions.addAll(registry.getAllIds());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
        }

        return completions;
    }
}
