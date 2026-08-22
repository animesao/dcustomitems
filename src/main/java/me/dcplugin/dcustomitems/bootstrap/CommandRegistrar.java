package me.dcplugin.dcustomitems.bootstrap;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.ItemRegistry;
import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import me.dcplugin.dcustomitems.api.commands.CustomCommandExecutor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Field;
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
            CommandMap commandMap = getCommandMap();
            if (commandMap == null) return;

            // Access knownCommands via reflection (Paper remaps field names)
            Map<String, Command> knownCommands = getKnownCommands(commandMap);

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
        // Try Bukkit.getCommandMap() via reflection (Paper 1.21+)
        try {
            java.lang.reflect.Method getCommandMapMethod = Bukkit.class.getMethod("getCommandMap");
            return (CommandMap) getCommandMapMethod.invoke(null);
        } catch (Exception e) {
            // Fallback: field-based reflection (Spigot / older Paper)
            try {
                Field commandMapField = Bukkit.class.getDeclaredField("commandMap");
                commandMapField.setAccessible(true);
                return (CommandMap) commandMapField.get(null);
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to access CommandMap: " + ex.getMessage(), ex);
                return null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Command> getKnownCommands(CommandMap commandMap) {
        // Try all possible field names (Paper remaps some)
        for (String fieldName : new String[]{"knownCommands", "knownCommands"}) {
            try {
                Field field = commandMap.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                return (Map<String, Command>) field.get(commandMap);
            } catch (Exception ignored) {}
        }
        // Fallback: iterate all declared fields looking for a Map<String, Command>
        for (Field field : commandMap.getClass().getDeclaredFields()) {
            if (Map.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(commandMap);
                    if (value instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) value;
                        if (!map.isEmpty() && map.keySet().iterator().next() instanceof String) {
                            return (Map<String, Command>) value;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        plugin.getLogger().warning("Could not access knownCommands map");
        return new java.util.HashMap<>();
    }
}
