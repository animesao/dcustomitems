package me.dcplugin.dcustomitems.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Централизованное кэширование Bukkit enum lookup'ов.
 *
 * Material.valueOf(), Particle.valueOf(), Sound.valueOf() и т.д. используют
 * рефлексию/реестры — кэширование убирает этот оверхед при повторных вызовах.
 *
 * Все карты — ConcurrentHashMap: кэш читается из основного потока сервера,
 * но lookup'ы могут прилететь и из async-пула (БД, апдейт-чекер).
 * ConcurrentHashMap не допускает null-значений, поэтому "не найдено"
 * кэшируется через Sentinel.
 */
public final class EnumCache {

    private EnumCache() {} // utility class

    /** Маркер "значение не найдено" — ConcurrentHashMap не хранит null. */
    private static final Object NULL = new Object();

    // ===== Material =====

    private static final Map<String, Object> MATERIAL_CACHE = new ConcurrentHashMap<>();

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
        clean = clean.replace('-', '_');
        String key = clean.toUpperCase(Locale.ROOT);
        Object cached = MATERIAL_CACHE.computeIfAbsent(key, k -> {
            try {
                return Material.valueOf(k);
            } catch (IllegalArgumentException e) {
                return NULL;
            }
        });
        return cached == NULL ? null : (Material) cached;
    }

    /**
     * Проверить, является ли материал AIR
     */
    public static boolean isAir(Material material) {
        return material == null || material.isAir();
    }

    // ===== Particle =====

    private static final Map<String, Object> PARTICLE_CACHE = new ConcurrentHashMap<>();

    public static Particle getParticle(String name) {
        if (name == null || name.isEmpty()) return null;
        String key = normalize(name);
        Object cached = PARTICLE_CACHE.computeIfAbsent(key, k -> {
            try {
                return Particle.valueOf(k);
            } catch (IllegalArgumentException | IllegalStateException e) {
                return NULL;
            }
        });
        return cached == NULL ? null : (Particle) cached;
    }

    // ===== Sound =====

    private static final Map<String, Object> SOUND_CACHE = new ConcurrentHashMap<>();

    /**
     * Sound-константы помечены как deprecated в Paper 1.21+, но Registry-ключи
     * у звуков другие ("entity.item_break" вместо "ENTITY_ITEM_BREAK"), поэтому
     * valueOf с фолбэком на поле константы остаётся самым надёжным способом.
     */
    @SuppressWarnings({"deprecation", "removal"})
    public static Sound getSound(String name) {
        if (name == null || name.isEmpty()) return null;
        String key = normalize(name);
        Object cached = SOUND_CACHE.computeIfAbsent(key, k -> {
            try {
                return Sound.valueOf(k);
            } catch (Exception | LinkageError e) {
                return NULL;
            }
        });
        return cached == NULL ? null : (Sound) cached;
    }

    // ===== PotionEffectType =====

    private static final Map<String, Object> EFFECT_CACHE = new ConcurrentHashMap<>();

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
        String key = normalize(name);

        ensureEffectAliases();
        Object cached = EFFECT_CACHE.get(key);
        if (cached == NULL) return null;
        if (cached != null) return (PotionEffectType) cached;

        // Попытка через Bukkit API
        PotionEffectType effectType = PotionEffectType.getByName(key);
        if (effectType != null) {
            EFFECT_CACHE.put(key, effectType);
            return effectType;
        }
        EFFECT_CACHE.put(key, NULL);
        return null;
    }

    // ===== Enchantment =====

    private static final Map<String, Object> ENCHANTMENT_CACHE = new ConcurrentHashMap<>();

    public static Enchantment getEnchantment(String name) {
        if (name == null || name.isEmpty()) return null;
        String key = name.toLowerCase(Locale.ROOT).replace('-', '_');

        Object cached = ENCHANTMENT_CACHE.get(key);
        if (cached == NULL) return null;
        if (cached != null) return (Enchantment) cached;

        Enchantment ench = null;
        try {
            ench = Enchantment.getByKey(NamespacedKey.minecraft(key));
        } catch (Exception ignored) {
            // Неизвестное зачарование — возвращаем null
        }
        ENCHANTMENT_CACHE.put(key, ench == null ? NULL : ench);
        return ench;
    }

    // ===== ItemFlag =====

    private static final Map<String, Object> FLAG_CACHE = new ConcurrentHashMap<>();

    public static ItemFlag getItemFlag(String name) {
        if (name == null || name.isEmpty()) return null;
        String key = normalize(name);
        Object cached = FLAG_CACHE.computeIfAbsent(key, k -> {
            try {
                return ItemFlag.valueOf(k);
            } catch (IllegalArgumentException e) {
                return NULL;
            }
        });
        return cached == NULL ? null : (ItemFlag) cached;
    }

    // ===== Attribute =====

    private static final Map<String, Object> ATTRIBUTE_CACHE = new ConcurrentHashMap<>();

    /**
     * Резолвит атрибут по имени.
     *
     * Paper 1.21.3+ больше не знает legacy-имён вида {@code GENERIC_ATTACK_DAMAGE}:
     * {@code Attribute.valueOf} ищет ключ в реестре и бросает
     * IllegalArgumentException. Здесь поддерживаются и legacy-имена
     * (GENERIC_/PLAYER_/ZOMBIE_ префиксы), и современные (attack_damage),
     * и регистровые ключи.
     *
     * @return Attribute или null, если атрибут неизвестен
     */
    @SuppressWarnings({"deprecation", "removal"})
    public static Attribute getAttribute(String name) {
        if (name == null || name.isEmpty()) return null;
        String key = name.trim().toUpperCase(Locale.ROOT).replace('-', '_');

        Object cached = ATTRIBUTE_CACHE.get(key);
        if (cached == NULL) return null;
        if (cached != null) return (Attribute) cached;

        Attribute attribute = resolveAttribute(key);
        ATTRIBUTE_CACHE.put(key, attribute == null ? NULL : attribute);
        return attribute;
    }

    @SuppressWarnings({"deprecation", "removal"})
    private static Attribute resolveAttribute(String upperKey) {
        // 1) Прямое имя (современный вид: ATTACK_DAMAGE / attack_damage)
        Attribute direct = valueOfAttribute(upperKey);
        if (direct != null) return direct;

        // 2) Legacy-префиксы: GENERIC_MAX_HEALTH -> MAX_HEALTH
        for (String prefix : LEGACY_ATTRIBUTE_PREFIXES) {
            if (upperKey.startsWith(prefix) && upperKey.length() > prefix.length()) {
                Attribute stripped = valueOfAttribute(upperKey.substring(prefix.length()));
                if (stripped != null) return stripped;
            }
        }

        // 3) Сопоставление с ключом реестра (minecraft:attack_damage)
        String registryKey = upperKey.toLowerCase(Locale.ROOT);
        for (Attribute attribute : allAttributes()) {
            NamespacedKey key = attribute.getKey();
            if (key != null && registryKey.equals(key.getKey())) {
                return attribute;
            }
        }
        return null;
    }

    private static final String[] LEGACY_ATTRIBUTE_PREFIXES = {
            "GENERIC_", "PLAYER_", "ZOMBIE_", "HORSE_", "LLAMA_", "MINECART_", "BOAT_"
    };

    @SuppressWarnings({"deprecation", "removal"})
    private static Attribute valueOfAttribute(String upperKey) {
        try {
            return Attribute.valueOf(upperKey);
        } catch (Exception | LinkageError e) {
            // Неизвестное имя атрибута. LinkageError — потому что без сервера
            // статический инициализатор Attribute падает с
            // ExceptionInInitializerError.
            return null;
        }
    }

    /**
     * Все атрибуты реестра. В окружении без сервера (юнит-тесты) реестр
     * недоступен — возвращаем пустой массив, а не падаем.
     */
    @SuppressWarnings({"deprecation", "removal"})
    private static Attribute[] allAttributes() {
        try {
            return Attribute.values();
        } catch (Exception | LinkageError e) {
            return new Attribute[0];
        }
    }

    // ===== Хелперы =====

    /** Нормализация имени enum-константы: uppercase + дефисы → подчёркивания. */
    private static String normalize(String name) {
        return name.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

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
        stats.put("attributes", ATTRIBUTE_CACHE.size());
        return stats;
    }
}
