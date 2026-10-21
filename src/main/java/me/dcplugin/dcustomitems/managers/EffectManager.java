package me.dcplugin.dcustomitems.managers;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.models.CustomItem;
import me.dcplugin.dcustomitems.utils.EnumCache;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Менеджер эффектов от кастомных предметов.
 *
 * Использует map-based подход: эффекты применяются через applyEffectsFromMap(),
 * который вызывается из глобального EquippedItemsChecker (1 таск на сервер).
 *
 * Мёртвый код (per-player BukkitRunnable) удалён — startPeriodicEffects()
 * никогда не вызывался извне.
 */
public class EffectManager {

    private final Main plugin;

    // Отслеживаем какие эффекты применены к каждому игроку (для удаления)
    private final Map<UUID, Map<PotionEffectType, Integer>> playerAppliedEffects;

    // Длительность эффектов: 60 секунд (в тиках)
    // EquippedItemsChecker обновляет каждые 150ms, поэтому 60s достаточно
    private static final int EFFECT_DURATION_TICKS = 60 * 20;

    // Кэш разобранных сет-бонусов: setId -> (effectType -> amplifier).
    // Пересчитывается только при reload конфига, а не на каждый пересчёт
    // экипировки (это было горячим путём — recalculateEquipment вызывается
    // при каждом изменении инвентаря игрока).
    private final Map<String, Map<PotionEffectType, Integer>> setBonusCache = new HashMap<>();

    public EffectManager(Main plugin) {
        this.plugin = plugin;
        this.playerAppliedEffects = new HashMap<>();
    }

    // ===== Parsing =====

