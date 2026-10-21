package me.dcplugin.dcustomitems.handlers;

import me.dcplugin.dcustomitems.models.CustomItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Management of the use counter of custom items.
 * Uses the PersistentDataContainer to store the data.
 */
public class UsesManager {

    private final NamespacedKey itemUsesKey;

    public UsesManager(NamespacedKey itemUsesKey) {
        this.itemUsesKey = itemUsesKey;
    }

    /**
     * Get the current number of uses of the item
     *
     * @return the number of uses, or -1 if not set
     */
    public int getUses(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return -1;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return -1;

        Integer uses = meta.getPersistentDataContainer().get(itemUsesKey, PersistentDataType.INTEGER);
        return uses != null ? uses : -1;
    }

    /**
     * Set the number of uses
     */
    public void setUses(ItemStack itemStack, int uses) {
        if (itemStack == null || !itemStack.hasItemMeta()) return;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(itemUsesKey, PersistentDataType.INTEGER, uses);
            itemStack.setItemMeta(meta);
        }
    }

    /**
     * Decrease the number of uses by 1
     *
     * @return the new number of uses
     */
    public int decrementUses(ItemStack itemStack, CustomItem customItem) {
        if (itemStack == null || itemStack.getType() == Material.AIR) return -1;
        if (customItem == null || !customItem.hasMaxUses()) return -1;

        int uses = getUses(itemStack);
        if (uses == -1) uses = customItem.getMaxUses();

        uses--;
        uses = Math.max(0, uses);
        setUses(itemStack, uses);

        return uses;
    }

    /**
     * Check whether the item still has uses left
     */
    public boolean hasUsesRemaining(ItemStack itemStack, CustomItem customItem) {
        if (!customItem.hasMaxUses()) return true;
        int uses = getUses(itemStack);
        if (uses == -1) return true; // Not set = infinite
        return uses > 0;
    }

    /**
     * Initialize the uses for a new item (set maxUses if not set)
     */
    public void initializeUses(ItemStack itemStack, CustomItem customItem) {
        if (!customItem.hasMaxUses()) return;
        if (getUses(itemStack) == -1) {
            setUses(itemStack, customItem.getMaxUses());
        }
    }
}
