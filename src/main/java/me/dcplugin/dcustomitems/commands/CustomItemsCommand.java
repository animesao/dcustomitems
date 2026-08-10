package me.dcplugin.dcustomitems.commands;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.config.MessagesConfig;
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
        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.CI_HEADER));
        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.CI_HELP_GIVE));
        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.CI_HELP_LIST));
        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.CI_HELP_RELOAD));
        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.CI_HELP_UPDATE));
    }

    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("customitems.give")) {
            sender.sendMessage(MessagesConfig.colorize(MessagesConfig.NO_PERMISSION));
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage(MessagesConfig.colorize(MessagesConfig.CI_GIVE_USAGE));
            return false;
        }

        String itemId = args[1];
        CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);

        if (customItem == null) {
            sender.sendMessage(MessagesConfig.format(MessagesConfig.ITEM_NOT_FOUND, "{item}", itemId));
            return false;
        }

        if (customItem.hasPermission() && !sender.hasPermission(customItem.getPermission())) {
            sender.sendMessage(MessagesConfig.colorize(MessagesConfig.CI_GIVE_NO_PERM));
            return false;
        }

        Player target;
        if (args.length >= 3) {
            target = plugin.getServer().getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(MessagesConfig.colorize(MessagesConfig.PLAYER_NOT_FOUND));
                return false;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(MessagesConfig.colorize(MessagesConfig.CI_GIVE_USAGE));
                return false;
            }
            target = (Player) sender;
        }

        ItemStack itemStack = customItem.getItemStack().clone();
        itemStack = plugin.getItemHandler().updateItemWithUses(itemStack);
        target.getInventory().addItem(itemStack);

        if (sender.equals(target)) {
            sender.sendMessage(MessagesConfig.format(MessagesConfig.CI_GIVE_SELF, "{item}", itemId));
        } else {
            sender.sendMessage(MessagesConfig.format(MessagesConfig.CI_GIVE_OTHER, "{item}", itemId, "{player}", target.getName()));
            target.sendMessage(MessagesConfig.format(MessagesConfig.CI_GIVE_RECEIVED, "{item}", itemId));
        }

        return true;
    }

    private boolean handleListCommand(CommandSender sender) {
        if (!sender.hasPermission("customitems.list")) {
            sender.sendMessage(MessagesConfig.colorize(MessagesConfig.NO_PERMISSION));
            return false;
        }

        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.LIST_HEADER));

        if (plugin.getItemHandler().getAllCustomItems().isEmpty()) {
            sender.sendMessage(MessagesConfig.colorize(MessagesConfig.LIST_EMPTY));
            return true;
        }

        for (String itemId : plugin.getItemHandler().getAllCustomItems().keySet()) {
            CustomItem item = plugin.getItemHandler().getCustomItem(itemId);
            sender.sendMessage(MessagesConfig.format(MessagesConfig.LIST_ITEM,
                "{item}", itemId,
                "{type}", item.getType(),
                "{slot}", item.getActivationSlot()));
        }

        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("customitems.reload")) {
            sender.sendMessage(MessagesConfig.colorize(MessagesConfig.NO_PERMISSION));
            return false;
        }

        try {
            plugin.getConfigManager().reloadConfig();
            plugin.getItemHandler().reloadItems();
            plugin.getArmorSetManager().reload();

            if (plugin.getApiItemRegistry() != null) {
                plugin.getApiItemRegistry().reload();
                int items = plugin.getApiItemRegistry().getItemCount();
                int cmds = plugin.getApiItemRegistry().getCommandCount();
                int phs = plugin.getApiItemRegistry().getPlaceholderCount();

                // Re-register custom commands with server
                plugin.registerCustomCommands();

                sender.sendMessage(MessagesConfig.colorize(MessagesConfig.RELOAD_SUCCESS));
                sender.sendMessage(MessagesConfig.format(MessagesConfig.RELOAD_YAML, "{count}", String.valueOf(plugin.getItemHandler().getAllCustomItems().size())));
                sender.sendMessage(MessagesConfig.format(MessagesConfig.RELOAD_JAVA_ITEMS, "{count}", String.valueOf(items)));
                sender.sendMessage(MessagesConfig.format(MessagesConfig.RELOAD_JAVA_COMMANDS, "{count}", String.valueOf(cmds)));
                sender.sendMessage(MessagesConfig.format(MessagesConfig.RELOAD_JAVA_PLACEHOLDERS, "{count}", String.valueOf(phs)));
            } else {
                sender.sendMessage(MessagesConfig.colorize(MessagesConfig.RELOAD_SUCCESS));
            }
        } catch (Exception e) {
            sender.sendMessage(MessagesConfig.format(MessagesConfig.RELOAD_ERROR, "{error}", e.getMessage()));
            plugin.getLogger().severe("Reload error: " + e.getMessage());
        }

        return true;
    }

    private boolean handleUpdateCommand(CommandSender sender) {
        if (!sender.hasPermission("customitems.update")) {
            sender.sendMessage(MessagesConfig.colorize(MessagesConfig.NO_PERMISSION));
            return false;
        }

        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.UPDATE_CHECKING));
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
            for (String sub : Arrays.asList("give", "list", "reload", "update")) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
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
                if (player.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }
}
