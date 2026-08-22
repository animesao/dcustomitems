package me.dcplugin.dcustomitems.managers;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.models.CustomItem;
import me.dcplugin.dcustomitems.utils.EnumCache;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public EffectManager(Main plugin) {
        this.plugin = plugin;
        this.playerAppliedEffects = new HashMap<>();
    }

    // ===== Parsing =====

    public Map<PotionEffectType, Integer> parseEffects(List<String> effectStrings) {
        Map<PotionEffectType, Integer> parsedEffects = new HashMap<>();

        for (String effectString : effectStrings) {
            try {
                String[] parts = effectString.split(":");
                if (parts.length >= 2) {
                    String effectName = parts[0].toUpperCase();

                    PotionEffectType effectType = EnumCache.getPotionEffect(effectName);

                    if (effectType != null) {
                        int amplifier = Integer.parseInt(parts[1]);
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
        if (item.getParsedEffects() == null || item.getParsedEffects().isEmpty()) return;

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
        if (item.getParsedEffects() == null || item.getParsedEffects().isEmpty()) return;

        for (PotionEffectType effectType : item.getParsedEffects().keySet()) {
            player.removePotionEffect(effectType);
        }
    }

    // ===== Map-based (для EquippedItemsChecker) =====

    /**
     * Добавить эффекты предмета в карту для суммирования
     */
    public void addEffectsToMap(Map<PotionEffectType, Integer> effectsMap, CustomItem item) {
        if (item.getParsedEffects() != null) {
            for (Map.Entry<PotionEffectType, Integer> entry : item.getParsedEffects().entrySet()) {
                effectsMap.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
    }

    /**
     * Добавить эффекты сет-бонуса в карту
     */
    public void addSetBonusEffectsToMap(Map<PotionEffectType, Integer> effectsMap, String setId) {
        List<String> effectStrings = plugin.getConfig().getStringList("set-bonuses." + setId + ".effects");

        if (effectStrings == null || effectStrings.isEmpty()) {
            plugin.getLogger().warning("Сет-бонусы для '" + setId + "' не найдены в конфиге!");
            return;
        }

        Map<PotionEffectType, Integer> setBonusEffects = parseEffects(effectStrings);

        for (Map.Entry<PotionEffectType, Integer> entry : setBonusEffects.entrySet()) {
            effectsMap.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    /**
     * Применить суммарные эффекты из карты к игроку
     */
    public void applyEffectsFromMap(Player player, Map<PotionEffectType, Integer> effectsMap) {
        UUID playerId = player.getUniqueId();

        // Сохраняем для удаления
        playerAppliedEffects.put(playerId, new HashMap<>(effectsMap));

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
     * Остановить периодические эффекты для игрока (совместимость).
     * В новой архитектуре периодические таски не используются — эффекты
     * обновляются через глобальный EquippedItemsChecker.
     */
    public void stopPeriodicEffects(Player player) {
        // В новой архитектуре ничего не нужно — эффекты управляются через
        // applyEffectsFromMap / removeAllEffectsFromPlayer.
        // Метод оставлен для обратной совместимости.
    }

    /**
     * Остановить все эффекты (при отключении плагина)
     */
    public void stopAllEffects() {
        playerAppliedEffects.clear();
    }

    /**
     * Получить карту применённых эффектов от кастомных предметов для игрока
     */
    public Map<PotionEffectType, Integer> getAppliedEffects(Player player) {
        return playerAppliedEffects.getOrDefault(player.getUniqueId(), new HashMap<>());
    }
}
