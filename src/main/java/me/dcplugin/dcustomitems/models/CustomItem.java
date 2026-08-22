package me.dcplugin.dcustomitems.models;

import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
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
    private final double buyPrice;
    private final double sellPrice;

    private CustomItem(Builder b) {
        this.id = b.id;
        this.itemStack = b.itemStack;
        this.type = b.type;
        this.activationSlot = b.activationSlot;
        this.placeable = b.placeable;
        this.effects = b.effects;
        this.attributes = b.attributes;
        this.armorSetId = b.armorSetId;
        this.hasSetBonus = b.hasSetBonus;
        this.leftClickActions = b.leftClickActions;
        this.rightClickActions = b.rightClickActions;
        this.clickCooldown = b.clickCooldown;
        this.maxUses = b.maxUses;
        this.loreTemplate = b.loreTemplate;
        this.customModelData = b.customModelData;
        this.permission = b.permission;
        this.equipParticles = b.equipParticles;
        this.equipSounds = b.equipSounds;
        this.unequipParticles = b.unequipParticles;
        this.unequipSounds = b.unequipSounds;
        this.triggerActions = b.triggerActions;
        this.equipMessage = b.equipMessage;
        this.unequipMessage = b.unequipMessage;
        this.cooldownMessage = b.cooldownMessage;
        this.activationMessage = b.activationMessage;
        this.deactivationMessage = b.deactivationMessage;
        this.usesDepletedMessage = b.usesDepletedMessage;
        this.buyPrice = b.buyPrice;
        this.sellPrice = b.sellPrice;
    }

    // ===== БUILDER =====

    public static Builder builder(String id, ItemStack itemStack) {
        return new Builder(id, itemStack);
    }

    public static class Builder {
        // Required
        private final String id;
        private final ItemStack itemStack;

        // Defaults
        private String type = "RUNE";
        private String activationSlot = "HAND";
        private boolean placeable = false;
        private List<String> effects = new ArrayList<>();
        private Map<String, Double> attributes = new HashMap<>();
        private String armorSetId = null;
        private boolean hasSetBonus = false;
        private List<String> leftClickActions = new ArrayList<>();
        private List<String> rightClickActions = new ArrayList<>();
        private long clickCooldown = 0;
        private int maxUses = -1;
        private List<String> loreTemplate = new ArrayList<>();
        private int customModelData = -1;
        private String permission = null;
        private List<String> equipParticles = new ArrayList<>();
        private List<String> equipSounds = new ArrayList<>();
        private List<String> unequipParticles = new ArrayList<>();
        private List<String> unequipSounds = new ArrayList<>();
        private List<String> triggerActions = new ArrayList<>();
        private String equipMessage = null;
        private String unequipMessage = null;
        private String cooldownMessage = null;
        private String activationMessage = null;
        private String deactivationMessage = null;
        private String usesDepletedMessage = null;
        private double buyPrice = -1;  // -1 = нельзя купить
        private double sellPrice = -1; // -1 = нельзя продать

        private Builder(String id, ItemStack itemStack) {
            this.id = id;
            this.itemStack = itemStack;
        }

        public Builder type(String type) { this.type = type; return this; }
        public Builder activationSlot(String slot) { this.activationSlot = slot; return this; }
        public Builder placeable(boolean placeable) { this.placeable = placeable; return this; }
        public Builder effects(List<String> effects) { this.effects = effects != null ? effects : new ArrayList<>(); return this; }
        public Builder attributes(Map<String, Double> attributes) { this.attributes = attributes != null ? attributes : new HashMap<>(); return this; }
        public Builder armorSet(String armorSetId, boolean hasSetBonus) { this.armorSetId = armorSetId; this.hasSetBonus = hasSetBonus; return this; }
        public Builder leftClickActions(List<String> actions) { this.leftClickActions = actions != null ? actions : new ArrayList<>(); return this; }
        public Builder rightClickActions(List<String> actions) { this.rightClickActions = actions != null ? actions : new ArrayList<>(); return this; }
        public Builder clickCooldown(long ms) { this.clickCooldown = ms; return this; }
        public Builder maxUses(int maxUses) { this.maxUses = maxUses; return this; }
        public Builder loreTemplate(List<String> lore) { this.loreTemplate = lore != null ? lore : new ArrayList<>(); return this; }
        public Builder customModelData(int data) { this.customModelData = data; return this; }
        public Builder permission(String permission) { this.permission = permission; return this; }
        public Builder equipParticles(List<String> particles) { this.equipParticles = particles != null ? particles : new ArrayList<>(); return this; }
        public Builder equipSounds(List<String> sounds) { this.equipSounds = sounds != null ? sounds : new ArrayList<>(); return this; }
        public Builder unequipParticles(List<String> particles) { this.unequipParticles = particles != null ? particles : new ArrayList<>(); return this; }
        public Builder unequipSounds(List<String> sounds) { this.unequipSounds = sounds != null ? sounds : new ArrayList<>(); return this; }
        public Builder triggerActions(List<String> actions) { this.triggerActions = actions != null ? actions : new ArrayList<>(); return this; }
        public Builder equipMessage(String msg) { this.equipMessage = msg; return this; }
        public Builder unequipMessage(String msg) { this.unequipMessage = msg; return this; }
        public Builder cooldownMessage(String msg) { this.cooldownMessage = msg; return this; }
        public Builder activationMessage(String msg) { this.activationMessage = msg; return this; }
        public Builder deactivationMessage(String msg) { this.deactivationMessage = msg; return this; }
        public Builder usesDepletedMessage(String msg) { this.usesDepletedMessage = msg; return this; }
        public Builder buyPrice(double price) { this.buyPrice = price; return this; }
        public Builder sellPrice(double price) { this.sellPrice = price; return this; }

        public CustomItem build() {
            return new CustomItem(this);
        }
    }

    // ===== Legacy constructors (для обратной совместимости) =====

    /** @deprecated Используйте {@link #builder(String, ItemStack)} */
    @Deprecated
    public CustomItem(String id, ItemStack itemStack, String type, String activationSlot, boolean placeable, List<String> effects, Map<String, Double> attributes, String armorSetId, boolean hasSetBonus, List<String> leftClickActions, List<String> rightClickActions, long clickCooldown, int maxUses, List<String> loreTemplate, int customModelData, String permission, List<String> equipParticles, List<String> equipSounds, List<String> unequipParticles, List<String> unequipSounds, List<String> triggerActions, String equipMessage, String unequipMessage, String cooldownMessage, String activationMessage, String deactivationMessage, String usesDepletedMessage, double buyPrice, double sellPrice) {
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
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
    }

    /** @deprecated Используйте {@link #builder(String, ItemStack)} */
    @Deprecated
    public CustomItem(String id, ItemStack itemStack, String type, String activationSlot, boolean placeable, List<String> effects, Map<String, Double> attributes, String armorSetId, boolean hasSetBonus) {
        this(id, itemStack, type, activationSlot, placeable, effects, attributes, armorSetId, hasSetBonus, new java.util.ArrayList<>(), new java.util.ArrayList<>(), 0, -1, new java.util.ArrayList<>(), -1, null, new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>(), null, null, null, null, null, null, -1, -1);
    }

    /** @deprecated Используйте {@link #builder(String, ItemStack)} */
    @Deprecated
    public CustomItem(String id, ItemStack itemStack, String type, String activationSlot, boolean placeable, List<String> effects, Map<String, Double> attributes, String armorSetId, boolean hasSetBonus, List<String> leftClickActions, List<String> rightClickActions, long clickCooldown) {
        this(id, itemStack, type, activationSlot, placeable, effects, attributes, armorSetId, hasSetBonus, leftClickActions, rightClickActions, clickCooldown, -1, new java.util.ArrayList<>(), -1, null, new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.ArrayList<>(), null, null, null, null, null, null, -1, -1);
    }

    // ===== Геттеры =====

    public String getId() { return id; }
    public ItemStack getItemStack() { return itemStack; }
    public String getType() { return type; }
    public String getActivationSlot() { return activationSlot; }
    public boolean isPlaceable() { return placeable; }
    public List<String> getEffects() { return effects; }
    public Map<String, Double> getAttributes() { return attributes; }
    public String getArmorSetId() { return armorSetId; }
    public boolean hasSetBonus() { return hasSetBonus; }
    public List<String> getLeftClickActions() { return leftClickActions; }
    public List<String> getRightClickActions() { return rightClickActions; }
    public long getClickCooldown() { return clickCooldown; }
    public int getMaxUses() { return maxUses; }
    public boolean hasMaxUses() { return maxUses > 0; }
    public List<String> getLoreTemplate() { return loreTemplate; }
    public int getCustomModelData() { return customModelData; }
    public boolean hasCustomModelData() { return customModelData > 0; }
    public String getPermission() { return permission; }
    public boolean hasPermission() { return permission != null && !permission.isEmpty(); }
    public List<String> getEquipParticles() { return equipParticles; }
    public List<String> getEquipSounds() { return equipSounds; }
    public List<String> getUnequipParticles() { return unequipParticles; }
    public List<String> getUnequipSounds() { return unequipSounds; }
    public List<String> getTriggerActions() { return triggerActions; }
    public boolean hasTriggerActions() { return triggerActions != null && !triggerActions.isEmpty(); }
    public String getEquipMessage() { return equipMessage; }
    public String getUnequipMessage() { return unequipMessage; }
    public String getCooldownMessage() { return cooldownMessage; }
    public String getActivationMessage() { return activationMessage; }
    public String getDeactivationMessage() { return deactivationMessage; }
    public String getUsesDepletedMessage() { return usesDepletedMessage; }

    public Map<PotionEffectType, Integer> getParsedEffects() { return parsedEffects; }
    public void setParsedEffects(Map<PotionEffectType, Integer> parsedEffects) { this.parsedEffects = parsedEffects; }

    public boolean hasEffect(PotionEffectType effectType) {
        return parsedEffects != null && parsedEffects.containsKey(effectType);
    }

    public int getEffectLevel(PotionEffectType effectType) {
        if (parsedEffects != null && parsedEffects.containsKey(effectType)) {
            return parsedEffects.get(effectType);
        }
        return 0;
    }

    public boolean hasEquipMessage() { return equipMessage != null && !equipMessage.trim().isEmpty(); }
    public boolean hasUnequipMessage() { return unequipMessage != null && !unequipMessage.trim().isEmpty(); }
    public boolean hasCooldownMessage() { return cooldownMessage != null && !cooldownMessage.trim().isEmpty(); }
    public boolean hasActivationMessage() { return activationMessage != null && !activationMessage.trim().isEmpty(); }
    public boolean hasDeactivationMessage() { return deactivationMessage != null && !deactivationMessage.trim().isEmpty(); }
    public boolean hasUsesDepletedMessage() { return usesDepletedMessage != null && !usesDepletedMessage.trim().isEmpty(); }
    public double getBuyPrice() { return buyPrice; }
    public double getSellPrice() { return sellPrice; }
    public boolean isBuyable() { return buyPrice > 0; }
    public boolean isSellable() { return sellPrice > 0; }

    public enum ItemType { RUNE, TOOL, ARMOR, CONSUMABLE }
    public enum ActivationSlot { HAND, OFFHAND, HEAD, CHEST, LEGS, FEET }

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
