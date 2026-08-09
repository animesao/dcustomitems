import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

/**
 * Команда /heal - Исцеление
 * 
 * Положи этот файл в plugins/DC-CustomItems/items/
 * Сделай /ci reload
 * Команда будет доступна!
 */
public class HealCommand extends CustomCommand {

    public HealCommand() {
        super("heal", "Исцелить себя или игрока", "/heal [игрок]", "dci.command.heal", "healme");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player target = getTarget(sender, args, 0);
        if (target == null) return false;

        // Лечение
        double maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getValue();
        target.setHealth(maxHealth);
        target.setFoodLevel(20);
        target.setSaturation(20f);

        // Очищаем негативные эффекты
        for (var effect : target.getActivePotionEffects()) {
            if (effect.getType().equals(PotionEffectType.POISON) ||
                effect.getType().equals(PotionEffectType.WITHER) ||
                effect.getType().equals(PotionEffectType.NAUSEA)) {
                target.removePotionEffect(effect.getType());
            }
        }

        // Эффекты
        target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 2, 0), 30);
        target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, target.getLocation().add(0, 1, 0), 20);
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);

        // Сообщения
        if (sender.equals(target)) {
            msg(sender, "&aВы полностью исцелены!");
        } else {
            msg(sender, "&aВы исцелили " + target.getName() + "!");
            msg(target, "&aВы были исцелены!");
        }

        // Заголовок
        title(target, "&a❤ ИСЦЕЛЕНИЕ!", "&7Полное здоровье!");

        return true;
    }

    @Override
    public java.util.List<String> tabComplete(CommandSender sender, String[] args, int cursor) {
        if (cursor == 0) {
            // Имена онлайн игроков
            java.util.List<String> players = new java.util.ArrayList<>();
            for (Player p : sender.getServer().getOnlinePlayers()) {
                players.add(p.getName());
            }
            return players;
        }
        return java.util.Collections.emptyList();
    }
}
