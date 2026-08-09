package me.dcplugin.dcustomitems.api;

import me.dcplugin.dcustomitems.api.config.MessagesConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

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

        switch (args[0].toLowerCase()) {
            case "give": return handleGive(sender, args);
            case "list": return handleList(sender);
            case "info": return handleInfo(sender, args);
            default: sendHelp(sender); return true;
        }
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessagesConfig.colorize(MessagesConfig.API_GIVE_USAGE));
            return true;
        }

        String itemId = args[1];
        AbstractCustomItem item = registry.getItem(itemId);
        if (item == null) {
            sender.sendMessage(MessagesConfig.format(MessagesConfig.ITEM_NOT_FOUND, "{item}", itemId));
            return true;
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(MessagesConfig.colorize(MessagesConfig.PLAYER_NOT_FOUND));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(MessagesConfig.colorize(MessagesConfig.API_GIVE_USAGE));
            return true;
        }

        ItemStack stack = item.createItemStack();
        target.getInventory().addItem(stack);
        target.sendMessage(MessagesConfig.format(MessagesConfig.CI_GIVE_RECEIVED, "{item}", item.getDisplayName()));

        if (!sender.equals(target)) {
            sender.sendMessage(MessagesConfig.format(MessagesConfig.API_GIVE_OTHER,
                "{item}", item.getDisplayName(), "{player}", target.getName()));
        }

        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (registry.getAllItems().isEmpty()) {
            sender.sendMessage(MessagesConfig.colorize(MessagesConfig.API_LIST_EMPTY));
            return true;
        }

        sender.sendMessage(MessagesConfig.format(MessagesConfig.API_HEADER, "{count}", String.valueOf(registry.getAllItems().size())));
        for (var entry : registry.getAllItems().entrySet()) {
            AbstractCustomItem item = entry.getValue();
            sender.sendMessage(MessagesConfig.colorize(
                " &e" + item.getId() + " &7- " + item.getDisplayName() + " &7[" + item.getType() + "]"
            ));
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessagesConfig.colorize(MessagesConfig.API_GIVE_USAGE));
            return true;
        }

        String itemId = args[1];
        AbstractCustomItem item = registry.getItem(itemId);
        if (item == null) {
            sender.sendMessage(MessagesConfig.format(MessagesConfig.ITEM_NOT_FOUND, "{item}", itemId));
            return true;
        }

        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.API_INFO_HEADER));
        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.format(MessagesConfig.API_INFO_ID, "{id}", item.getId())));
        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.format(MessagesConfig.API_INFO_NAME, "{name}", item.getDisplayName())));
        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.format(MessagesConfig.API_INFO_MATERIAL, "{material}", item.getMaterial().name())));
        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.format(MessagesConfig.API_INFO_TYPE, "{type}", item.getType())));
        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.format(MessagesConfig.API_INFO_SLOT, "{slot}", item.getActivationSlot())));
        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.format(MessagesConfig.API_INFO_COOLDOWN, "{cooldown}", String.valueOf(item.getClickCooldown()))));
        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.format(MessagesConfig.API_INFO_UNBREAKABLE, "{value}", String.valueOf(item.isUnbreakable()))));
        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.format(MessagesConfig.API_INFO_GLOWING, "{value}", String.valueOf(item.isGlowing()))));
        if (item.getItemModel() != null) {
            sender.sendMessage(MessagesConfig.colorize(MessagesConfig.format(MessagesConfig.API_INFO_MODEL, "{model}", item.getItemModel())));
        }
        if (item.getPermission() != null) {
            sender.sendMessage(MessagesConfig.colorize(MessagesConfig.format(MessagesConfig.API_INFO_PERMISSION, "{perm}", item.getPermission())));
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.API_HEADER));
        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.API_HELP_GIVE));
        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.API_HELP_LIST));
        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.API_HELP_INFO));
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
