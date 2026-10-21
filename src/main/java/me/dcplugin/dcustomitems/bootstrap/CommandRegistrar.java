package me.dcplugin.dcustomitems.bootstrap;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.ItemRegistry;
import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import me.dcplugin.dcustomitems.api.commands.CustomCommandExecutor;
import me.dcplugin.dcustomitems.utils.CommandMapAccessor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;

import java.util.*;
import java.util.logging.Level;

/**
 * Регистрация/удаление динамических команд через рефлексию Bukkit CommandMap.
 * Вынесен из Main для разделения ответственности.
 */
public class CommandRegistrar {

    private final Main plugin;
    private final Set<Command> dynamicCommands = Collections.newSetFromMap(new IdentityHashMap<>());

    public CommandRegistrar(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Зарегистрировать все команды из API реестра
     */
    @SuppressWarnings("unchecked")
    public void registerAll(ItemRegistry registry) {
        try {
            CommandMap commandMap = getCommandMap();
            if (commandMap == null) return;

            CustomCommandExecutor executor = new CustomCommandExecutor(registry);

            for (CustomCommand cmd : registry.getAllCommands().values()) {
                Command bukkitCmd = createBukkitCommand(cmd, executor);
                bukkitCmd.setPermission(cmd.getPermission());
                commandMap.register(plugin.getName(), bukkitCmd);
                dynamicCommands.add(bukkitCmd);
                plugin.getLogger().info("[API] Registered command: /" + cmd.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to register custom commands: " + e.getMessage(), e);
        }
    }

    /**
     * Удалить все динамически зарегистрированные команды
     */
    @SuppressWarnings("unchecked")
    public void unregisterAll() {
        try {
            CommandMap commandMap = CommandMapAccessor.getCommandMap();
            if (commandMap == null) return;

            // Access knownCommands via reflection (Paper remaps field names)
            Map<String, Command> knownCommands = CommandMapAccessor.getKnownCommands(commandMap);
            if (knownCommands == null) {
                plugin.getLogger().warning("[API] knownCommands недоступна — команды остаются в CommandMap");
                return;
            }

            for (Command cmd : dynamicCommands) {
                knownCommands.remove(cmd.getName().toLowerCase());
                for (String alias : cmd.getAliases()) {
                    knownCommands.remove(alias.toLowerCase());
                }
            }
            dynamicCommands.clear();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to unregister custom commands: " + e.getMessage(), e);
        }
    }

    // ===== Internal =====

    private Command createBukkitCommand(CustomCommand cmd, CustomCommandExecutor executor) {
        return new Command(
                cmd.getName(),
                cmd.getDescription() != null ? cmd.getDescription() : "",
                cmd.getUsage() != null ? "/" + cmd.getName() : "",
                cmd.getAliases() != null ? new ArrayList<>(cmd.getAliases()) : new ArrayList<>()
        ) {
            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                return executor.onCommand(sender, this, commandLabel, args);
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
                return executor.onTabComplete(sender, this, alias, args);
            }
        };
    }

    private CommandMap getCommandMap() {
        return CommandMapAccessor.getCommandMap();
    }
}
