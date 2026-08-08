
package me.dcplugin.dcustomitems.models;

import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;

public class CustomItem {
    private final String id;
    private final ItemStack itemStack;
    private final String type;
    private final String activationSlot;
    private final boolean placeable;
    private final List<String> effects;
    private Map<PotionEffectType, Integer> parsedEffects;
    private final Map<String, Double> attributes;
    private final String armorSetId;
    private final boolean hasSetBonus;
    private final List<String> leftClickActions;
    private final List<String> rightClickActions;
    private final long clickCooldown;
    private final int maxUses;
    private final List<String> loreTemplate;
    private final int customModelData;
    private final String permission;
    private final List<String> equipParticles;
    private final List<String> equipSounds;
    private final List<String> unequipParticles;
    private final List<String> unequipSounds;
    private final List<String> triggerActions;
    private final String equipMessage;
    private final String unequipMessage;
    private final String cooldownMessage;
    private final String activationMessage;
    private final String deactivationMessage;
    private final String usesDepletedMessage;

    public CustomItem(String id, ItemStack itemStack, String type, String activationSlot, boolean placeable, List<String> effects, Map<String, Double> attributes, String armorSetId, boolean hasSetBonus, List<String> leftClickActions, List<String> rightClickActions, long clickCooldown, int maxUses, List<String> loreTemplate, int customModelData, String permission, List<String> equipParticles, List<String> equipSounds, List<String> unequipParticles, List<String> unequipSounds, List<String> triggerActions, String equipMessage, String unequipMessage, String cooldownMessage, String activationMessage, String deactivationMessage, String usesDepletedMessage) {
        this.id = id;
        this.itemStack = itemStack;
        this.type = type;
        this.activationSlot = activationSlot;
        this.placeable = placeable;
        this.effects = effects;
        this.attributes = attributes != null ? attributes : new java.util.HashMap<>();
        this.armorSetId = armorSetId;
        this.hasSetBonus = hasSetBonus;
        this.leftClickActions = leftClickActions != null ? leftClickActions : new java.util.ArrayList<>();
        this.rightClickActions = rightClickActions != null ? rightClickActions : new java.util.ArrayList<>();
        this.clickCooldown = clickCooldown;
        this.maxUses = maxUses;
        this.loreTemplate = loreTemplate != null ? loreTemplate : new java.util.ArrayList<>();
        this.customModelData = customModelData;
        this.permission = permission;
        this.equipParticles = equipParticles != null ? equipParticles : new java.util.ArrayList<>();
        this.equipSounds = equipSounds != null ? equipSounds : new java.util.ArrayList<>();
        this.unequipParticles = unequipParticles != null ? unequipParticles : new java.util.ArrayList<>();
        this.unequipSounds = unequipSounds != null ? unequipSounds : new java.util.ArrayList<>();
        this.triggerActions = triggerActions != null ? triggerActions : new java.util.ArrayList<>();
        this.equipMessage = equipMessage;
        this.unequipMessage = unequipMessage;
        this.cooldownMessage = cooldownMessage;
        this.activationMessage = activationMessage;
        this.deactivationMessage = deactivationMessage;
        this.usesDepletedMessage = usesDepletedMessage;
    }

    // Legacy constructor for backward compatibility
    public CustomItem(String id, ItemStack itemStack, String type, String activationSlot, boolean placeable, List<String> effects, Map<String, Double> attributes, String armorSetId, boolean hasSetBonus) {
        this(id, itemStack, type, activationSlot, placeable, effects, attributes, armorSetId, hasSetBonus, new java.util.ArrayList<>(), new java.util.ArrayList<>(), 0, -1, new java.util.ArrayList<>(), -1, null, new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>(), null, null, null, null, null, null);
    }
    
    public CustomItem(String id, ItemStack itemStack, String type, String activationSlot, boolean placeable, List<String> effects, Map<String, Double> attributes, String armorSetId, boolean hasSetBonus, List<String> leftClickActions, List<String> rightClickActions, long clickCooldown) {
        this(id, itemStack, type, activationSlot, placeable, effects, attributes, armorSetId, hasSetBonus, leftClickActions, rightClickActions, clickCooldown, -1, new java.util.ArrayList<>(), -1, null, new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>(), null, null, null, null, null, null);
    }

