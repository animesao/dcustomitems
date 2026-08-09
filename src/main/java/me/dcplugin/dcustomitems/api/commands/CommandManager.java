package me.dcplugin.dcustomitems.api.commands;

import me.dcplugin.dcustomitems.Main;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Менеджер для управления кастомными командами
 */
public class CommandManager implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final Map<String, CustomCommand> commands = new HashMap<>();

    public CommandManager(Main plugin) {
        this.plugin = plugin;
    }

    // ===== РЕГИСТРАЦИЯ КОМАНД =====

    /**
     * Зарегистрировать кастомную команду
     */
    public boolean registerCommand(CustomCommand command) {
        if (command == null || command.getName() == null) {
            plugin.getLogger().warning("Попытка зарегистрировать null команду!");
            return false;
        }

        String name = command.getName().toLowerCase();

        if (commands.containsKey(name)) {
            plugin.getLogger().warning("Команда /" + name + " уже зарегистрирована!");
            return false;
        }

        // Регистрируем в Bukkit
        PluginCommand pluginCommand = plugin.getCommand(name);
        if (pluginCommand == null) {
            plugin.getLogger().warning("Команда /" + name + " не найдена в plugin.yml!");
            return false;
        }

        pluginCommand.setExecutor(this);
        pluginCommand.setTabCompleter(this);
        pluginCommand.setDescription(command.getDescription());
        pluginCommand.setUsage(command.getUsage());

        if (command.getPermission() != null) {
            pluginCommand.setPermission(command.getPermission());
        }

        // Сохраняем
        commands.put(name, command);

        // Вызываем callback
        command.onRegister();

        plugin.getLogger().info("Зарегистрирована команда: /" + name);
        return true;
    }

    /**
     * Зарегистрировать команду из конфигурации YAML
     */
    public boolean registerCommandFromConfig(String name, Map<String, Object> config) {
        YamlCommand command = new YamlCommand(name, config, plugin);
        return registerCommand(command);
    }

    /**
     * Отменить регистрацию команды
     */
    public void unregisterCommand(String name) {
        name = name.toLowerCase();
        CustomCommand command = commands.remove(name);
        if (command != null) {
            command.onUnregister();
            
            PluginCommand pluginCommand = plugin.getCommand(name);
            if (pluginCommand != null) {
                pluginCommand.setExecutor(null);
                pluginCommand.setTabCompleter(null);
            }
            
            plugin.getLogger().info("Команда /" + name + " отменена.");
        }
    }

    /**
     * Перезагрузить все команды из конфига
     */
    public void reloadCommands() {
        // Удаляем старые YAML команды
        Iterator<Map.Entry<String, CustomCommand>> it = commands.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, CustomCommand> entry = it.next();
            if (entry.getValue() instanceof YamlCommand) {
                unregisterCommand(entry.getKey());
            }
        }
        
        // Загружаем новые
        loadCommandsFromConfig();
    }

    /**
     * Загрузить команды из конфигурации
     */
    public void loadCommandsFromConfig() {
        if (plugin.getConfig().contains("commands")) {
            Set<String> commandNames = plugin.getConfig().getConfigurationSection("commands").getKeys(false);
            for (String name : commandNames) {
                Map<String, Object> config = new HashMap<>();
                config.put("description", plugin.getConfig().getString("commands." + name + ".description", ""));
                config.put("usage", plugin.getConfig().getString("commands." + name + ".usage", "/" + name));
                config.put("permission", plugin.getConfig().getString("commands." + name + ".permission", ""));
                config.put("aliases", plugin.getConfig().getStringList("commands." + name + ".aliases"));
                config.put("actions", plugin.getConfig().getStringList("commands." + name + ".actions"));
                config.put("cooldown", plugin.getConfig().getInt("commands." + name + ".cooldown", 0));
                config.put("requires-target", plugin.getConfig().getBoolean("commands." + name + ".requires-target", false));
                
                registerCommandFromConfig(name, config);
            }
        }
    }

    // ===== EXECUTOR =====

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();
        CustomCommand customCommand = commands.get(name);

        if (customCommand == null) {
            sender.sendMessage("\u00a7cКоманда не найдена!");
            return true;
        }

        // Проверка прав
        if (!customCommand.hasPermission(sender)) {
            sender.sendMessage(customCommand.getPermissionMessage());
            return true;
        }

        // Выполнение
        try {
            return customCommand.execute(sender, args);
        } catch (Exception e) {
            sender.sendMessage("\u00a7cОшибка при выполнении команды: " + e.getMessage());
            plugin.getLogger().severe("Ошибка в команде /" + name + ": " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }

    // ===== TAB COMPLETE =====

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase();
        CustomCommand customCommand = commands.get(name);

        if (customCommand == null) {
            return Collections.emptyList();
        }

        // Проверка прав
        if (!customCommand.hasPermission(sender)) {
            return Collections.emptyList();
        }

        try {
            return customCommand.tabComplete(sender, args, args.length - 1);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // ===== УТИЛИТЫ =====

    /**
     * Получить команду по имени
     */
    public CustomCommand getCommand(String name) {
        return commands.get(name.toLowerCase());
    }

    /**
     * Получить все зарегистрированные команды
     */
    public Collection<CustomCommand> getAllCommands() {
        return commands.values();
    }

    /**
     * Проверить существует ли команда
     */
    public boolean hasCommand(String name) {
        return commands.containsKey(name.toLowerCase());
    }

    /**
     * Получить список всех имён команд
     */
    public Set<String> getCommandNames() {
        return commands.keySet();
    }

    /**
     * Очистить все команды
     */
    public void clear() {
        for (String name : new ArrayList<>(commands.keySet())) {
            unregisterCommand(name);
        }
        commands.clear();
    }
}