    public Map<PotionEffectType, Integer> parseEffects(List<String> effectStrings) {
        Map<PotionEffectType, Integer> parsedEffects = new HashMap<>();

        if (effectStrings == null) return parsedEffects;

        for (String effectString : effectStrings) {
            if (effectString == null) continue;
            try {
                String[] parts = effectString.split(":");
                if (parts.length >= 2) {
                    String effectName = parts[0].toUpperCase(java.util.Locale.ROOT);

                    PotionEffectType effectType = EnumCache.getPotionEffect(effectName);

                    if (effectType != null) {
                        int amplifier = Integer.parseInt(parts[1].trim());
                        parsedEffects.put(effectType, amplifier);
                    } else {
                        plugin.getLogger().warning("Неизвестный эффект: " + effectName);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при парсинге эффекта: " + effectString);
            }
        }

        return parsedEffects;
    }

    // ===== Apply / Remove (one-shot) =====

    /**
     * Применить эффекты от предмета (разово, например CONSUMABLE)
     */
    public void applyEffects(Player player, CustomItem item) {
        if (item == null || item.getParsedEffects() == null || item.getParsedEffects().isEmpty()) return;

        for (Map.Entry<PotionEffectType, Integer> entry : item.getParsedEffects().entrySet()) {
            player.addPotionEffect(new PotionEffect(
                    entry.getKey(), EFFECT_DURATION_TICKS, entry.getValue() - 1, false, false
            ), true);
        }
    }

    /**
     * Удалить эффекты от конкретного предмета
     */
    public void removeEffects(Player player, CustomItem item) {
        if (item == null || item.getParsedEffects() == null || item.getParsedEffects().isEmpty()) return;

        for (PotionEffectType effectType : item.getParsedEffects().keySet()) {
            player.removePotionEffect(effectType);
        }
    }

    // ===== Map-based (для EquippedItemsChecker) =====

    /**
     * Добавить эффекты предмета в карту для суммирования.
     *
     * Уровень эффекта НЕ суммируется: два предмета с эффектом 1 и 2 дают
     * уровень 2 (максимум), а не 3. Раньше здесь был Integer::sum, из-за
     * чего экипировка двух предметов неожиданно усиливала эффект.
     */
    public void addEffectsToMap(Map<PotionEffectType, Integer> effectsMap, CustomItem item) {
        if (item == null || item.getParsedEffects() == null) return;
        for (Map.Entry<PotionEffectType, Integer> entry : item.getParsedEffects().entrySet()) {
            effectsMap.merge(entry.getKey(), entry.getValue(), Math::max);
        }
    }

    /**
     * Добавить эффекты сет-бонуса в карту
     */
    public void addSetBonusEffectsToMap(Map<PotionEffectType, Integer> effectsMap, String setId) {
        if (setId == null) return;

        Map<PotionEffectType, Integer> setBonusEffects = getSetBonusEffects(setId);
        if (setBonusEffects.isEmpty()) {
            // Предупреждение один раз на сет: пересчёт экипировки happens
            // при каждом изменении инвентаря, иначе лог засорялся бы.
            if (!warnedMissingSets.add(setId)) return;
            plugin.getLogger().warning("Сет-бонусы для '" + setId + "' не найдены в конфиге!");
            return;
        }

        for (Map.Entry<PotionEffectType, Integer> entry : setBonusEffects.entrySet()) {
            effectsMap.merge(entry.getKey(), entry.getValue(), Math::max);
        }
    }

    /** Сеты, о которых уже выводилось предупреждение. */
    private final Set<String> warnedMissingSets = new HashSet<>();

    /**
     * Разобранные эффекты сет-бонуса (из кэша).
     */
    public Map<PotionEffectType, Integer> getSetBonusEffects(String setId) {
        if (setId == null) return Map.of();
        Map<PotionEffectType, Integer> cached = setBonusCache.get(setId);
        if (cached != null) return cached;

        List<String> effectStrings = plugin.getConfig().getStringList("set-bonuses." + setId + ".effects");
        Map<PotionEffectType, Integer> parsed = effectStrings == null || effectStrings.isEmpty()
                ? Map.of()
                : Map.copyOf(parseEffects(effectStrings));
        setBonusCache.put(setId, parsed);
        return parsed;
    }

    /**
     * Сбросить кэш сет-бонусов (после reload конфига).
     */
    public void invalidateSetBonusCache() {
        setBonusCache.clear();
        warnedMissingSets.clear();
    }

    /**
     * Применить суммарные эффекты из карты к игроку
     */
    public void applyEffectsFromMap(Player player, Map<PotionEffectType, Integer> effectsMap) {
        UUID playerId = player.getUniqueId();

        // Сохраняем для удаления (immutable — внешние вызовы не должны менять состояние)
        playerAppliedEffects.put(playerId, java.util.Collections.unmodifiableMap(new HashMap<>(effectsMap)));

        // Применяем
        for (Map.Entry<PotionEffectType, Integer> entry : effectsMap.entrySet()) {
            player.addPotionEffect(new PotionEffect(
                    entry.getKey(),
                    EFFECT_DURATION_TICKS,
                    entry.getValue() - 1,
                    false,
                    false
            ), true);
        }
    }

    /**
     * Удалить все ранее применённые эффекты от кастомных предметов
     */
    public void removeAllEffectsFromPlayer(Player player) {
        if (player == null) return;
        UUID playerId = player.getUniqueId();

        Map<PotionEffectType, Integer> appliedEffects = playerAppliedEffects.remove(playerId);
        if (appliedEffects != null) {
            for (PotionEffectType effectType : appliedEffects.keySet()) {
                player.removePotionEffect(effectType);
            }
        }
    }

    // ===== Compatibility (вызывается из PlayerListener.onPlayerQuit и PluginBootstrap.disable) =====

    /**
     * Снять эффекты кастомных предметов с игрока и забыть его состояние.
     *
     * Метод был заглушкой: эффекты оставались на игроке до конца своей
     * длительности (60 с), а запись в playerAppliedEffects оставалась в памяти
     * — это была утечка на каждого вышедшего игрока.
     */
    public void stopPeriodicEffects(Player player) {
        removeAllEffectsFromPlayer(player);
    }

    /**
     * Остановить все эффекты (при отключении плагина)
     */
    public void stopAllEffects() {
        playerAppliedEffects.clear();
    }

    /**
     * Получить карту применённых эффектов от кастомных предметов для игрока.
     *
     * Возвращается копия: внешний код не должен менять внутреннее состояние
     * (иначе рассинхронизация с removeAllEffectsFromPlayer).
     */
    public Map<PotionEffectType, Integer> getAppliedEffects(Player player) {
        if (player == null) return new HashMap<>();
        Map<PotionEffectType, Integer> effects = playerAppliedEffects.get(player.getUniqueId());
        return effects == null ? new HashMap<>() : new HashMap<>(effects);
    }
}
