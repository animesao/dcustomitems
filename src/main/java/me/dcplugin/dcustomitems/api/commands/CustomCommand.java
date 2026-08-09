package me.dcplugin.dcustomitems.api.commands;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Базовый класс для создания кастомных команд
 */
public abstract class CustomCommand {

    private final String name;
    private final String description;
    private final String usage;
    private final String permission;
    private final List<String> aliases;
    private final String permissionMessage;

    protected CustomCommand(String name, String description, String usage, String permission) {
        this(name, description, usage, permission, Collections.emptyList(), "\u00a7c\u0423 \u0432\u0430\u0441 \u043d\u0435\u0442 \u043f\u0440\u0430\u0432!");
    }

    protected CustomCommand(String name, String description, String usage, String permission, List<String> aliases) {
        this(name, description, usage, permission, aliases, "\u00a7c\u0423 \u0432\u0430\u0441 \u043d\u0435\u0442 \u043f\u0440\u0430\u0432!");
    }

    protected CustomCommand(String name, String description, String usage, String permission, List<String> aliases, String permissionMessage) {
        this.name = name.toLowerCase();
        this.description = description;
        this.usage = usage;
        this.permission = permission;
        this.aliases = new ArrayList<>(aliases);
        this.permissionMessage = permissionMessage;
    }

    public abstract boolean execute(CommandSender sender, String[] args);

    public List<String> tabComplete(CommandSender sender, String[] args, int cursor) {
        return Collections.emptyList();
    }

    public void onRegister() {}
    public void onUnregister() {}

    public boolean hasPermission(CommandSender sender) {
        if (permission == null || permission.isEmpty()) return true;
        return sender.hasPermission(permission);
    }

    protected Player getTarget(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            Player target = sender.getServer().getPlayer(args[index]);
            if (target == null) {
                sendMessage(sender, "\u00a7c\u0418\u0433\u0440\u043e\u043a " + args[index] + " \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d!");
                return null;
            }
            return target;
        }
        if (sender instanceof Player) {
            return (Player) sender;
        }
        sendMessage(sender, "\u00a7c\u0423\u043a\u0430\u0436\u0438\u0442\u0435 \u0438\u0433\u0440\u043e\u043a\u0430: " + usage);
        return null;
    }

    protected int getInt(CommandSender sender, String[] args, int index, int defaultValue) {
        if (args.length <= index) return defaultValue;
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException e) {
            sendMessage(sender, "\u00a7c" + args[index] + " is not a number!");
            return defaultValue;
        }
    }

    protected double getDouble(CommandSender sender, String[] args, int index, double defaultValue) {
        if (args.length <= index) return defaultValue;
        try {
            return Double.parseDouble(args[index]);
        } catch (NumberFormatException e) {
            sendMessage(sender, "\u00a7c" + args[index] + " is not a number!");
            return defaultValue;
        }
    }

    protected String joinArgs(String[] args, int startIndex) {
        if (args.length <= startIndex) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
    }

    protected void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(colorize(message));
    }

    protected void sendTitle(Player player, String title, String subtitle) {
        player.sendTitle(colorize(title), colorize(subtitle), 10, 40, 10);
    }

    protected String colorize(String message) {
        if (message == null) return "";
        return message.replace("&", "\u00a7");
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getUsage() { return usage; }
    public String getPermission() { return permission; }
    public List<String> getAliases() { return aliases; }
    public String getPermissionMessage() { return permissionMessage; }

    @Override
    public String toString() {
        return "/" + name + " - " + description;
    }
}
