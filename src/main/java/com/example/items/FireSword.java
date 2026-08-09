package com.example.items;

import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Пример огненного меча на Java API.
 *
 * ЛКМ - поджигает цель
 * ПКМ - поджигает всех рядом
 * При убийстве - исцеление
 */
public class FireSword extends AbstractCustomItem {

    @Override
    public String getId() { return "java_fire_sword"; }

    @Override
    public String getDisplayName() { return "&c🔥 &fОгненный Клинок"; }

    @Override
    public Material getMaterial() { return Material.NETHERITE_SWORD; }

    @Override
    public java.util.List<String> getLore() {
        return java.util.List.of(
            "",
            " &7ЛКМ - поджечь цель",
            " &7ПКМ - огненный взрыв",
            "",
            " &cУрон: +8",
            " &cШанс крита: 25%"
        );
    }

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public boolean isGlowing() { return true; }

    @Override
    public String getItemModel() { return "fire_sword"; }

    @Override
    public long getClickCooldown() { return 500; }

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        // ПКМ - огненный взрыв вокруг
        ItemAPI.setOnFire(player, 0); // Гасим себя
        ItemAPI.setNearbyOnFire(player, 60, 4); // Поджигаем nearby на 3 сек
        ItemAPI.particles(player, Particle.FLAME, 100);
        ItemAPI.sound(player, Sound.ENTITY_BLAZE_SHOOT, 2f, 0.5f);
        ItemAPI.title(player, "&c🔥 ОГНЕННЫЙ ВЗРЫВ!", "");
    }

    @Override
    public void onLeftClick(PlayerInteractEvent event, Player player) {
        // ЛКМ - поджигаем цель (куда смотрим)
        ItemAPI.lightningForward(player, 10); // Молния далеко вперед
        ItemAPI.particles(player, Particle.FLAME, 50);
        ItemAPI.sound(player, Sound.ITEM_TRIDENT_THUNDER, 2f, 1.5f);
    }

    @Override
    public void onDamageDealt(EntityDamageByEntityEvent event, Player player) {
        // +8 урона при ударе
        event.setDamage(event.getDamage() + 8);

        // 25% шанс крита
        if (Math.random() < 0.25) {
            event.setDamage(event.getDamage() * 2);
            ItemAPI.title(player, "&c💥 КРИТИЧЕСКИЙ УДАР!", "");
            ItemAPI.sound(player, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
        }

        // Поджигаем цель
        if (event.getEntity() instanceof Player) {
            ((Player) event.getEntity()).setFireTicks(60);
        }

        ItemAPI.particlesAt(event.getEntity().getLocation(), Particle.FLAME, 20);
    }

    @Override
    public void onKill(Player killer, Player victim) {
        // При убийстве - исцеление + эффекты
        ItemAPI.heal(killer, 5); // +5 сердец
        ItemAPI.effect(killer, org.bukkit.potion.PotionEffectType.STRENGTH, 10, 2);
        ItemAPI.particles(killer, Particle.HEART, 50);
        ItemAPI.sound(killer, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        ItemAPI.title(killer, "&c⚔ УБИЙСТВО!", "&7+5 ❤ &c+Сила II");
    }
}
