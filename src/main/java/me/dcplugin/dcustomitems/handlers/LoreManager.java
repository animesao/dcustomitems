package me.dcplugin.dcustomitems.handlers;

import me.dcplugin.dcustomitems.models.CustomItem;
import me.dcplugin.dcustomitems.utils.ColorUtils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Управление шаблонами lore для предметов:
 * подстановка %cooldown% и %uses% в лор предмета.
 */
public class LoreManager {

    /**
     * Обновить lore предмета с подстановкой %cooldown% и %uses%
     *
     * @param itemStack    предмет
     * @param customItem   кастомный предмет
     * @param cooldownStr  строка кулдауна (напр. "3.5")
     * @param uses         текущее кол-во использований
     */
    public ItemStack applyLoreTemplate(ItemStack itemStack, CustomItem customItem, String cooldownStr, int uses) {
        List<String> loreTemplate = customItem.getLoreTemplate();
        if (loreTemplate == null || loreTemplate.isEmpty()) return itemStack;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;

        List<String> newLore = new ArrayList<>();
        for (String line : loreTemplate) {
            String processed = line.replace("%cooldown%", cooldownStr);
            if (customItem.hasMaxUses()) {
                processed = processed.replace("%uses%", String.valueOf(uses));
            } else {
                processed = processed.replace("%uses%", "∞");
            }
            newLore.add(ColorUtils.colorize(processed));
        }
        meta.setLore(newLore);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    /**
     * Обновить lore с кулдауном (uses не меняется)
     */
    public ItemStack applyCooldownLore(ItemStack itemStack, CustomItem customItem, long remainingMs) {
        double seconds = remainingMs / 1000.0;
        String cooldownStr = String.format("%.1f", seconds);

        // Получаем текущие uses из меты предмета (если есть)
        int uses = -1;
        if (customItem.hasMaxUses()) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                Integer storedUses = meta.getPersistentDataContainer().get(
                        new org.bukkit.NamespacedKey("dcustomitems", "item_uses"),
                        org.bukkit.persistence.PersistentDataType.INTEGER);
                uses = storedUses != null ? storedUses : customItem.getMaxUses();
            }
        }

        return applyLoreTemplate(itemStack, customItem, cooldownStr, uses);
    }

    /**
     * Обновить lore с количеством использований (cooldown = 0.0)
     */
    public ItemStack applyUsesLore(ItemStack itemStack, CustomItem customItem, int uses) {
        return applyLoreTemplate(itemStack, customItem, "0.0", uses);
    }
}
