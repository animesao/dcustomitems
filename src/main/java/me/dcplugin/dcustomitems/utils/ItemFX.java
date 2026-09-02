package me.dcplugin.dcustomitems.utils;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.models.CustomItem;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Общие визуальные эффекты (частицы и звуки) для кастомных предметов.
 * Раньше логика дублировалась в PlayerListener и EquippedItemsChecker.
 */
public final class ItemFX {

    private ItemFX() {}

    /**
     * Спавнит частицы и звуки при экипировке/снятии предмета.
     *
     * @param plugin   экземпляр плагина (для логов)
     * @param player   игрок
     * @param item     предмет
     * @param isEquip  true = экипировка (equip-*), false = снятие (unequip-*)
     */
    public static void spawnEquipEffects(Main plugin, Player player, CustomItem item, boolean isEquip) {
        List<String> particles = isEquip ? item.getEquipParticles() : item.getUnequipParticles();
        List<String> sounds = isEquip ? item.getEquipSounds() : item.getUnequipSounds();

        for (String particleStr : particles) {
            try {
                String[] parts = particleStr.split(":");
                Particle particle = EnumCache.getParticle(parts[0]);
                if (particle == null) continue;
                int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 10;
                player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count);
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при спавне частиц: " + particleStr);
            }
        }

        for (String soundStr : sounds) {
            try {
                String[] parts = soundStr.split(":");
                Sound sound = EnumCache.getSound(parts[0]);
                if (sound == null) continue;
                float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
                float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при воспроизведении звука: " + soundStr);
            }
        }
    }
}