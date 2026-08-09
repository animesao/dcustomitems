import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import org.bukkit.entity.Player;
import org.bukkit.command.*;

/**
 * ✅ ПРИМЕР КОМАНДЫ (УСПЕШНАЯ)
 * 
 * Эта команда работает без ошибок!
 * 
 * В консоли выведет зелёное сообщение:
 * [API] ✅ Command: /heal
 * 
 * Использование:
 * /heal [игрок]
 */
public class EXAMPLEcommandSuccess extends CustomCommand {

    public EXAMPLEcommandSuccess() {
        super(
            "heal",                          // Имя команды
            "Исцелить себя или игрока",      // Описание
            "/heal [игрок]",                 // Использование
            "dci.command.heal",              // Право
            "healme", "health"               // Алиасы
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Проверка прав
        if (!hasPermission(sender)) {
            msg(sender, getPermissionMessage());
            return true;
        }

        // Получаем цель
        Player target = getTarget(sender, args, 0);
        if (target == null) return false;

        // Лечение
        target.setHealth(target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        target.setFoodLevel(20);

        // Эффекты
        target.getWorld().spawnParticle(org.bukkit.Particle.HEART, target.getLocation().add(0, 2, 0), 20);
        target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);

        // Сообщения
        if (sender.equals(target)) {
            msg(sender, "&aВы полностью исцелены!");
        } else {
            msg(sender, "&aВы исцелили " + target.getName() + "!");
            msg(target, "&aВас исцелили!");
        }

        return true;
    }

    @Override
    public java.util.List<String> tabComplete(CommandSender sender, String[] args, int cursor) {
        if (cursor == 0) {
            java.util.List<String> players = new java.util.ArrayList<>();
            for (Player p : sender.getServer().getOnlinePlayers()) {
                players.add(p.getName());
            }
            return players;
        }
        return java.util.Collections.emptyList();
    }
}
