import me.dcplugin.dcustomitems.api.modules.Module;
import me.dcplugin.dcustomitems.Main;
import org.bukkit.entity.Player;

/**
 * 🛒 Модуль Магазина
 * 
 * Структура:
 * items/shop/
 * ├── config.yml
 * ├── items.yml
 * └── shop.java
 * 
 * Команда: /shop
 */
public class shopModule extends Module {

    public shopModule(Main plugin, String id, java.io.File folder) {
        super(plugin, id, folder);
    }

    @Override
    protected void onEnable() {
        // Логика при включении
        plugin.getLogger().info("[Shop] Магазин включен!");
    }

    @Override
    protected void onDisable() {
        // Логика при выключении
        plugin.getLogger().info("[Shop] Магазин выключен!");
    }

    /**
     * Открыть магазин игроку
     */
    public void openShop(Player player) {
        // Создаём инвентарь
        org.bukkit.inventory.Inventory menu = plugin.getServer().createInventory(null, 27, "§6Магазин");
        
        // Заполняем фон
        org.bukkit.inventory.ItemStack glass = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < 27; i++) {
            menu.setItem(i, glass);
        }
        
        // Устанавливаем предметы из items.yml
        for (String itemId : getAllItemIds()) {
            java.util.Map<String, Object> itemData = getItem(itemId);
            org.bukkit.inventory.ItemStack item = createItemFromData(itemData);
            if (item != null) {
                // Находим свободный слот
                for (int i = 0; i < 27; i++) {
                    if (menu.getItem(i) == null || menu.getItem(i).getType() == org.bukkit.Material.GRAY_STAINED_GLASS_PANE) {
                        menu.setItem(i, item);
                        break;
                    }
                }
            }
        }
        
        player.openInventory(menu);
    }

    private org.bukkit.inventory.ItemStack createItemFromData(java.util.Map<String, Object> data) {
        String materialName = (String) data.get("material");
        String name = (String) data.get("name");
        java.util.List<String> lore = (java.util.List<String>) data.get("lore");
        
        org.bukkit.Material material = org.bukkit.Material.matchMaterial(materialName != null ? materialName.toUpperCase() : "STONE");
        if (material == null) material = org.bukkit.Material.STONE;
        
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(name.replace("&", "§"));
            }
            if (lore != null) {
                java.util.List<String> coloredLore = new java.util.ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(line.replace("&", "§"));
                }
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }
}
