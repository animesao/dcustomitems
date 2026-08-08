package me.dcplugin.dcustomitems.managers;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EffectManager {

    private final Main plugin;
    private final Map<UUID, BukkitRunnable> activeEffects;
    private final Map<UUID, CustomItem> playerActiveItems;
    // Отслеживаем какие эффекты применены к каждому игроку от брони/предметов
    private final Map<UUID, Map<PotionEffectType, Integer>> playerAppliedEffects;

    public EffectManager(Main plugin) {
        this.plugin = plugin;
        this.activeEffects = new HashMap<>();
        this.playerActiveItems = new HashMap<>();
        this.playerAppliedEffects = new HashMap<>();
    }

    public Map<PotionEffectType, Integer> parseEffects(List<String> effectStrings) {
        Map<PotionEffectType, Integer> parsedEffects = new HashMap<>();

        for (String effectString : effectStrings) {
            try {
                String[] parts = effectString.split(":");
                if (parts.length >= 2) {
                    String effectName = parts[0].toUpperCase();

                    // Обрабатываем кастомные эффекты
                    PotionEffectType effectType = null;
                    if ("INCREASE_DAMAGE".equals(effectName)) {
                        effectType = PotionEffectType.STRENGTH;
                    } else if ("DAMAGE_RESISTANCE".equals(effectName)) {
                        effectType = PotionEffectType.RESISTANCE;
                    } else {
                        effectType = PotionEffectType.getByName(effectName);
                    }

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

    public void applyEffects(Player player, CustomItem item) {
        if (item.getParsedEffects() == null || item.getParsedEffects().isEmpty()) {
            return;
        }

        for (Map.Entry<PotionEffectType, Integer> entry : item.getParsedEffects().entrySet()) {
            PotionEffectType effectType = entry.getKey();
            int level = entry.getValue();

            // Применяем эффект с указанным уровнем
            player.addPotionEffect(new PotionEffect(effectType, Integer.MAX_VALUE, level - 1, false, false), true);
        }
    }

    public void addEffectsToMap(Map<PotionEffectType, Integer> effectsMap, CustomItem item) {
        if (item.getParsedEffects() != null) {
            for (Map.Entry<PotionEffectType, Integer> entry : item.getParsedEffects().entrySet()) {
                // СКЛАДЫВАЕМ эффекты от разных частей брони
                effectsMap.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
    }

    public void addSetBonusEffectsToMap(Map<PotionEffectType, Integer> effectsMap, String setId) {
        List<String> effectStrings = plugin.getConfig().getStringList("set-bonuses." + setId + ".effects");
        
        if (effectStrings == null || effectStrings.isEmpty()) {
            plugin.getLogger().warning("Сет-бонусы для '" + setId + "' не найдены в конфиге!");
            return;
        }

        Map<PotionEffectType, Integer> setBonusEffects = parseEffects(effectStrings);
        
        for (Map.Entry<PotionEffectType, Integer> entry : setBonusEffects.entrySet()) {
            // СКЛАДЫВАЕМ эффекты от сета с уже существующими эффектами от частей брони
            effectsMap.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    public void applyEffectsFromMap(Player player, Map<PotionEffectType, Integer> effectsMap) {
        UUID playerId = player.getUniqueId();
        
        // Сохраняем информацию о применённых эффектах
        playerAppliedEffects.put(playerId, new HashMap<>(effectsMap));
        
        // Применяем эффекты
        for (Map.Entry<PotionEffectType, Integer> entry : effectsMap.entrySet()) {
            player.addPotionEffect(new PotionEffect(
                entry.getKey(),
                Integer.MAX_VALUE,
                entry.getValue() - 1,
                false,
                false
            ), true);
        }
    }

    public void removeEffects(Player player, CustomItem item) {
        if (item.getParsedEffects() == null || item.getParsedEffects().isEmpty()) {
            return;
        }

        for (PotionEffectType effectType : item.getParsedEffects().keySet()) {
            player.removePotionEffect(effectType);
        }
    }

    public void removeAllEffectsFromPlayer(Player player) {
        UUID playerId = player.getUniqueId();

        // Останавливаем периодические эффекты
        stopPeriodicEffects(player);

        // Удаляем все ранее применённые эффекты от кастомных предметов
        Map<PotionEffectType, Integer> appliedEffects = playerAppliedEffects.remove(playerId);
        if (appliedEffects != null) {
            for (PotionEffectType effectType : appliedEffects.keySet()) {
                player.removePotionEffect(effectType);
            }
        }
    }

    public void startPeriodicEffects(Player player, CustomItem item) {
        UUID playerId = player.getUniqueId();

        // Останавливаем предыдущие эффекты
        stopPeriodicEffects(player);

        // Сохраняем текущий активный предмет
        playerActiveItems.put(playerId, item);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    activeEffects.remove(playerId);
                    playerActiveItems.remove(playerId);
                    return;
                }

                applyEffects(player, item);
            }
        };

        task.runTaskTimer(plugin, 0L, 20L); // Каждую секунду
        activeEffects.put(playerId, task);
    }

    public void stopPeriodicEffects(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitRunnable task = activeEffects.remove(playerId);
        if (task != null) {
            task.cancel();
        }

        // Удаляем эффекты от предыдущего предмета
        CustomItem previousItem = playerActiveItems.remove(playerId);
        if (previousItem != null) {
            removeEffects(player, previousItem);
        }
    }

    public void stopAllEffects() {
        for (BukkitRunnable task : activeEffects.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        activeEffects.clear();
        playerActiveItems.clear();
        playerAppliedEffects.clear();
    }
}