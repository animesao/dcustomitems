import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ✈ Команда /fly - Включить/выключить полёт
 * 
 * Использование:
 *   /fly        - включить/выключить полёт себе
 *   /fly <nick> - включить/выключить полёт игроку
 * 
 * Право: dci.command.fly
 */
public class FlyCommand extends CustomCommand {

    public FlyCommand() {
        super(
            "fly",           // ID команды
            "Включить/выключить полёт",  // Описание
            "/fly [nick]",   // Использование
            "dci.command.fly" // Право
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Получаем целевого игрока
        Player target = getTarget(sender, args, 0);
        if (target == null) {
            return false;
        }

        // Переключаем полёт
        boolean wasFlying = target.getAllowFlight();
        target.setAllowFlight(!wasFlying);
        target.setFlying(!wasFlying);

        // Отправляем сообщение
        if (!wasFlying) {
            msg(sender, "&a✈ Полёт включён для &e" + target.getName());
            if (!sender.equals(target)) {
                msg(target, "&a✈ Полёт включён!");
            }
            target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
        } else {
            msg(sender, "&c✈ Полёт выключен для &e" + target.getName());
            if (!sender.equals(target)) {
                msg(target, "&c✈ Полёт выключен!");
            }
            target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.5f);
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, int cursor) {
        if (cursor == 0) {
            // Автодополнение имён онлайн-игроков
            List<String> players = new ArrayList<>();
            for (Player p : sender.getServer().getOnlinePlayers()) {
                players.add(p.getName());
            }
            return players;
        }
        return Collections.emptyList();
    }
}
