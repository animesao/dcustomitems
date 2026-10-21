package me.dcplugin.dcustomitems.listeners;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.models.CustomItem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class BlockPlaceListener implements Listener {

    private final Main plugin;

    public BlockPlaceListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();

        if (item == null || !item.hasItemMeta()) {
            return;
        }

        String customItemId = plugin.getItemHandler().getCustomItemId(item);

        if (customItemId == null) {
            return;
        }

        // YAML-предмет
        CustomItem customItem = plugin.getItemHandler().getCustomItem(customItemId);
        if (customItem != null) {
            if (!customItem.isPlaceable()) {
                event.setCancelled(true);
                // Сообщение не выводится - просто блокируем размещение
            }
            return;
        }

        // Java API-предмет — раньше проверка placeable делалась только для
        // YAML-предметов, и Java-предметы можно было ставить как блоки.
        if (plugin.getApiItemRegistry() == null) return;
        AbstractCustomItem javaItem = plugin.getApiItemRegistry().getItem(customItemId);
        if (javaItem != null && !javaItem.isPlaceable()) {
            event.setCancelled(true);
        }
    }
}
