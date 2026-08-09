package me.dcplugin.dcustomitems.api;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Слушатель событий, который перенаправляет Bukkit события на AbstractCustomItem.
 */
public class ApiEventListener implements Listener {

    private final ItemRegistry registry;
    private final Map<UUID, Long> lastClicks = new HashMap<>();
    private static final long CLICK_DELAY = 200L;

    public ApiEventListener(ItemRegistry registry) {
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        String id = ItemAPI.getCustomItemId(item);
        if (id == null) return;

        AbstractCustomItem customItem = registry.getItem(id);
        if (customItem == null) return;

        // Кулдаун
        long cooldown = customItem.getClickCooldown();
        if (cooldown > 0) {
            Long lastClick = lastClicks.get(player.getUniqueId());
            if (lastClick != null && (System.currentTimeMillis() - lastClick) < cooldown) {
                return;
            }
            lastClicks.put(player.getUniqueId(), System.currentTimeMillis());
        }

        if (event.getAction().name().contains("RIGHT")) {
            customItem.onRightClick(event, player);
        } else if (event.getAction().name().contains("LEFT")) {
            customItem.onLeftClick(event, player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player damaged = (Player) event.getEntity();

        // onDamageTaken для жертвы
        ItemStack damagedItem = findCustomItem(damaged);
        if (damagedItem != null) {
            AbstractCustomItem item = registry.getItem(ItemAPI.getCustomItemId(damagedItem));
            if (item != null) {
                item.onDamageTaken(event, damaged);
            }
        }

        // onDamageDealt для атакующего
        Player damager = null;
        if (event.getDamager() instanceof Player) {
            damager = (Player) event.getDamager();
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile) {
            org.bukkit.entity.Projectile proj = (org.bukkit.entity.Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) {
                damager = (Player) proj.getShooter();
            }
        }

        if (damager != null) {
            ItemStack damagerItem = damager.getInventory().getItemInMainHand();
            String id = ItemAPI.getCustomItemId(damagerItem);
            if (id != null) {
                AbstractCustomItem item = registry.getItem(id);
                if (item != null) {
                    item.onDamageDealt(event, damager);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // onDeath для умершего
        ItemStack deathItem = findCustomItem(player);
        if (deathItem != null) {
            AbstractCustomItem item = registry.getItem(ItemAPI.getCustomItemId(deathItem));
            if (item != null) {
                item.onDeath(player, event);
            }
        }

        // onKill для убийцы
        if (player.getKiller() != null) {
            ItemStack killerItem = player.getKiller().getInventory().getItemInMainHand();
            String id = ItemAPI.getCustomItemId(killerItem);
            if (id != null) {
                AbstractCustomItem item = registry.getItem(id);
                if (item != null) {
                    item.onKill(player.getKiller(), player);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        ItemStack item = findCustomItem(player);
        if (item == null) return;

        String id = ItemAPI.getCustomItemId(item);
        if (id == null) return;

        AbstractCustomItem customItem = registry.getItem(id);
        if (customItem == null) return;

        customItem.onMove(event, player);

        // Проверка прыжка
        if (event.getFrom().getY() < event.getTo().getY()) {
            customItem.onJump(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        String id = ItemAPI.getCustomItemId(item);
        if (id == null) return;

        AbstractCustomItem customItem = registry.getItem(id);
        if (customItem != null) {
            customItem.onBlockBreak(player, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack dropped = event.getItemDrop().getItemStack();
        String id = ItemAPI.getCustomItemId(dropped);
        if (id == null) return;

        AbstractCustomItem customItem = registry.getItem(id);
        if (customItem != null) {
            customItem.onDrop(player, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (item == null) return;

        String id = ItemAPI.getCustomItemId(item);
        if (id == null) return;

        AbstractCustomItem customItem = registry.getItem(id);
        if (customItem != null) {
            customItem.onSlotChange(event, player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();
        String id = ItemAPI.getCustomItemId(main);
        if (id == null) return;

        AbstractCustomItem customItem = registry.getItem(id);
        if (customItem != null) {
            customItem.onSwapHand(event, player);
        }
    }

    /**
     * Находит кастомный предмет в руках или броне игрока.
     */
    private ItemStack findCustomItem(Player player) {
        // Основная рука
        ItemStack main = player.getInventory().getItemInMainHand();
        if (ItemAPI.isCustomItem(main)) return main;

        // Вторая рука
        ItemStack off = player.getInventory().getItemInOffHand();
        if (ItemAPI.isCustomItem(off)) return off;

        // Броня
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (ItemAPI.isCustomItem(armor)) return armor;
        }

        return null;
    }
}
