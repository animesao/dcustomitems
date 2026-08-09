package com.example.items;

import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.api.ItemAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

/**
 * Пример посоха телепортации на Java API.
 *
 * ЛКМ - телепортация к рандомному игроку
 * ПКМ - серия телепортаций с анимацией
 */
public class TeleportStaff extends AbstractCustomItem {

    private final Random random = new Random();

    @Override
    public String getId() { return "java_teleport_staff"; }

    @Override
    public String getDisplayName() { return "&d🌀 &fПосох Телепорта"; }

    @Override
    public Material getMaterial() { return Material.BLAZE_ROD; }

    @Override
    public java.util.List<String> getLore() {
        return java.util.List.of(
            "",
            " &7ЛКМ - телепорт к игроку",
            " &7ПКМ - серия телепортаций",
            "",
            " &dТип: &fМагический"
        );
    }

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public boolean isGlowing() { return true; }

    @Override
    public String getItemModel() { return "teleport_staff"; }

    @Override
    public long getClickCooldown() { return 2000; }

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        // ПКМ - серия телепортаций с анимацией
        ItemAPI.effect(player, PotionEffectType.SPEED, 5, 2);

        // 5 телепортаций с задержкой
        for (int i = 0; i < 5; i++) {
            final int step = i;
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugins()[0],
                () -> {
                    // Телепортируем случайно
                    Location loc = player.getLocation().add(
                        random.nextDouble() * 20 - 10,
                        0,
                        random.nextDouble() * 20 - 10
                    );
                    loc.setY(player.getWorld().getHighestBlockYAt(loc) + 1);

                    // Эффекты
                    ItemAPI.particlesAt(player.getLocation(), Particle.PORTAL, 30);
                    ItemAPI.soundAt(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

                    player.teleport(loc);

                    ItemAPI.particlesAt(loc, Particle.PORTAL, 30);
                    ItemAPI.title(player, "&d🌀 Шаг " + (step + 1) + "/5", "");
                },
                i * 10L // Задержка 0.5 сек между шагами
            );
        }
    }

    @Override
    public void onLeftClick(PlayerInteractEvent event, Player player) {
        // ЛКМ - телепорт к ближайшему игроку
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other == player) continue;
            double dist = player.getLocation().distance(other.getLocation());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = other;
            }
        }

        if (nearest != null) {
            // Эффект исчезновения
            ItemAPI.particlesAt(player.getLocation(), Particle.PORTAL, 50);
            ItemAPI.sound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

            // Телепорт
            player.teleport(nearest.getLocation().add(0, 2, 0));

            // Эффект появления
            ItemAPI.particlesAt(player.getLocation(), Particle.PORTAL, 50);
            ItemAPI.message(player, "&d🌀 Телепортирован к &f" + nearest.getName());
            ItemAPI.title(player, "&d🌀 ТЕЛЕПОРТ!", "&7К &f" + nearest.getName());
        } else {
            ItemAPI.message(player, "&cНет игроков рядом!");
        }
    }
}
