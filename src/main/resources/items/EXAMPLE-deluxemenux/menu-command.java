import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.modules.Module;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 🎨 Команда /menu - Открыть меню
 * 
 * Использование:
 *   /menu          - главное меню
 *   /menu <id>     - открыть меню по ID
 *   /menu list     - список меню
 *   /menu reload   - перезагрузить меню
 */
public class menu_commandItem extends CustomCommand {

    public menu_commandItem() {
        super(
            "menu",
            "Открыть меню",
            "/menu [id]",
            "deluxemenux.use",
            "deluxemenu"
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Main plugin = Main.getInstance();
        if (plugin == null) {
            msg(sender, "&cПлагин не найден!");
            return true;
        }

        // Получаем модуль DeluxeMenuX
        Module module = plugin.getModuleManager().getModule("deluxemenux");
        if (module == null) {
            msg(sender, "&cМодуль DeluxeMenuX не загружен!");
            return true;
        }

        if (args.length == 0) {
            // Открываем главное меню
            if (sender instanceof Player) {
                openMenu((Player) sender, "main");
            } else {
                msg(sender, "&cТолько для игроков!");
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                return handleList(sender);
            case "reload":
                return handleReload(sender, module);
            default:
                // Открыть меню по ID
                if (sender instanceof Player) {
                    openMenu((Player) sender, args[0]);
                } else {
                    msg(sender, "&cТолько для игроков!");
                }
                return true;
        }
    }

    private boolean handleList(CommandSender sender) {
        msg(sender, "&6=== Меню ===");
        // TODO: Получить список меню из модуля
        msg(sender, "&e/shop &7- Магазин");
        msg(sender, "&e/main &7- Главное меню");
        return true;
    }

    private boolean handleReload(CommandSender sender, Module module) {
        if (!sender.hasPermission("deluxemenux.admin")) {
            msg(sender, "&cУ вас нет прав!");
            return true;
        }
        
        module.reload();
        msg(sender, "&aМеню перезагружены!");
        return true;
    }

    private void openMenu(Player player, String menuId) {
        Main plugin = Main.getInstance();
        if (plugin == null) return;
        
        Module module = plugin.getModuleManager().getModule("deluxemenux");
        module.openMenu(player, menuId);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, int cursor) {
        if (cursor == 0) {
            List<String> options = new ArrayList<>();
            options.add("list");
            options.add("reload");
            options.add("shop");
            options.add("main");
            return options;
        }
        return Collections.emptyList();
    }
}
