import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.gui.CustomGUI;
import me.dcplugin.dcustomitems.api.gui.GUIManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 🎨 Команда /gui - Открыть GUI
 * 
 * Использование:
 *   /gui <id>      - открыть GUI
 *   /gui list      - список GUI
 * 
 * Право: dci.command.gui
 */
public class gui_commandItem extends CustomCommand {

    public gui_commandItem() {
        super(
            "gui",
            "Открыть GUI",
            "/gui <id>",
            "dci.command.gui"
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            msg(sender, "&cТолько для игроков!");
            return true;
        }

        Player player = (Player) sender;
        Main plugin = Main.getInstance();
        
        if (plugin == null) {
            msg(sender, "&cПлагин не найден!");
            return true;
        }

        GUIManager guiManager = plugin.getGUIManager();
        
        if (args.length == 0) {
            msg(sender, "&6Использование: &e/gui <id>");
            msg(sender, "&7Используйте &e/gui list &7для списка");
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            msg(sender, "&6=== GUI ===");
            if (guiManager.getGUICount() == 0) {
                msg(sender, "&cНет доступных GUI");
            } else {
                for (String id : guiManager.getAllGUIs().keySet()) {
                    CustomGUI gui = guiManager.getGUI(id);
                    msg(sender, "&e" + id + " &7- " + gui.getTitle());
                }
            }
            return true;
        }

        String guiId = args[0].toLowerCase();
        
        if (!guiManager.getAllGUIs().containsKey(guiId)) {
            msg(sender, "&cGUI не найдено: " + guiId);
            return true;
        }

        guiManager.openGUI(player, guiId);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, int cursor) {
        if (cursor == 0) {
            Main plugin = Main.getInstance();
            if (plugin != null && plugin.getGUIManager() != null) {
                List<String> guis = new ArrayList<>(plugin.getGUIManager().getAllGUIs().keySet());
                guis.add("list");
                return guis;
            }
        }
        return Collections.emptyList();
    }
}