    public String getId() {
        return id;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public String getType() {
        return type;
    }

    public String getActivationSlot() {
        return activationSlot;
    }

    public boolean isPlaceable() {
        return placeable;
    }

    public List<String> getEffects() {
        return effects;
    }

    public Map<PotionEffectType, Integer> getParsedEffects() {
        return parsedEffects;
    }

    public void setParsedEffects(Map<PotionEffectType, Integer> parsedEffects) {
        this.parsedEffects = parsedEffects;
    }

    public boolean hasEffect(PotionEffectType effectType) {
        return parsedEffects != null && parsedEffects.containsKey(effectType);
    }

    public int getEffectLevel(PotionEffectType effectType) {
        if (parsedEffects != null && parsedEffects.containsKey(effectType)) {
            return parsedEffects.get(effectType);
        }
        return 0;
    }

    public Map<String, Double> getAttributes() {
        return attributes;
    }

    public String getArmorSetId() {
        return armorSetId;
    }

    public boolean hasSetBonus() {
        return hasSetBonus;
    }

    public List<String> getLeftClickActions() {
        return leftClickActions;
    }

    public List<String> getRightClickActions() {
        return rightClickActions;
    }

    public long getClickCooldown() {
        return clickCooldown;
    }
    
    public int getMaxUses() {
        return maxUses;
    }
    
    public boolean hasMaxUses() {
        return maxUses > 0;
    }
    
    public List<String> getLoreTemplate() {
        return loreTemplate;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public boolean hasCustomModelData() {
        return customModelData > 0;
    }

    public String getPermission() {
        return permission;
    }

    public boolean hasPermission() {
        return permission != null && !permission.isEmpty();
    }

    public List<String> getEquipParticles() {
        return equipParticles;
    }

    public List<String> getEquipSounds() {
        return equipSounds;
    }

    public List<String> getUnequipParticles() {
        return unequipParticles;
    }

    public List<String> getUnequipSounds() {
        return unequipSounds;
    }

    public List<String> getTriggerActions() {
        return triggerActions;
    }

    public boolean hasTriggerActions() {
        return triggerActions != null && !triggerActions.isEmpty();
    }

    public String getEquipMessage() {
        return equipMessage;
    }

    public String getUnequipMessage() {
        return unequipMessage;
    }

    public String getCooldownMessage() {
        return cooldownMessage;
    }

    public String getActivationMessage() {
        return activationMessage;
    }

    public String getDeactivationMessage() {
        return deactivationMessage;
    }

    public String getUsesDepletedMessage() {
        return usesDepletedMessage;
    }

    public boolean hasEquipMessage() {
        return equipMessage != null && !equipMessage.isEmpty();
    }

    public boolean hasUnequipMessage() {
        return unequipMessage != null && !unequipMessage.isEmpty();
    }

    public boolean hasCooldownMessage() {
        return cooldownMessage != null && !cooldownMessage.isEmpty();
    }

    public boolean hasActivationMessage() {
        return activationMessage != null && !activationMessage.isEmpty();
    }

    public boolean hasDeactivationMessage() {
        return deactivationMessage != null && !deactivationMessage.isEmpty();
    }

    public boolean hasUsesDepletedMessage() {
        return usesDepletedMessage != null && !usesDepletedMessage.isEmpty();
    }

    public enum ItemType {
        RUNE,
        TOOL,
        ARMOR,
        CONSUMABLE
    }

    public enum ActivationSlot {
        HAND,
        OFFHAND,
        HEAD,
        CHEST,
        LEGS,
        FEET
    }

    @Override
    public String toString() {
        return "CustomItem{" +
               "id='" + id + '\'' +
               ", type='" + type + '\'' +
               ", activationSlot='" + activationSlot + '\'' +
               ", placeable=" + placeable +
               ", effects=" + effects +
               '}';
    }
}
