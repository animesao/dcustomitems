import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.potion.*;
import org.bukkit.scheduler.*;

import java.util.*;

/**
 * 🔥 БЕРСЕРК ТОПОР - Механика Ярости
 * 
 * Чем меньше HP, тем ты СИЛЬНЕЕ!
 * 
 * Механика:
 * - 100-75% HP: x1.5 урона, Скорость I
 * - 75-50% HP: x2 урона, Скорость II
 * - 50-25% HP: x3 урона, Скорость III, Регенерация I
 * - 25-0% HP: x5 урона, Скорость IV, Регенерация II, Невидимость на 3 сек при ударе
 * 
 * При убийстве: +2 сердца на 10 сек
 * При смерти: Взрыв ярости (урон всем рядом)
 */
public class BerserkerAxe extends AbstractCustomItem {

    // Хранилище множителей для игроков
    private final Map<UUID, Double> damageMultipliers = new HashMap<>();
    private final Map<UUID, Integer> rageLevels = new HashMap<>();

    @Override
    public String getId() { return "berserker_axe"; }

    @Override
    public String getDisplayName() { return "&4&l🔥 Берсерк Топор"; }

    @Override
    public Material getMaterial() { return Material.NETHERITE_AXE; }

    @Override
    public java.util.List<String> getLore() {
        return java.util.List.of(
            "",
            " &7&o\"Чем меньше крови, тем сильнее клинок\"",
            "",
            " &4⚔ &fМеханика Ярости:",
            " &7• 100-75% HP: &cx1.5 урона",
            " &7• 75-50% HP: &cx2 урона",
            " &7• 50-25% HP: &cx3 урона + регенерация",
            " &7• 25-0% HP: &cx5 урона + невидимость!",
            "",
            " &c🔥 &fПри убийстве: +2 сердца",
            " &4💀 &fПри смерти: ВЗРЫВ ЯРОСТИ!",
            ""
        );
    }

    @Override
    public String getItemModel() { return "berserker_axe"; }

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public boolean isGlowing() { return true; }

    @Override
    public long getPeriodicInterval() { return 20; } // Каждую секунду

    /**
     * Вызывается каждую секунду - обновляем уровень ярости
     */
    @Override
    public void onPeriodic(Player player) {
        updateRageLevel(player);
    }

    /**
     * Обновляет уровень ярости на основе HP
     */
    private void updateRageLevel(Player player) {
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        double currentHealth = player.getHealth();
        double healthPercent = (currentHealth / maxHealth) * 100;

        int newLevel;
        double newMultiplier;

        if (healthPercent > 75) {
            // 100-75% HP
            newLevel = 1;
            newMultiplier = 1.5;
        } else if (healthPercent > 50) {
            // 75-50% HP
            newLevel = 2;
            newMultiplier = 2.0;
        } else if (healthPercent > 25) {
            // 50-25% HP
            newLevel = 3;
            newMultiplier = 3.0;
        } else {
            // 25-0% HP - МАКСИМАЛЬНАЯ ЯРОСТЬ!
            newLevel = 4;
            newMultiplier = 5.0;
        }

        UUID uuid = player.getUniqueId();
        Integer oldLevel = rageLevels.getOrDefault(uuid, 0);

        // Если уровень изменился - показываем уведомление
        if (oldLevel != newLevel) {
            rageLevels.put(uuid, newLevel);
            damageMultipliers.put(uuid, newMultiplier);
            showRageChange(player, newLevel, newMultiplier);
        }

        // Применяем эффекты по уровню ярости
        applyRageEffects(player, newLevel);
    }

