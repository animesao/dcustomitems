package me.dcplugin.dcustomitems.handlers;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.models.CustomItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * Менеджер кастомных предметов.
 * Делегирует загрузку в ItemLoader, lore в LoreManager, uses в UsesManager.
 */
public class CustomItemHandler {

    private final Main plugin;
    private final Map<String, CustomItem> customItems;
    private final NamespacedKey customItemKey;
    private final NamespacedKey itemUsesKey;
    private final NamespacedKey itemModelKey;
    private final NamespacedKey acquiredTimeKey;

    // Delegates
    private final ItemLoader itemLoader;
    private final LoreManager loreManager;
    private final UsesManager usesManager;
    private final RecipeManager recipeManager;

    public CustomItemHandler(Main plugin) {
        this.plugin = plugin;
        this.customItems = new HashMap<>();
        this.customItemKey = new NamespacedKey(plugin, "custom_item_id");
        this.itemUsesKey = new NamespacedKey(plugin, "item_uses");
        this.itemModelKey = new NamespacedKey(plugin, "item_model");

        this.itemLoader = new ItemLoader(plugin, customItemKey, itemModelKey);
        this.loreManager = new LoreManager();
        this.usesManager = new UsesManager(itemUsesKey);
        this.recipeManager = new RecipeManager(plugin);

        this.acquiredTimeKey = new NamespacedKey(plugin, "acquired_time");
    }

    // ===== Loading =====

    public void loadCustomItems() {
        customItems.clear();
        recipeManager.clear();
        customItems.putAll(itemLoader.loadAll());

        // Регистрируем крафт-рецепты после полной загрузки предметов:
        // ингредиенты могут ссылаться на другие кастомные предметы.
        for (CustomItem item : customItems.values()) {
            if (item.hasRecipes()) {
                recipeManager.register(item);
            }
        }

        plugin.getLogger().info("Загружено " + customItems.size() + " кастомных предметов");
    }

    public void reloadItems() {
        loadCustomItems();
    }

    /**
     * Менеджер крафт-рецептов (recipes секция в YAML предмета).
     */
    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    // ===== Item lookup =====

    public CustomItem getCustomItem(String itemId) {
        return customItems.get(itemId);
    }

    public String getCustomItemId(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return null;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(customItemKey, PersistentDataType.STRING);
    }

    public Map<String, CustomItem> getAllCustomItems() {
        return new HashMap<>(customItems);
    }

    public boolean isCustomItem(ItemStack itemStack) {
        return getCustomItemId(itemStack) != null;
    }

    // ===== Lore =====

    /**
     * Обновить lore предмета с подстановкой %cooldown% и %uses%
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

        return loreManager.applyCooldownLore(itemStack, customItem, remaining);
    }

    /**
     * Обновить lore предмета с подстановкой %uses%
     */
    public ItemStack updateItemWithUses(ItemStack itemStack) {
        String customItemId = getCustomItemId(itemStack);
        if (customItemId == null) return itemStack;

        CustomItem customItem = getCustomItem(customItemId);
        if (customItem == null || !customItem.hasMaxUses()) return itemStack;

        int uses = getItemUses(itemStack);
        if (uses == -1) {
            uses = customItem.getMaxUses();
            setItemUses(itemStack, uses);
        }

        return loreManager.applyUsesLore(itemStack, customItem, uses);
    }

    // ===== Uses =====

    public int getItemUses(ItemStack itemStack) {
        int uses = usesManager.getUses(itemStack);
        if (uses != -1) return uses;

        // Fallback: return maxUses from CustomItem
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
        usesManager.setUses(itemStack, uses);
    }

    // ===== Uses без YAML-модели (для Java API-предметов) =====

    /**
     * Сырой счётчик использований из PDC (-1, если не установлен).
     * Не зависит от YAML-модели — работает и для AbstractCustomItem.
     */
    public int getStoredUses(ItemStack itemStack) {
        return usesManager.getUses(itemStack);
    }

    /**
     * Записать счётчик использований в PDC (для Java API-предметов).
     */
    public void setStoredUses(ItemStack itemStack, int uses) {
        usesManager.setUses(itemStack, uses);
    }

    /**
     * Уменьшить счётчик использований на 1 без привязки к YAML-модели.
     * Возвращает новое значение (или -1, если счётчик не был установлен).
     */
    public int decrementStoredUses(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) return -1;
        int uses = usesManager.getUses(itemStack);
        if (uses == -1) return -1;
        uses = Math.max(0, uses - 1);
        usesManager.setUses(itemStack, uses);
        return uses;
    }

    public void decrementItemUses(ItemStack itemStack) {
        String customItemId = getCustomItemId(itemStack);
        if (customItemId == null) return;

        CustomItem customItem = getCustomItem(customItemId);
        if (customItem == null || !customItem.hasMaxUses()) return;

        int uses = usesManager.decrementUses(itemStack, customItem);
        if (uses >= 0) {
            loreManager.applyUsesLore(itemStack, customItem, uses);
        }
    }

    // ===== Duration =====

    /**
     * Установить время приобретения предмета (если ещё не установлено)
     */
    public void ensureAcquiredTime(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;
        if (!meta.getPersistentDataContainer().has(acquiredTimeKey, PersistentDataType.LONG)) {
            meta.getPersistentDataContainer().set(acquiredTimeKey, PersistentDataType.LONG, System.currentTimeMillis());
            itemStack.setItemMeta(meta);
        }
    }

    /**
     * Получить оставшееся время жизни предмета (в секундах). -1 = бессрочно.
     */
    public long getRemainingDuration(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return -1;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return -1;
        Long acquiredTime = meta.getPersistentDataContainer().get(acquiredTimeKey, PersistentDataType.LONG);
        if (acquiredTime == null) return -1;

        String itemId = getCustomItemId(itemStack);
        if (itemId == null) return -1;
        CustomItem customItem = getCustomItem(itemId);
        if (customItem == null || !customItem.hasDuration()) return -1;

        long elapsed = (System.currentTimeMillis() - acquiredTime) / 1000;
        return Math.max(0, customItem.getDuration() - elapsed);
    }

    /**
     * Проверить, истёк ли срок жизни предмета
     */
    public boolean isExpired(ItemStack itemStack) {
        return getRemainingDuration(itemStack) == 0;
    }
}
