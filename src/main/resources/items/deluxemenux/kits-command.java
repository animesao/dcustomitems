import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.modules.Module;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Открывает меню наборов DeluxeMenuX.
 *
 * Команды:
 * /kits
 * /kits list
 */
public class kits_commandItem extends CustomCommand {

    public kits_commandItem() {
        super(
            "kits",
            "Открыть меню наборов",
            "/kits",
            "deluxemenux.use",
            "kit"
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
        if (!(module instanceof deluxemenuxModule)) {
            msg(sender, "&cМодуль DeluxeMenuX не загружен!");
            return true;
        }

        ((deluxemenuxModule) module).openMenu((Player) sender, "kits");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, int cursor) {
        return Collections.emptyList();
    }
}
