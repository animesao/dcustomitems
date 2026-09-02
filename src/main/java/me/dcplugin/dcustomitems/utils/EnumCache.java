package me.dcplugin.dcustomitems.utils;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

/**
 * Централизованное кэширование Bukkit enum lookup'ов.
 *
 * Material.valueOf(), Particle.valueOf(), Sound.valueOf() и т.д. используют
 * рефлексию内部 — кэширование убирает этот оверхед при повторных вызовах.
 *
 * Все методы thread-safe (HashMap lookup для enum-констант безопасен).
 */
public final class EnumCache {

    private EnumCache() {} // utility class

    // ===== Material =====

    private static final Map<String, Material> MATERIAL_CACHE = new HashMap<>();

    /**
     * Получить Material по имени (безопасно, кэшировано)
     *
     * @return Material или null если не найден
     */
    public static Material getMaterial(String name) {
        if (name == null || name.isEmpty()) return null;
        // Убираем namespace prefix: minecraft:diamond_sword -> DIAMOND_SWORD
        String clean = name;
        if (clean.contains(":")) {
            clean = clean.substring(clean.indexOf(':') + 1);
        }
        // Заменяем дефисы на подчёркивания: diamond-sword -> DIAMOND_SWORD
        clean = clean.replace("-", "_");
        String key = clean.toUpperCase();
        return MATERIAL_CACHE.computeIfAbsent(key, k -> {
            try {
                return Material.valueOf(k);
            } catch (IllegalArgumentException e) {
                return null;
            }
        });
    }

    /**
     * Проверить, является ли материал AIR
     */
    public static boolean isAir(Material material) {
        return material == null || material.isAir();
    }

    // ===== Particle =====

    private static final Map<String, Particle> PARTICLE_CACHE = new HashMap<>();

    public static Particle getParticle(String name) {
        if (name == null || name.isEmpty()) return null;
        String key = name.toUpperCase();
        return PARTICLE_CACHE.computeIfAbsent(key, k -> {
            try {
                return Particle.valueOf(k);
            } catch (IllegalArgumentException e) {
                return null;
            }
        });
    }

    // ===== Sound =====

    private static final Map<String, Sound> SOUND_CACHE = new HashMap<>();

    public static Sound getSound(String name) {
        if (name == null || name.isEmpty()) return null;
        String key = name.toUpperCase();
        return SOUND_CACHE.computeIfAbsent(key, k -> {
            try {
                return Sound.valueOf(k);
            } catch (IllegalArgumentException e) {
                return null;
            }
        });
    }

    // ===== PotionEffectType =====

    private static final Map<String, PotionEffectType> EFFECT_CACHE = new HashMap<>();

    /**
     * Ленивая инициализация алиасов: статические ссылки на PotionEffectType
     * требуют инициализированного Bukkit и ломали бы загрузку класса
     * в чистом окружении (юнит-тесты без сервера).
     */
    private static void ensureEffectAliases() {
        if (!EFFECT_CACHE.isEmpty()) return;
        EFFECT_CACHE.put("INCREASE_DAMAGE", PotionEffectType.STRENGTH);
        EFFECT_CACHE.put("DAMAGE_RESISTANCE", PotionEffectType.RESISTANCE);
    }

    public static PotionEffectType getPotionEffect(String name) {
        if (name == null || name.isEmpty()) return null;
        String key = name.toUpperCase();

        ensureEffectAliases();
        PotionEffectType cached = EFFECT_CACHE.get(key);
        if (cached != null) return cached;

        // Попытка через Bukkit API
        PotionEffectType effectType = PotionEffectType.getByName(key);
        if (effectType != null) {
            EFFECT_CACHE.put(key, effectType);
        }
        return effectType;
    }

    // ===== Enchantment =====

    private static final Map<String, Enchantment> ENCHANTMENT_CACHE = new HashMap<>();

    public static Enchantment getEnchantment(String name) {
        if (name == null || name.isEmpty()) return null;
        String key = name.toLowerCase();

        Enchantment cached = ENCHANTMENT_CACHE.get(key);
        if (cached != null) return cached;

        try {
            Enchantment ench = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(key));
            if (ench != null) {
                ENCHANTMENT_CACHE.put(key, ench);
            }
            return ench;
        } catch (Exception e) {
            return null;
        }
    }

    // ===== ItemFlag =====

    private static final Map<String, ItemFlag> FLAG_CACHE = new HashMap<>();

    public static ItemFlag getItemFlag(String name) {
        if (name == null || name.isEmpty()) return null;
        String key = name.toUpperCase();
        return FLAG_CACHE.computeIfAbsent(key, k -> {
            try {
                return ItemFlag.valueOf(k);
            } catch (IllegalArgumentException e) {
                return null;
            }
        });
    }

    // ===== Stats =====

    /**
     * Размеры кэшей (для отладки/логирования)
     */
    public static Map<String, Integer> getCacheStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("materials", MATERIAL_CACHE.size());
        stats.put("particles", PARTICLE_CACHE.size());
        stats.put("sounds", SOUND_CACHE.size());
        stats.put("effects", EFFECT_CACHE.size());
        stats.put("enchantments", ENCHANTMENT_CACHE.size());
        stats.put("flags", FLAG_CACHE.size());
        return stats;
    }
}
