import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.modules.Module;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Открывает магазин DeluxeMenuX.
 *
 * Команда:
 * /shop
 */
public class shop_commandItem extends CustomCommand {

    public shop_commandItem() {
        super(
            "shop",
            "Открыть магазин",
            "/shop",
            "deluxemenux.use"
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            msg(sender, "&cТолько для игроков!");
            return true;
        }

        Main plugin = Main.getInstance();
        if (plugin == null || plugin.getModuleManager() == null) {
            msg(sender, "&cМодуль DeluxeMenuX не загружен!");
            return true;
        }

        Module module = plugin.getModuleManager().getModule("deluxemenux");
        if (!module.openMenu((Player) sender, "shop")) {
            msg(sender, "&cМеню shop не найдено!");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, int cursor) {
        return Collections.emptyList();
    }
}
