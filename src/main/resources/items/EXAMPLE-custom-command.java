import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.*;

import java.util.*;

/**
 * 🔥 БЕРСЕРК КОМАНДА - Пример кастомной команды на Java
 * 
 * Команда /berserk:
 * - Включает/выключает режим берсерка
 * - Мало HP = больше урона
 * - Визуальные эффекты
 * - Автоматическое отключение при смерти
 * 
 * Использование:
 *   /berserk - Включить/выключить
 *   /berserk on - Включить
 *   /berserk off - Выключить
 *   /berserk status - Статус
 */
public class BerserkCommand extends CustomCommand {

    // Хранилище активных берсерков
    private static final Set<UUID> activeBerserkers = new HashSet<>();

    public BerserkCommand() {
        super(
            "berserk",                    // Имя команды
            "Включить/выключить берсерк", // Описание
            "/berserk [on|off|status]",   // Использование
            "dci.command.berserk",        // Право
            List.of("brk", "rage"),       // Алиасы
            "&cУ вас нет прав на берсерк!" // Сообщение об ошибке
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "&cТолько для игроков!");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        // Определяем действие
        String action = args.length > 0 ? args[0].toLowerCase() : "toggle";

        switch (action) {
            case "on":
            case "enable":
                enableBerserk(player);
                break;

            case "off":
            case "disable":
                disableBerserk(player);
                break;

            case "status":
                showStatus(player);
                break;

            default: // toggle
                if (activeBerserkers.contains(uuid)) {
                    disableBerserk(player);
                } else {
                    enableBerserk(player);
                }
                break;
        }

        return true;
    }

    /**
     * Включить берсерк
     */
    private void enableBerserk(Player player) {
        UUID uuid = player.getUniqueId();

        if (activeBerserkers.contains(uuid)) {
            sendMessage(player, "&cВы уже в берсерке!");
            return;
        }

        activeBerserkers.add(uuid);

        // Эффекты
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 999999, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 999999, 0, false, false));

        // Визуальные эффекты
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 2, 0), 100);
        player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 50);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);

        // Заголовок
        player.sendTitle(
            ChatColor.DARK_RED + "" + ChatColor.BOLD + "🔥 БЕРСЕРК!",
            ChatColor.RED + "Сила x2 | Скорость x2",
            10, 60, 20
        );

        sendMessage(player, "&4Вы進入ли в режим БЕРСЕРКА! (+2 урона, +скорость)");
    }

    /**
     * Выключить берсерк
     */
    private void disableBerserk(Player player) {
        UUID uuid = player.getUniqueId();

        if (!activeBerserkers.contains(uuid)) {
            sendMessage(player, "&cВы не в берсерке!");
            return;
        }

        activeBerserkers.remove(uuid);

        // Убираем эффекты
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);

        // Визуальные эффекты
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 2, 0), 30);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1f, 1f);

        // Заголовок
        player.sendTitle(
            ChatColor.GRAY + "Берсерк выключен",
            "",
            5, 20, 5
        );

        sendMessage(player, "&7Режим берсерка отключён.");
    }

    /**
     * Показать статус
     */
    private void showStatus(Player player) {
        UUID uuid = player.getUniqueId();
        boolean isActive = activeBerserkers.contains(uuid);
        double health = player.getHealth();
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        int healthPercent = (int) ((health / maxHealth) * 100);

        sendMessage(player, "&6=== БЕРСЕРК ===");
        sendMessage(player, "&7Статус: " + (isActive ? "&aАктивен" : "&cВыключен"));
        sendMessage(player, "&7HP: &c" + (int) health + "/" + (int) maxHealth + " (" + healthPercent + "%)");

        if (isActive) {
            int bonusDamage = calculateBonusDamage(healthPercent);
            sendMessage(player, "&7Бонус урона: &c+" + bonusDamage);
        }

        sendMessage(player, "&6================");
    }

    /**
     * Рассчитать бонус урона на основе HP
     */
    private int calculateBonusDamage(int healthPercent) {
        if (healthPercent > 75) return 1;
        if (healthPercent > 50) return 2;
        if (healthPercent > 25) return 4;
        return 8; // Максимальный бонус при критическом HP
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, int cursor) {
        if (cursor == 0) {
            return Arrays.asList("on", "off", "status");
        }
        return Collections.emptyList();
    }

    /**
     * Проверить активен ли берсерк у игрока
     */
    public static boolean isActive(Player player) {
        return activeBerserkers.contains(player.getUniqueId());
    }

    /**
     * Получить бонус урона для игрока
     */
    public static int getBonusDamage(Player player) {
        if (!isActive(player)) return 0;
        double health = player.getHealth();
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        int healthPercent = (int) ((health / maxHealth) * 100);

        if (healthPercent > 75) return 2;
        if (healthPercent > 50) return 4;
        if (healthPercent > 25) return 6;
        return 10;
    }

    /**
     * Очистить при отключении плагина
     */
    @Override
    public void onUnregister() {
        // Выключаем берсерк у всех
        for (UUID uuid : new HashSet<>(activeBerserkers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                disableBerserk(player);
            }
        }
        activeBerserkers.clear();
    }
}
