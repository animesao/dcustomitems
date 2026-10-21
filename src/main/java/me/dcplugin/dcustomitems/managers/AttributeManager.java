package me.dcplugin.dcustomitems.managers;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.models.CustomItem;
import me.dcplugin.dcustomitems.utils.EnumCache;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

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
        if (item == null || item.getAttributes() == null || item.getAttributes().isEmpty()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        Map<Attribute, AttributeModifier> appliedModifiers = playerAppliedAttributes.computeIfAbsent(playerId, k -> new HashMap<>());

        for (Map.Entry<String, Double> entry : item.getAttributes().entrySet()) {
            Attribute attribute = EnumCache.getAttribute(entry.getKey());
            if (attribute == null) {
                plugin.getLogger().warning("Неизвестный атрибут: " + entry.getKey() + " для предмета: " + item.getId());
                continue;
            }
            double value = entry.getValue();

            // Создаем уникальный модификатор для этого предмета и слота
            String modifierName = "custom_" + item.getId() + "_" + slotType + "_" + attributeKey(attribute);
            AttributeModifier modifier = new AttributeModifier(
                    modifierKey(modifierName),
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
        }
    }

    /**
     * Удаляет все атрибуты от кастомных предметов у игрока
     */
    public void removeAllAttributes(Player player) {
        if (player == null) return;
        UUID playerId = player.getUniqueId();
        Map<Attribute, AttributeModifier> appliedModifiers = playerAppliedAttributes.remove(playerId);

        if (appliedModifiers != null) {
            for (Map.Entry<Attribute, AttributeModifier> entry : appliedModifiers.entrySet()) {
                AttributeInstance attributeInstance = player.getAttribute(entry.getKey());
                if (attributeInstance != null) {
                    try {
                        attributeInstance.removeModifier(entry.getValue());
                    } catch (Exception ignored) {
                        // Модификатор уже удален или не существует
                    }
                }
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

        if (attributesMap == null || attributesMap.isEmpty()) {
            return;
        }

        Map<Attribute, AttributeModifier> appliedModifiers = playerAppliedAttributes.computeIfAbsent(playerId, k -> new HashMap<>());

        for (Map.Entry<Attribute, Double> entry : attributesMap.entrySet()) {
            Attribute attribute = entry.getKey();
            double value = entry.getValue();

            try {
                // Стабильный модификатор (без timestamp для корректного удаления)
                String modifierName = "custom_total_" + attributeKey(attribute);
                AttributeModifier modifier = new AttributeModifier(
                        modifierKey(modifierName),
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
                    attributeInstance.addModifier(modifier);
                    appliedModifiers.put(attribute, modifier);
                }
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "Ошибка при применении атрибута " + attributeKey(attribute) + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Добавляет атрибуты предмета в карту для последующего суммирования
     */
    public void addAttributesToMap(Map<Attribute, Double> attributesMap, CustomItem item) {
        if (item == null || item.getAttributes() == null || item.getAttributes().isEmpty()) {
            return;
        }

        for (Map.Entry<String, Double> entry : item.getAttributes().entrySet()) {
            Attribute attribute = EnumCache.getAttribute(entry.getKey());
            if (attribute == null) {
                plugin.getLogger().warning("Неизвестный атрибут: " + entry.getKey() + " для предмета: " + item.getId());
                continue;
            }
            // Суммируем атрибуты от разных предметов
            attributesMap.merge(attribute, entry.getValue(), Double::sum);
        }
    }

    /**
     * Очищает все данные при отключении плагина
     */
    public void cleanup() {
        playerAppliedAttributes.clear();
    }

    // ===== Хелперы =====

    /**
     * Ключ реестра атрибута (например {@code attack_damage}).
     * Используется в имени модификатора — без него ключ был бы
     * {@code minecraft:attack_damage}, т.е. в namespace ванильного Minecraft.
     */
    private static String attributeKey(Attribute attribute) {
        NamespacedKey key = attribute.getKey();
        return key != null ? key.getKey() : "attribute";
    }

    /**
     * Строит NamespacedKey в namespace плагина.
     *
     * {@code NamespacedKey.fromString} без префикса namespace возвращает
     * ключ из namespace {@code minecraft} (или null при недопустимых
     * символах) — а {@code new NamespacedKey(plugin, ...)} бросает
     * IllegalArgumentException. Поэтому ключ нормализуется вручную:
     * только {@code [a-z0-9._-]}.
     */
    private NamespacedKey modifierKey(String rawKey) {
        String sanitized = rawKey == null ? "attribute" : rawKey.toLowerCase(java.util.Locale.ROOT);
        StringBuilder sb = new StringBuilder(sanitized.length());
        for (int i = 0; i < sanitized.length(); i++) {
            char c = sanitized.charAt(i);
            boolean valid = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '-' || c == '.';
            sb.append(valid ? c : '_');
        }
        if (sb.length() == 0) sb.append("attribute");
        return new NamespacedKey(plugin, sb.toString());
    }
}
