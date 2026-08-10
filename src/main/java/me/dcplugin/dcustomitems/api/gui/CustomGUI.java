package me.dcplugin.dcustomitems.api.gui;

import me.dcplugin.dcustomitems.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Базовый класс для создания кастомных GUI
 * 
 * Пример использования:
 * public class MyGUI extends CustomGUI {
 *     public MyGUI() {
 *         super("my_gui", "Моё меню", 27);
 *     }
 *     
 *     @Override
 *     public void onOpen(Player player) {
 *         setItem(13, Material.DIAMOND, "&bАлмаз");
 *     }
 *     
 *     @Override
 *     public void onClick(Player player, int slot, ItemStack item) {
 *         if (slot == 13) {
 *             player.sendMessage("Вы нажали на алмаз!");
 *         }
 *     }
 * }
 */
public abstract class CustomGUI implements InventoryHolder, Listener {

    protected Main plugin;
    protected final String id;
    protected final String title;
    protected final int size;
    protected final Map<Integer, ItemStack> items = new HashMap<>();
    protected final Map<UUID, Inventory> openInventories = new HashMap<>();
    
    private Inventory inventory;

    public CustomGUI(String id, String title, int size) {
        this.id = id;
        this.title = colorize(title);
        this.size = size;
        this.plugin = null; // Will be set by GUIManager
    }
    
    /**
     * Set plugin instance (called by GUIManager)
     */
    public void setPlugin(Main plugin) {
        this.plugin = plugin;
        if (plugin != null) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
        }
    }

    /**
     * Открыть GUI игроку
     */
    public void open(Player player) {
        onOpen(player);
        
        inventory = Bukkit.createInventory(this, size, title);
        
        // Устанавливаем предметы
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue());
        }
        
        player.openInventory(inventory);
        openInventories.put(player.getUniqueId(), inventory);
    }

    /**
     * Закрыть GUI
     */
    public void close(Player player) {
        player.closeInventory();
        openInventories.remove(player.getUniqueId());
    }

    /**
     * Установить предмет в слот
     */
    protected void setItem(int slot, Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(colorize(name));
            }
            if (lore.length > 0) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(colorize(line));
                }
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }
        
        items.put(slot, item);
    }

    /**
     * Установить предмет из конфига
     */
    protected void setItemFromConfig(int slot, String configPath) {
        if (plugin == null) return;
        
        try {
            String materialName = plugin.getConfig().getString(configPath + ".material", "STONE");
            String name = plugin.getConfig().getString(configPath + ".name", "&fПредмет");
            List<String> lore = plugin.getConfig().getStringList(configPath + ".lore");
            
            Material material = Material.matchMaterial(materialName.toUpperCase());
            if (material == null) material = Material.STONE;
            
            setItem(slot, material, name, lore.toArray(new String[0]));
        } catch (Exception e) {
            setItem(slot, Material.BARRIER, "&cОшибка конфига");
        }
    }

    /**
     * Очистить слот
     */
    protected void clearItem(int slot) {
        items.remove(slot);
    }

    /**
     * Очистить все слоты
     */
    protected void clearAll() {
        items.clear();
    }

    /**
     * Создать ItemStack программно
     */
    protected ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(colorize(name));
            }
            if (lore.length > 0) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(colorize(line));
                }
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * Заполнить слоты материалом (для фона)
     */
    protected void fill(Material material, String name) {
        for (int i = 0; i < size; i++) {
            if (!items.containsKey(i)) {
                setItem(i, material, name);
            }
        }
    }

    // ===== АБСТРАКТНЫЕ МЕТОДЫ =====

    /**
     * Вызывается при открытии GUI
     * Используй для настройки предметов
     */
    public abstract void onOpen(Player player);

    /**
     * Вызывается при клике на предмет
     * @param slot - номер слота
     * @param item - предмет в слоте
     */
    public abstract void onClick(Player player, int slot, ItemStack item);

    /**
     * Вызывается при закрытии GUI
     */
    public void onClose(Player player) {}

    // ===== ОБРАБОТЧИКИ СОБЫТИЙ =====

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        Inventory topInventory = event.getView().getTopInventory();
        
        if (topInventory == null || topInventory.getHolder() != this) return;
        
        event.setCancelled(true); // Отменяем забирание предметов
        
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= size) return;
        
        ItemStack item = items.get(slot);
        if (item != null) {
            onClick(player, slot, item);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        Inventory topInventory = event.getView().getTopInventory();
        
        if (topInventory != null && topInventory.getHolder() == this) {
            openInventories.remove(player.getUniqueId());
            onClose(player);
        }
    }

    // ===== УТИЛИТЫ =====

    protected String colorize(String msg) {
        return msg == null ? "" : msg.replace("&", "§");
    }

    // ===== ГЕТТЕРЫ =====

    public String getId() { return id; }
    public String getTitle() { return title; }
    public int getSize() { return size; }
    public Map<Integer, ItemStack> getItems() { return Collections.unmodifiableMap(items); }
    public Set<UUID> getOpenPlayers() { return openInventories.keySet(); }
    public boolean isOpen(Player player) { return openInventories.containsKey(player.getUniqueId()); }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
