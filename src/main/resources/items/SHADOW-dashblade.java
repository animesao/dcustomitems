import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.potion.*;
import org.bukkit.scheduler.*;
import org.bukkit.util.*;

import java.util.*;

/**
 * ⚡ ТЕНЕВОЙ КЛИНОК - Механика Рывка
 * 
 * ПКМ - Рывок вперёд на 15 блоков
 * ЛКМ - Рывок к цели (если смотришь на моба/игрока)
 * 
 * Механика:
 * - Рывок сквозь воздух
 * - Оставляет теневые остатки
 * - Урон при рывке через врагов
 * - Кулдаун 5 сек
 * - 3 заряда (регенерация 1 заряд в 10 сек)
 * 
 * Дополнительно:
 * - Невидимость 2 сек после рывка
 * - Скорость I на 3 сек после рывка
 */
public class ShadowDashblade extends AbstractCustomItem {

    private final Map<UUID, Integer> charges = new HashMap<>();
    private final Map<UUID, Long> lastRegen = new HashMap<>();

    @Override
    public String getId() { return "shadow_dashblade"; }

    @Override
    public String getDisplayName() { return "&5&l⚡ Теневой Клинок"; }

    @Override
    public Material getMaterial() { return Material.NETHERITE_SWORD; }

    @Override
    public java.util.List<String> getLore() {
        return java.util.List.of(
            "",
            " &7&o\"Тень не знает границ...\"",
            "",
            " &5⚡ &fМеханика Рывка:",
            " &7• ПКМ - Рывок вперёд (15 блоков)",
            " &7• ЛКМ - Рывок к цели",
            "",
            " &d✦ &fЗаряды: &e3/3",
            " &7• Регенерация: 1 заряд / 10 сек",
            "",
            " &5🔮 &fБонусы после рывка:",
            " &7• Невидимость 2 сек",
            " &7• Скорость I 3 сек",
            " &7• Урон врагам на пути!",
            ""
        );
    }

    @Override
    public String getItemModel() { return "shadow_dashblade"; }

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public boolean isGlowing() { return true; }

    @Override
    public long getPeriodicInterval() { return 200; } // Каждые 10 сек

    /**
     * Регенерация зарядов
     */
    @Override
    public void onPeriodic(Player player) {
        UUID uuid = player.getUniqueId();
        int currentCharges = charges.getOrDefault(uuid, 3);
        Long last = lastRegen.getOrDefault(uuid, 0L);

        if (currentCharges < 3 && System.currentTimeMillis() - last > 10000) {
            charges.put(uuid, Math.min(currentCharges + 1, 3));
            lastRegen.put(uuid, System.currentTimeMillis());

            if (charges.get(uuid) < 3) {
                player.sendActionBar(ChatColor.LIGHT_PURPLE + "⚡ Заряд регенерирован: " + charges.get(uuid) + "/3");
            }
        }
    }

    /**
     * ПКМ - Рывок вперёд
     */
    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        UUID uuid = player.getUniqueId();
        int currentCharges = charges.getOrDefault(uuid, 3);

        if (currentCharges <= 0) {
            player.sendMessage(ChatColor.RED + "❌ Нет зарядов!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Тратим заряд
        charges.put(uuid, currentCharges - 1);

        // Рывок вперёд
        performDash(player, player.getLocation().getDirection().multiply(15), true);

        // Уведомление
        player.sendActionBar(ChatColor.LIGHT_PURPLE + "⚡ Заряды: " + charges.get(uuid) + "/3");
    }

    /**
     * ЛКМ - Рывок к цели (если смотришь на моба/игрока)
     */
    @Override
    public void onLeftClick(PlayerInteractEvent event, Player player) {
        UUID uuid = player.getUniqueId();
        int currentCharges = charges.getOrDefault(uuid, 3);

        if (currentCharges <= 0) {
            player.sendMessage(ChatColor.RED + "❌ Нет зарядов!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Ищем цель в радиусе 20 блоков
        RayTraceResult rayTrace = player.getWorld().rayTrace(
            player.getLocation().add(0, player.getEyeHeight(), 0),
            player.getLocation().getDirection(),
            20,
            RayTraceResult.ShapeType.ENTITY,
            false,
            false,
            null,
            entity -> entity instanceof LivingEntity && entity != player
        );

        if (rayTrace != null && rayTrace.getHitEntity() != null) {
            // Рывок к цели
            Location targetLoc = rayTrace.getHitEntity().getLocation();
            charges.put(uuid, currentCharges - 1);

            performDashToTarget(player, targetLoc);

            // Урон при приземлении
            if (rayTrace.getHitEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) rayTrace.getHitEntity();
                target.damage(12, player);
                target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 30);
            }

            player.sendActionBar(ChatColor.LIGHT_PURPLE + "⚡ Заряды: " + charges.get(uuid) + "/3");
        } else {
            // Обычный рывок вперёд если нет цели
            charges.put(uuid, currentCharges - 1);
            performDash(player, player.getLocation().getDirection().multiply(10), true);
            player.sendActionBar(ChatColor.LIGHT_PURPLE + "⚡ Заряды: " + charges.get(uuid) + "/3");
        }
    }

    /**
     * Рывок вперёд с уроном
     */
    private void performDash(Player player, Vector direction, boolean damageEntities) {
        Location startLoc = player.getLocation().clone();

        // Эффекты перед рывком
        player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 50);
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 30);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BREATH, 1f, 2f);

        // Телепортация
        Location targetLoc = player.getLocation().add(direction);
        targetLoc.setY(player.getWorld().getHighestBlockYAt(targetLoc) + 1);
        player.teleport(targetLoc);

        // Эффекты после рывка
        player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 50);
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 30);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

        // Урон всем на пути
        if (damageEntities) {
            for (Entity entity : player.getNearbyEntities(3, 3, 3)) {
                if (entity instanceof LivingEntity && entity != player) {
                    LivingEntity living = (LivingEntity) entity;
                    living.damage(10, player);
                    living.getWorld().spawnParticle(Particle.CRIT, living.getLocation().add(0, 1, 0), 20);
                }
            }
        }

        // Бонусы после рывка
        applyPostDashEffects(player);

        // Теневые остатки
        createShadowTrail(startLoc, player.getLocation());
    }

    /**
     * Рывок к цели (быстрая телепортация)
     */
    private void performDashToTarget(Player player, Location target) {
        Location startLoc = player.getLocation().clone();

        // Эффекты перед рывком
        player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 50);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.5f);

        // Быстрая телепортация (через scheduler для анимации)
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("DC-CustomItems"),
            () -> {
                player.teleport(target.add(0, 0, 0));
                player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 50);
                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 30);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

                applyPostDashEffects(player);
                createShadowTrail(startLoc, player.getLocation());
            },
            2L // 2 тика задержки
        );
    }

    /**
     * Бонусы после рывка
     */
    private void applyPostDashEffects(Player player) {
        // Невидимость 2 сек
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0, false, false));

        // Скорость I на 3 сек
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, false, false));

        // Частицы
        player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 30);
    }

    /**
     * Создаёт теневые остатки между начальной и конечной точками
     */
    private void createShadowTrail(Location start, Location end) {
        Vector direction = end.toVector().subtract(start.toVector());
        double distance = direction.length();
        Vector step = direction.normalize().multiply(0.5);

        Location current = start.clone();
        for (double i = 0; i < distance; i += 0.5) {
            current.add(step);
            start.getWorld().spawnParticle(
                Particle.SMOKE_LARGE, 
                current, 
                2, 
                0.1, 0.1, 0.1, 
                0.01
            );
        }
    }
}
