
package me.dcplugin.dcustomitems.commands;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.models.CustomItem;
import me.dcplugin.dcustomitems.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomItemsCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public CustomItemsCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give":
                return handleGiveCommand(sender, args);
            case "list":
                return handleListCommand(sender);
            case "reload":
                return handleReloadCommand(sender);
            case "update":
                return handleUpdateCommand(sender);
            default:
                sendHelpMessage(sender);
                return true;
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(plugin.getMessageManager().getMessage("help.header", "&6=== DC-CustomItems Помощь ==="));
        sender.sendMessage(plugin.getMessageManager().getMessage("help.give", "&e/customitems give <предмет> [игрок] &7- Выдать предмет"));
        sender.sendMessage(plugin.getMessageManager().getMessage("help.list", "&e/customitems list &7- Список предметов"));
        sender.sendMessage(plugin.getMessageManager().getMessage("help.reload", "&e/customitems reload &7- Перезагрузить плагин"));
    }

    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("customitems.give")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission", "&cУ вас нет прав на использование этой команды!"));
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.give-usage", "&cИспользование: /customitems give <предмет> [игрок]"));
            return false;
        }

        String itemId = args[1];
        CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);

        if (customItem == null) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.item-not-found", "&cПредмет с ID '{item}' не найден!").replace("{item}", itemId));
            return false;
        }

        // Проверка права на конкретный предмет
        if (customItem.hasPermission() && !sender.hasPermission(customItem.getPermission())) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.no-item-permission", "&cУ вас нет прав на использование этого предмета!"));
            return false;
        }

        Player target;
        if (args.length >= 3) {
            target = plugin.getServer().getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(plugin.getMessageManager().getMessage("commands.player-not-found", "&cИгрок не найден!"));
                return false;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getMessageManager().getMessage("commands.specify-player", "&cУкажите игрока!"));
                return false;
            }
            target = (Player) sender;
        }

        ItemStack itemStack = customItem.getItemStack().clone();
        itemStack = plugin.getItemHandler().updateItemWithUses(itemStack);
        target.getInventory().addItem(itemStack);

        sender.sendMessage(plugin.getMessageManager().getMessage("commands.item-given", "&aПредмет '{item}' выдан игроку {player}").replace("{item}", itemId).replace("{player}", target.getName()));
        if (!sender.equals(target)) {
            target.sendMessage(plugin.getMessageManager().getMessage("commands.item-received", "&aВы получили кастомный предмет: {item}").replace("{item}", itemId));
        }

        return true;
    }

    private boolean handleListCommand(CommandSender sender) {
        if (!sender.hasPermission("customitems.use")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission", "&cУ вас нет прав на использование этой команды!"));
            return false;
        }

        sender.sendMessage(plugin.getMessageManager().getMessage("commands.list-header", "&6=== Список кастомных предметов ==="));
        
        if (plugin.getItemHandler().getAllCustomItems().isEmpty()) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.list-empty", "&cНет загруженных предметов"));
            return true;
        }

        for (String itemId : plugin.getItemHandler().getAllCustomItems().keySet()) {
            CustomItem item = plugin.getItemHandler().getCustomItem(itemId);
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.list-item", "&e{item} &7- &f{type} &7({slot})")
                .replace("{item}", itemId)
                .replace("{type}", item.getType())
                .replace("{slot}", item.getActivationSlot()));
        }

        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("customitems.reload")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission", "&cУ вас нет прав на использование этой команды!"));
            return false;
        }

        try {
            plugin.getConfigManager().reloadConfig();
            plugin.getItemHandler().reloadItems();
            plugin.getArmorSetManager().reload();
            sender.sendMessage(plugin.getMessageManager().getMessage("plugin.reloaded", "&aПлагин успешно перезагружен!"));
        } catch (Exception e) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.reload-error", "&cОшибка при перезагрузке: {error}").replace("{error}", e.getMessage()));
            plugin.getLogger().severe("Ошибка при перезагрузке: " + e.getMessage());
        }

        return true;
    }

    private boolean handleUpdateCommand(CommandSender sender) {
        if (!sender.hasPermission("customitems.update")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission", "&cУ вас нет прав на использование этой команды!"));
            return false;
        }

        sender.sendMessage(plugin.getMessageManager().getMessage("commands.update-checking", "&eПроверка обновлений..."));
        plugin.getUpdateChecker().checkForUpdates(message -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ColorUtils.colorize(message));
            });
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("give", "list", "reload", "update");
            for (String subcommand : subcommands) {
                if (subcommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subcommand);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (String itemId : plugin.getItemHandler().getAllCustomItems().keySet()) {
                if (itemId.toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(itemId);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                String name = player.getName();
                if (name.toLowerCase().startsWith(args[2].toLowerCase())) {
                    completions.add(name);
                }
            }
        }

        return completions;
    }
}
