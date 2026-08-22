package me.dcplugin.dcustomitems.commands;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.config.MessagesConfig;
import me.dcplugin.dcustomitems.utils.EnumCache;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Единственная встроенная команда плагина: /ci reload
 *
 * Все остальные команды (give, buy, sell, и т.д.)
 * регистрируются модулями через items/<module>/
 */
public class CustomItemsCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public CustomItemsCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("reload")) {
            return handleReloadCommand(sender);
        }

        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.CI_HEADER));
        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.CI_HELP_RELOAD));
        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("dcustomitems.reload")) {
            sender.sendMessage(MessagesConfig.colorize(MessagesConfig.NO_PERMISSION));
            return false;
        }

        try {
            // Перезагружаем конфиг
            plugin.getConfigManager().reloadConfig();

            // Перезагружаем YAML-предметы
            plugin.getItemHandler().reloadItems();

            // Перезагружаем сет-бонусы
            plugin.getArmorSetManager().reload();

            // Перезагружаем Java API (предметы, команды, плейсхолдеры)
            if (plugin.getApiItemRegistry() != null) {
                plugin.getApiItemRegistry().reload();

                int items = plugin.getApiItemRegistry().getItemCount();
                int cmds = plugin.getApiItemRegistry().getCommandCount();
                int phs = plugin.getApiItemRegistry().getPlaceholderCount();

                // Перерегистрируем кастомные команды
                plugin.registerCustomCommands();

                sender.sendMessage(MessagesConfig.colorize(MessagesConfig.RELOAD_SUCCESS));
                sender.sendMessage(MessagesConfig.format(MessagesConfig.RELOAD_YAML,
                    "{count}", String.valueOf(plugin.getItemHandler().getAllCustomItems().size())));
                sender.sendMessage(MessagesConfig.format(MessagesConfig.RELOAD_JAVA_ITEMS,
                    "{count}", String.valueOf(items)));
                sender.sendMessage(MessagesConfig.format(MessagesConfig.RELOAD_JAVA_COMMANDS,
                    "{count}", String.valueOf(cmds)));
                sender.sendMessage(MessagesConfig.format(MessagesConfig.RELOAD_JAVA_PLACEHOLDERS,
                    "{count}", String.valueOf(phs)));
            } else {
                sender.sendMessage(MessagesConfig.colorize(MessagesConfig.RELOAD_SUCCESS));
            }

            // Перезагружаем модули
            if (plugin.getModuleManager() != null) {
                plugin.getModuleManager().reloadAll();
                sender.sendMessage(MessagesConfig.colorize(
                    "&7Модулей: &e" + plugin.getModuleManager().getModuleCount()));
            }

            // EnumCache статистика (для отладки)
            if (plugin.getConfig().getBoolean("debug-mode", false)) {
                Map<String, Integer> cacheStats = EnumCache.getCacheStats();
                sender.sendMessage(MessagesConfig.colorize("&8[&6DCI&8] &7EnumCache:"));
                for (Map.Entry<String, Integer> entry : cacheStats.entrySet()) {
                    sender.sendMessage(MessagesConfig.colorize(
                        "&8  &7" + entry.getKey() + ": &e" + entry.getValue()));
                }
            }

        } catch (Exception e) {
            sender.sendMessage(MessagesConfig.format(MessagesConfig.RELOAD_ERROR,
                "{error}", e.getMessage()));
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                "Reload error: " + e.getMessage(), e);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && "reload".startsWith(args[0].toLowerCase())) {
            return Collections.singletonList("reload");
        }
        return new ArrayList<>();
    }
}
