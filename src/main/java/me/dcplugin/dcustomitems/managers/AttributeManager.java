package me.dcplugin.dcustomitems.managers;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.models.CustomItem;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AttributeManager {

    private final Main plugin;
    private final Map<UUID, Map<Attribute, AttributeModifier>> playerAppliedAttributes;

    public AttributeManager(Main plugin) {
        this.plugin = plugin;
        this.playerAppliedAttributes = new HashMap<>();
    }

    /**
     * Применяет атрибуты от кастомного предмета к игроку
     */
    public void applyAttributes(Player player, CustomItem item, String slotType) {
        if (item.getAttributes() == null || item.getAttributes().isEmpty()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        Map<Attribute, AttributeModifier> appliedModifiers = playerAppliedAttributes.computeIfAbsent(playerId, k -> new HashMap<>());

        for (Map.Entry<String, Double> entry : item.getAttributes().entrySet()) {
            try {
                Attribute attribute = Attribute.valueOf(entry.getKey().toUpperCase());
                double value = entry.getValue();

                // Создаем уникальный модификатор для этого предмета и слота
                String modifierName = "custom_" + item.getId() + "_" + slotType + "_" + attribute.name().toLowerCase();
                AttributeModifier modifier = new AttributeModifier(
                    org.bukkit.NamespacedKey.fromString(modifierName),
                    value,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.ANY
                );

                AttributeInstance attributeInstance = player.getAttribute(attribute);
                if (attributeInstance != null) {
                    // Удаляем старый модификатор если есть
                    AttributeModifier oldModifier = appliedModifiers.get(attribute);
                    if (oldModifier != null) {
                        attributeInstance.removeModifier(oldModifier);
                    }

                    // Применяем новый модификатор
                    attributeInstance.addModifier(modifier);
                    appliedModifiers.put(attribute, modifier);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Неизвестный атрибут: " + entry.getKey() + " для предмета: " + item.getId());
            }
        }
    }

    /**
     * Удаляет все атрибуты от кастомных предметов у игрока
     */
    public void removeAllAttributes(Player player) {
        UUID playerId = player.getUniqueId();
        Map<Attribute, AttributeModifier> appliedModifiers = playerAppliedAttributes.remove(playerId);

        if (appliedModifiers != null) {
            for (Map.Entry<Attribute, AttributeModifier> entry : appliedModifiers.entrySet()) {
                AttributeInstance attributeInstance = player.getAttribute(entry.getKey());
                if (attributeInstance != null) {
                    try {
                        attributeInstance.removeModifier(entry.getValue());
                    } catch (Exception e) {
                        // Модификатор уже удален или не существует, игнорируем
                    }
                }
            }
        }
        
        // Дополнительная очистка - удаляем все модификаторы с нашими именами
        for (Attribute attribute : Attribute.values()) {
            try {
                AttributeInstance attributeInstance = player.getAttribute(attribute);
                if (attributeInstance != null) {
                    // Удаляем все модификаторы, начинающиеся с "custom_"
                    attributeInstance.getModifiers().stream()
                        .filter(modifier -> modifier.getKey().toString().startsWith("custom_"))
                        .forEach(modifier -> {
                            try {
                                attributeInstance.removeModifier(modifier);
                            } catch (Exception ignored) {
                                // Модификатор уже удален
                            }
                        });
                }
            } catch (Exception ignored) {
                // Атрибут не поддерживается для этой сущности
            }
        }
    }

    /**
     * Применяет атрибуты из карты (для суммирования атрибутов от нескольких предметов)
     */
    public void applyAttributesFromMap(Player player, Map<Attribute, Double> attributesMap) {
        UUID playerId = player.getUniqueId();
        
        // Сначала удаляем все старые атрибуты
        removeAllAttributes(player);
        
        if (attributesMap.isEmpty()) {
            return;
        }
        
        Map<Attribute, AttributeModifier> appliedModifiers = playerAppliedAttributes.computeIfAbsent(playerId, k -> new HashMap<>());

        for (Map.Entry<Attribute, Double> entry : attributesMap.entrySet()) {
            Attribute attribute = entry.getKey();
            double value = entry.getValue();

            try {
                // Создаем уникальный модификатор для суммарного значения
                String modifierName = "custom_total_" + attribute.name().toLowerCase() + "_" + System.currentTimeMillis();
                AttributeModifier modifier = new AttributeModifier(
                    org.bukkit.NamespacedKey.fromString(modifierName),
                    value,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.ANY
                );

                AttributeInstance attributeInstance = player.getAttribute(attribute);
                if (attributeInstance != null) {
                    // Проверяем, что модификатор еще не применен
                    boolean alreadyApplied = attributeInstance.getModifiers().stream()
                        .anyMatch(mod -> mod.getKey().toString().startsWith("custom_total_" + attribute.name().toLowerCase()));
                    
                    if (!alreadyApplied) {
                        attributeInstance.addModifier(modifier);
                        appliedModifiers.put(attribute, modifier);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "Ошибка при применении атрибута " + attribute.name() + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Добавляет атрибуты предмета в карту для последующего суммирования
     */
    public void addAttributesToMap(Map<Attribute, Double> attributesMap, CustomItem item) {
        if (item.getAttributes() == null || item.getAttributes().isEmpty()) {
            return;
        }

        for (Map.Entry<String, Double> entry : item.getAttributes().entrySet()) {
            try {
                Attribute attribute = Attribute.valueOf(entry.getKey().toUpperCase());
                double value = entry.getValue();
                
                // Суммируем атрибуты от разных предметов
                attributesMap.merge(attribute, value, Double::sum);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Неизвестный атрибут: " + entry.getKey() + " для предмета: " + item.getId());
            }
        }
    }

    /**
     * Очищает все данные при отключении плагина
     */
    public void cleanup() {
        playerAppliedAttributes.clear();
    }
}