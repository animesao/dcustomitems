package me.dcplugin.dcustomitems.listeners;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.events.CustomItemCraftEvent;
import me.dcplugin.dcustomitems.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Дублирует CustomItemCraftEvent в ОБЫЧНЫЙ верстак.
 *
 * GUI-крафт (/craft, модуль customcraft) стреляет событие сам; здесь
 * ловится крафт в ванильном столе/инвентарном крафте. Отмена
 * CustomItemCraftEvent отменяет и CraftItemEvent — результат не забирается,
 * ингредиенты остаются.
 */
public class CraftItemListener implements Listener {

    private final Main plugin;

    public CraftItemListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType() == org.bukkit.Material.AIR) return;

        String itemId = plugin.getItemHandler().getCustomItemId(result);
        if (itemId == null) return;

        // Ингредиенты могут давать результат с количеством > 1 — инициализируем копию как есть
        CustomItemCraftEvent craftEvent;
        CustomItem yamlItem = plugin.getItemHandler().getCustomItem(itemId);
        if (yamlItem != null) {
            craftEvent = new CustomItemCraftEvent(player, yamlItem, result.clone());
        } else {
            AbstractCustomItem javaItem = plugin.getApiItemRegistry().getItem(itemId);
            if (javaItem == null) return;
            craftEvent = new CustomItemCraftEvent(player, javaItem, result.clone());
        }

        plugin.getServer().getPluginManager().callEvent(craftEvent);
        if (craftEvent.isCancelled()) {
            event.setCancelled(true);
        }
    }
}