    /**
     * Показывает уведомление об изменении ярости
     */
    private void showRageChange(Player player, int level, double multiplier) {
        switch (level) {
            case 1:
                player.sendTitle("", "", 0, 0, 0); // Сброс
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 2f);
                break;
            case 2:
                player.sendTitle("", "", 5, 20, 5);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.7f, 1.5f);
                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 2, 0), 20);
                break;
            case 3:
                player.sendTitle("", "", 5, 30, 10);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 2, 0), 50);
                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 2, 0), 30);
                break;
            case 4: // МАКСИМУМ!
                player.sendTitle("", "", 5, 40, 15);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.5f);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.5f);
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 2, 0), 100);
                player.getWorld().spawnParticle(Particle.LAVA, player.getLocation().add(0, 2, 0), 50);
                // Исправлено: SMOKE_LARGE -> SMOKE
                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 30);
                break;
        }
    }

    /**
     * Применяет эффекты в зависимости от уровня ярости
     */
    private void applyRageEffects(Player player, int level) {
        // Убираем старые эффекты
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.REGENERATION);

        switch (level) {
            case 2:
                // Скорость II
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false));
                break;
            case 3:
                // Скорость III + Регенерация I
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 2, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, false, false));
                break;
            case 4:
                // Скорость IV + Регенерация II
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 3, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 1, false, false));

                // Красные частицы вокруг (эффект горения)
                // Исправлено: DUST -> COLOR_RED или используем DustOptions
                player.getWorld().spawnParticle(Particle.DUST, 
                    player.getLocation().add(0, 1, 0), 
                    10, 
                    0.5, 0.5, 0.5,
                    new Particle.DustOptions(Color.RED, 1f)
                );
                break;
        }
    }

    /**
     * При ударе - наносим ДОПОЛНИТЕЛЬНЫЙ урон на основе ярости
     */
    @Override
    public void onDamageDealt(EntityDamageByEntityEvent event, Player player) {
        UUID uuid = player.getUniqueId();
        double multiplier = damageMultipliers.getOrDefault(uuid, 1.0);

        // Дополнительный урон (процент от базового)
        double baseDamage = event.getDamage();
        double bonusDamage = baseDamage * (multiplier - 1.0); // x1.5 = +50%, x2 = +100%
        event.setDamage(baseDamage + bonusDamage);

        // Эффекты при ударе
        LivingEntity target = null;
        if (event.getEntity() instanceof LivingEntity) {
            target = (LivingEntity) event.getEntity();
        }

        if (target != null) {
            int level = rageLevels.getOrDefault(uuid, 1);

            // Частицы при ударе (зависят от уровня)
            switch (level) {
                case 2:
                    target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0, 1, 0), 20);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 1.5f);
                    break;
                case 3:
                    target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 30);
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 20);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 1f);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1f);
                    break;
                case 4: // МАКСИМУМ!
                    target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 50);
                    target.getWorld().spawnParticle(Particle.LAVA, target.getLocation().add(0, 1, 0), 20);
                    // Исправлено: EXPLOSION_LARGE -> EXPLOSION
                    target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation().add(0, 1, 0), 1);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.5f);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);

                    // Невидимость на 3 сек при ударе (максимальная ярость)
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0, false, false));
                    // Исправлено: SMOKE_LARGE -> SMOKE
                    player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 50);
                    player.sendTitle("", "", 0, 0, 0);
                    break;
            }
        }
    }

    /**
     * При убийстве - исцеление
     */
    @Override
    public void onKill(Player killer, Player victim) {
        // +2 сердца на 10 сек
        double maxHealth = killer.getAttribute(Attribute.MAX_HEALTH).getValue();
        double newHealth = Math.min(killer.getHealth() + 4, maxHealth);
        killer.setHealth(newHealth);

        // Эффекты
        killer.getWorld().spawnParticle(Particle.HEART, killer.getLocation().add(0, 2, 0), 20);
        killer.getWorld().spawnParticle(Particle.FLAME, killer.getLocation().add(0, 1, 0), 30);
        killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        killer.sendTitle("", "", 5, 30, 10);
    }

    /**
     * При смерти - ВЗРЫВ ЯРОСТИ! (урон всем рядом)
     */
    @Override
    public void onDeath(Player player, PlayerDeathEvent event) {
        // Взрыв ярости - урон всем в радиусе 10 блоков
        double explosionRadius = 10.0;
        double explosionDamage = 20.0; // 10 сердец урона

        // Исправлено: правильный синтаксис createExplosion
        // createExplosion(double x, double y, double z, float power, boolean setFire, boolean breakBlocks)
        Location loc = player.getLocation();
        player.getWorld().createExplosion(
            loc.getX(), 
            loc.getY(), 
            loc.getZ(), 
            0f, // Без разрушения блоков
            false, // Без огня
            false // Не взрывать блоки
        );

        // Визуальные эффекты
        // Исправлено: EXPLOSION_HUGE -> EXPLOSION_EMITTER
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
        player.getWorld().spawnParticle(Particle.FLAME, loc, 200);
        player.getWorld().spawnParticle(Particle.LAVA, loc, 100);
        // Исправлено: SMOKE_LARGE -> SMOKE
        player.getWorld().spawnParticle(Particle.SMOKE, loc, 50);
        player.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);
        player.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.5f);

        // Урон всем в радиусе
        for (Entity entity : player.getNearbyEntities(explosionRadius, explosionRadius, explosionRadius)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity living = (LivingEntity) entity;
                living.damage(explosionDamage, player);
                living.setFireTicks(100); // Поджигаем
                living.getWorld().spawnParticle(Particle.FLAME, living.getLocation().add(0, 1, 0), 20);
            }
        }

        // Сообщение всем nearby
        for (Entity entity : player.getNearbyEntities(20, 20, 20)) {
            if (entity instanceof Player && entity != player) {
                ((Player) entity).sendMessage(ChatColor.RED + "💀 " + ChatColor.DARK_RED + player.getName() + " взорвался в ярости!");
            }
        }
    }

    /**
     * При ЛКМ - Крик Ярости (AoE урон + ускорение)
     */
    @Override
    public void onLeftClick(PlayerInteractEvent event, Player player) {
        // AoE урон всем в радиусе 5 блоков
        double radius = 5.0;
        double damage = 8.0;

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity living = (LivingEntity) entity;
                living.damage(damage, player);
                living.setFireTicks(60);

                // Отбрасывание - используем правильный Vector
                org.bukkit.util.Vector knockback = living.getLocation().toVector()
                    .subtract(player.getLocation().toVector())
                    .normalize()
                    .multiply(1.5)
                    .setY(0.5);
                living.setVelocity(knockback);
            }
        }

        // Эффекты
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 100);
        // Исправлено: SMOKE_LARGE -> SMOKE
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 50);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);
        player.sendTitle("", "", 5, 20, 10);
    }
}
