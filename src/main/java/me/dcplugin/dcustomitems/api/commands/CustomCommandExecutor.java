package me.dcplugin.dcustomitems.api.commands;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.ItemRegistry;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Level;

/**
 * Handler for all custom commands
 */
public class CustomCommandExecutor implements CommandExecutor, TabCompleter {

    private final ItemRegistry registry;

    public CustomCommandExecutor(ItemRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        CustomCommand cmd = registry.getCommand(label.toLowerCase());
        if (cmd == null) {
            // Try to look the command up by name
            for (CustomCommand c : registry.getAllCommands().values()) {
                if (c.getName().equalsIgnoreCase(label) || c.getAliases().contains(label.toLowerCase())) {
                    cmd = c;
                    break;
                }
            }
        }

        if (cmd == null) {
            sender.sendMessage("§cCommand not found: " + label);
            return true;
        }

        // Permission check
        if (!cmd.hasPermission(sender)) {
            sender.sendMessage(cmd.getPermissionMessage());
            return true;
        }

        try {
            return cmd.execute(sender, args);
        } catch (Exception e) {
            sender.sendMessage("§cError executing command: " + e.getMessage());
            Main plugin = Main.getInstance();
            if (plugin != null) {
                plugin.getLogger().log(Level.WARNING,
                    "Error executing command " + label + ": " + e.getMessage(), e);
            }
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        CustomCommand cmd = registry.getCommand(alias.toLowerCase());
        if (cmd == null) {
            for (CustomCommand c : registry.getAllCommands().values()) {
                if (c.getName().equalsIgnoreCase(alias) || c.getAliases().contains(alias.toLowerCase())) {
                    cmd = c;
                    break;
                }
            }
        }

        if (cmd == null) return Collections.emptyList();
        if (!cmd.hasPermission(sender)) return Collections.emptyList();

        return cmd.tabComplete(sender, args, args.length);
    }
}
