package me.dcplugin.dcustomitems.handlers;

import me.dcplugin.dcustomitems.models.CustomItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Управление счётчиком использований кастомных предметов.
 * Использует PersistentDataContainer для хранения данных.
 */
public class UsesManager {

    private final NamespacedKey itemUsesKey;

    public UsesManager(NamespacedKey itemUsesKey) {
        this.itemUsesKey = itemUsesKey;
    }

    /**
     * Получить текущее кол-во использований предмета
     *
     * @return кол-во использований или -1 если не установлено
     */
    public int getUses(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return -1;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return -1;

        Integer uses = meta.getPersistentDataContainer().get(itemUsesKey, PersistentDataType.INTEGER);
        return uses != null ? uses : -1;
    }

    /**
     * Установить кол-во использований
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
     * Уменьшить кол-во использований на 1
     *
     * @return новое кол-во использований
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
     * Проверить, есть ли использования у предмета
     */
    public boolean hasUsesRemaining(ItemStack itemStack, CustomItem customItem) {
        if (!customItem.hasMaxUses()) return true;
        int uses = getUses(itemStack);
        if (uses == -1) return true; // Не установлено = бесконечно
        return uses > 0;
    }

    /**
     * Инициализировать Uses для нового предмета (установить maxUses если не установлено)
     */
    public void initializeUses(ItemStack itemStack, CustomItem customItem) {
        if (!customItem.hasMaxUses()) return;
        if (getUses(itemStack) == -1) {
            setUses(itemStack, customItem.getMaxUses());
        }
    }
}
