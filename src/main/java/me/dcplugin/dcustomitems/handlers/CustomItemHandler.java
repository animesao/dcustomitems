package me.dcplugin.dcustomitems.handlers;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.models.CustomItem;
import me.dcplugin.dcustomitems.utils.ItemBuilder;
import me.dcplugin.dcustomitems.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomItemHandler {

    private final Main plugin;
    private final Map<String, CustomItem> customItems;
    private final NamespacedKey customItemKey;
    private final NamespacedKey itemUsesKey;
    private final NamespacedKey itemModelKey;

    public CustomItemHandler(Main plugin) {
        this.plugin = plugin;
        this.customItems = new HashMap<>();
        this.customItemKey = new NamespacedKey(plugin, "custom_item_id");
        this.itemUsesKey = new NamespacedKey(plugin, "item_uses");
        this.itemModelKey = new NamespacedKey(plugin, "item_model");
    }

    public void loadCustomItems() {
        customItems.clear();

        // 1. Загружаем предметы из config.yml
        ConfigurationSection config = plugin.getConfig();
        for (String itemId : config.getKeys(false)) {
            try {
                if ("set-bonuses".equals(itemId)) {
                    continue;
                }
                CustomItem customItem = loadCustomItem(itemId, config.getConfigurationSection(itemId));
                if (customItem != null) {
                    customItems.put(itemId, customItem);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Ошибка при загрузке предмета " + itemId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // 2. Загружаем предметы из папки items/
        loadItemsFromFolder();

        plugin.getLogger().info("Загружено " + customItems.size() + " кастомных предметов");
    }

    private void loadItemsFromFolder() {
        File itemsFolder = new File(plugin.getDataFolder(), "items");
        if (!itemsFolder.exists()) {
            itemsFolder.mkdirs();
            plugin.getLogger().info("Создана папка items/ для кастомных предметов");
            return;
        }

        File[] files = itemsFolder.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().info("Папка items/ пуста. Добавьте .yml файлы с предметами.");
            return;
        }

        for (File file : files) {
            try {
                YamlConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);
                String fileName = file.getName().replace(".yml", "").replace(".yaml", "");
                
                for (String itemId : fileConfig.getKeys(false)) {
                    try {
                        if ("set-bonuses".equals(itemId)) {
                            continue;
                        }
                        CustomItem customItem = loadCustomItem(itemId, fileConfig.getConfigurationSection(itemId));
                        if (customItem != null) {
                            customItems.put(itemId, customItem);
                            plugin.getLogger().fine("[items] Загружен предмет '" + itemId + "' из " + file.getName());
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("Ошибка при загрузке предмета " + itemId + " из " + file.getName() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Ошибка при чтении файла " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    private CustomItem loadCustomItem(String itemId, ConfigurationSection section) {
        if (section == null) return null;
        
        // Пропускаем секцию set-bonuses
        if ("set-bonuses".equals(itemId)) {
            return null;
        }

        try {
            // Загружаем основные параметры
            String type = section.getString("type", "RUNE");
            String activationSlot = section.getString("activation-slot", "HAND");
            boolean placeable = section.getBoolean("placeable", false);
            List<String> effects = section.getStringList("effects");

            // Валидация типа
            List<String> validTypes = List.of("RUNE", "TOOL", "ARMOR", "CONSUMABLE");
            if (!validTypes.contains(type.toUpperCase())) {
                plugin.getLogger().warning("Недопустимый тип предмета: " + type + " для предмета: " + itemId);
                type = "RUNE";
            }

            // Валидация activation-slot
            List<String> validSlots = List.of("HAND", "OFFHAND", "HEAD", "CHEST", "LEGS", "FEET");
            if (!validSlots.contains(activationSlot.toUpperCase())) {
                plugin.getLogger().warning("Недопустимый слот активации: " + activationSlot + " для предмета: " + itemId);
                activationSlot = "HAND";
            }

            // Загружаем параметры предмета
            ConfigurationSection itemSection = section.getConfigurationSection("item");
            if (itemSection == null) {
                plugin.getLogger().warning("Секция 'item' не найдена для предмета: " + itemId);
                return null;
            }

            // Материал
            String materialName = itemSection.getString("type", "STONE");
            Material material;
            try {
                material = Material.valueOf(materialName.toUpperCase());
                if (material.isAir()) {
                    plugin.getLogger().warning("Недопустимый материал (AIR): " + materialName + " для предмета: " + itemId);
                    return null;
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Неизвестный материал: " + materialName + " для предмета: " + itemId);
                return null;
            }

            ItemBuilder builder = new ItemBuilder(material);

            // Название
            String title = itemSection.getString("title");
            if (title != null) {
                builder.setDisplayName(title);
            }

            // Описание
            List<String> lore = section.getStringList("lore");
            if (!lore.isEmpty()) {
                builder.setLore(lore);
            }

            // Количество
            int amount = itemSection.getInt("amount", 1);
            builder.setAmount(amount);

            // Свечение
            boolean glowing = itemSection.getBoolean("glowing", false);
            if (glowing) {
                builder.setGlowing(true);
            }

            // Неломаемость
            boolean unbreakable = itemSection.getBoolean("unbreakable", false);
            if (unbreakable) {
                builder.setUnbreakable(true);
            }

            // Чары
            ConfigurationSection enchantmentsSection = itemSection.getConfigurationSection("enchantments");
            if (enchantmentsSection != null) {
                for (String enchantName : enchantmentsSection.getKeys(false)) {
                    try {
                        Enchantment enchantment = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchantName.toLowerCase()));
                        if (enchantment != null) {
                            int level = enchantmentsSection.getInt(enchantName);
                            builder.addEnchantment(enchantment, level);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Неизвестное зачарование: " + enchantName);
                    }
                }
            }

            // Флаги предмета
            List<String> itemFlags = itemSection.getStringList("item-flags");
            for (String flagName : itemFlags) {
                try {
                    ItemFlag flag = ItemFlag.valueOf(flagName.toUpperCase());
                    builder.addItemFlags(flag);
                } catch (Exception e) {
                    plugin.getLogger().warning("Неизвестный флаг предмета: " + flagName);
                }
            }

            // Текстура для головы
            String texture = itemSection.getString("texture");
            if (texture != null && material == Material.PLAYER_HEAD) {
                builder.setSkullTexture(texture);
            }

            ItemStack itemStack = builder.build();

            // Добавляем метку кастомного предмета
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(customItemKey, PersistentDataType.STRING, itemId);
                
                // НЕ применяем атрибуты к ItemMeta - они будут применяться через AttributeManager
                // только при активации в правильном слоте
                
                itemStack.setItemMeta(meta);
            }

            // Предварительно заменяем %cooldown% и %uses% в lore
            // (чтобы плейсхолдеры не отображались как текст)
            int maxUses = section.getInt("max-uses", -1);
            List<String> lorePre = section.getStringList("lore");
            List<String> processedLore = new ArrayList<>();
            List<String> loreTemplate = new ArrayList<>(); // Шаблон для обновлений
            
            if (!lorePre.isEmpty()) {
                for (String line : lorePre) {
                    // Создаём processed версию для отображения
                    String processed = line.replace("%cooldown%", "0.0");
                    if (maxUses > 0) {
                        processed = processed.replace("%uses%", String.valueOf(maxUses));
                    } else {
                        processed = processed.replace("%uses%", "∞");
                    }
                    processedLore.add(ColorUtils.colorize(processed));
                    
                    // Сохраняем шаблон БЕЗ colorize
                    // %cooldown% ОСТАЁМСЯ для динамической замены в updateItemWithCooldown()
                    // %uses% заменяется на значение
                    String templateLine = line;
                    if (maxUses > 0) {
                        templateLine = templateLine.replace("%uses%", String.valueOf(maxUses));
                    } else if (line.contains("%uses%")) {
                        templateLine = templateLine.replace("%uses%", "∞");
                    }
                    loreTemplate.add(templateLine);
                }
                meta = itemStack.getItemMeta();
                if (meta != null) {
                    meta.setLore(processedLore);
                    itemStack.setItemMeta(meta);
                }
            }

            // Загружаем основные параметры
            if (effects == null) {
                effects = new ArrayList<>();
            }

            // Загружаем атрибуты для CustomItem объекта
            Map<String, Double> attributes = new HashMap<>();
            if (itemSection.contains("attributes")) {
                ConfigurationSection attrSection = itemSection.getConfigurationSection("attributes");
                if (attrSection != null) {
                    for (String attrKey : attrSection.getKeys(false)) {
                        Object attrValue = attrSection.get(attrKey);
                        double value;
                        
                        if (attrValue instanceof Number) {
                            value = ((Number) attrValue).doubleValue();
                        } else if (attrValue instanceof ConfigurationSection) {
                            ConfigurationSection attrDef = attrSection.getConfigurationSection(attrKey);
                            value = attrDef.getDouble("value", 0);
                        } else {
                            continue;
                        }
                        
                        attributes.put(attrKey.toUpperCase(), value);
                    }
                }
            }

            // Загружаем информацию о сете
            String armorSetId = itemSection.getString("armor-set", null);
            boolean hasSetBonus = itemSection.getBoolean("has-set-bonus", false);
            
            if (armorSetId != null) {
                plugin.getLogger().fine("[LOAD] Предмет '" + itemId + "': armor-set=" + armorSetId);
            }

            // Загружаем действия при клике по блокам
            List<String> leftClickActions = section.getStringList("left-click-actions");
            List<String> rightClickActions = section.getStringList("right-click-actions");
            long clickCooldown = section.getLong("click-cooldown", 0);

            // Загружаем новые параметры
            int customModelData = itemSection.getInt("custom-model-data", -1);
            String itemModel = itemSection.getString("item-model", null); // НОВЫЙ ПАРАМЕТР для 1.21.11
            String permission = section.getString("permission", null);
            List<String> equipParticles = section.getStringList("equip-particles");
            List<String> equipSounds = section.getStringList("equip-sounds");
            List<String> unequipParticles = section.getStringList("unequip-particles");
            List<String> unequipSounds = section.getStringList("unequip-sounds");
            List<String> triggerActions = section.getStringList("trigger-actions");

            // Загружаем per-item сообщения
            String equipMessage = section.getString("equip-message", null);
            String unequipMessage = section.getString("unequip-message", null);
            String cooldownMessage = section.getString("cooldown-message", null);
            String activationMessage = section.getString("activation-message", null);
            String deactivationMessage = section.getString("deactivation-message", null);
            String usesDepletedMessage = section.getString("uses-depleted-message", null);

            // Применяем CustomModelData или ItemModel к предмету
            meta = itemStack.getItemMeta();
            if (meta != null) {
                // ПРИОРИТЕТ: item-model (для 1.21.11) > custom-model-data (legacy)
                if (itemModel != null && !itemModel.isEmpty()) {
                    // Используем custom_model_data с строками (Minecraft 1.21.4+)
                    // Сохраняем строку в PDC для использования в ресурс-паке
                    meta.getPersistentDataContainer().set(itemModelKey, PersistentDataType.STRING, itemModel);
                    plugin.getLogger().fine("[LOAD] Применён item_model: " + itemModel + " для " + itemId);
                } else if (customModelData > 0) {
                    // Используем старый custom_model_data (legacy)
                    meta.setCustomModelData(customModelData);
                    plugin.getLogger().fine("[LOAD] Применён custom_model_data: " + customModelData + " для " + itemId);
                }
                
                itemStack.setItemMeta(meta);
            }

            CustomItem customItem = new CustomItem(itemId, itemStack, type, activationSlot, placeable, effects, attributes, armorSetId, hasSetBonus, leftClickActions, rightClickActions, clickCooldown, maxUses, loreTemplate, customModelData, permission, equipParticles, equipSounds, unequipParticles, unequipSounds, triggerActions, equipMessage, unequipMessage, cooldownMessage, activationMessage, deactivationMessage, usesDepletedMessage);

            // Парсим эффекты
            if (!effects.isEmpty()) {
                customItem.setParsedEffects(plugin.getEffectManager().parseEffects(effects));
            }

            return customItem;

        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при создании предмета " + itemId + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public CustomItem getCustomItem(String itemId) {
        return customItems.get(itemId);
    }

    public String getCustomItemId(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return null;
        }

        return meta.getPersistentDataContainer().get(customItemKey, PersistentDataType.STRING);
    }

    public Map<String, CustomItem> getAllCustomItems() {
        return new HashMap<>(customItems);
    }

    public boolean isCustomItem(ItemStack itemStack) {
        return getCustomItemId(itemStack) != null;
    }

    public void reloadItems() {
        loadCustomItems();
    }
    
    /**
     * Обновляет лор предмета с подстановкой %cooldown% (оставшееся время кулдауна в секундах)
     */
    public ItemStack updateItemWithCooldown(ItemStack itemStack, long lastClickTime, long cooldownMs) {
        String customItemId = getCustomItemId(itemStack);
        if (customItemId == null) return itemStack;
        
        CustomItem customItem = getCustomItem(customItemId);
        if (customItem == null) return itemStack;
        
        long now = System.currentTimeMillis();
        long remaining = 0;
        if (lastClickTime > 0 && cooldownMs > 0) {
            long elapsed = now - lastClickTime;
            remaining = Math.max(0, cooldownMs - elapsed);
        }
        
        double seconds = remaining / 1000.0;
        String cooldownStr = String.format("%.1f", seconds);
        
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            List<String> loreTemplate = customItem.getLoreTemplate();
            if (loreTemplate != null && !loreTemplate.isEmpty()) {
                List<String> newLore = new ArrayList<>();
                for (String line : loreTemplate) {
                    String processed = line.replace("%cooldown%", cooldownStr);
                    int uses = getItemUses(itemStack);
                    if (customItem.hasMaxUses()) {
                        processed = processed.replace("%uses%", String.valueOf(uses));
                    } else {
                        processed = processed.replace("%uses%", "∞");
                    }
                    newLore.add(ColorUtils.colorize(processed));
                }
                meta.setLore(newLore);
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }
    
    public int getItemUses(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return -1;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return -1;
        }
        Integer uses = meta.getPersistentDataContainer().get(itemUsesKey, PersistentDataType.INTEGER);
        if (uses != null) {
            return uses;
        }
        // Если Uses не установлены, возвращаем maxUses предмета
        String customItemId = getCustomItemId(itemStack);
        if (customItemId != null) {
            CustomItem customItem = getCustomItem(customItemId);
            if (customItem != null && customItem.hasMaxUses()) {
                return customItem.getMaxUses();
            }
        }
        return -1;
    }
    
    public void setItemUses(ItemStack itemStack, int uses) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(itemUsesKey, PersistentDataType.INTEGER, uses);
            itemStack.setItemMeta(meta);
        }
    }
    
    public ItemStack updateItemWithUses(ItemStack itemStack) {
        String customItemId = getCustomItemId(itemStack);
        if (customItemId == null) {
            return itemStack;
        }
        
        CustomItem customItem = getCustomItem(customItemId);
        if (customItem == null || !customItem.hasMaxUses()) {
            return itemStack;
        }
        
        int uses = getItemUses(itemStack);
        if (uses == -1) {
            setItemUses(itemStack, customItem.getMaxUses());
            uses = customItem.getMaxUses();
        }
        
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            // Используем шаблон лора для пересборки с подстановкой %uses%
            List<String> loreTemplate = customItem.getLoreTemplate();
            if (loreTemplate != null && !loreTemplate.isEmpty()) {
                List<String> newLore = new ArrayList<>();
                for (String line : loreTemplate) {
                    String processed = line.replace("%uses%", String.valueOf(uses));
                    processed = processed.replace("%cooldown%", "0.0");
                    newLore.add(ColorUtils.colorize(processed));
                }
                meta.setLore(newLore);
            }
            itemStack.setItemMeta(meta);
        }
        
        return itemStack;
    }
    
    public void decrementItemUses(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }
        
        String customItemId = getCustomItemId(itemStack);
        if (customItemId == null) {
            return;
        }
        
        CustomItem customItem = getCustomItem(customItemId);
        if (customItem == null || !customItem.hasMaxUses()) {
            return;
        }
        
        int uses = getItemUses(itemStack);
        if (uses == -1) {
            uses = customItem.getMaxUses();
        }
        
        uses--;
        uses = Math.max(0, uses);
        
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }
        
        meta.getPersistentDataContainer().set(itemUsesKey, PersistentDataType.INTEGER, uses);
        
        List<String> loreTemplate = customItem.getLoreTemplate();
        if (loreTemplate != null && !loreTemplate.isEmpty()) {
            List<String> newLore = new ArrayList<>();
            for (String line : loreTemplate) {
                String processed = line.replace("%uses%", String.valueOf(uses));
                processed = processed.replace("%cooldown%", "0.0");
                newLore.add(ColorUtils.colorize(processed));
            }
            meta.setLore(newLore);
        }
        
        itemStack.setItemMeta(meta);
    }
}