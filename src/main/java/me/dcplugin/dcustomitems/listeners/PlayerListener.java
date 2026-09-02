package me.dcplugin.dcustomitems.listeners;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.handlers.EquippedItemsChecker;
import me.dcplugin.dcustomitems.models.CustomItem;
import me.dcplugin.dcustomitems.utils.ActionParser;
import me.dcplugin.dcustomitems.utils.ColorUtils;
import me.dcplugin.dcustomitems.utils.ItemFX;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class PlayerListener implements Listener {

    private final Main plugin;
    private final Map<UUID, Long> lastProcessedClick = new WeakHashMap<>();
    private final Map<UUID, Long> playerCooldowns = new WeakHashMap<>();
    private static final long MIN_CLICK_DELAY = 200L;

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Получить глобальный checker (ленивая инициализация)
     */
    private EquippedItemsChecker getChecker() {
        return plugin.getEquippedItemsChecker();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        // Глобальный таск обработает на следующем тике
        getChecker().markForRecalculation(player);

        if ((player.isOp() || player.hasPermission("customitems.admin") || player.hasPermission("*")) && plugin.getConfig().getBoolean("update-available", false)) {
            String latest = plugin.getConfig().getString("latest-version", "unknown");
            player.sendMessage(plugin.getMessageManager().getMessage("plugin.update-available", "&e[CustomItems] &fДоступна новая версия: &b{version}").replace("{version}", latest));
            player.sendMessage(plugin.getMessageManager().getMessage("plugin.update-download", "&e[CustomItems] &fСкачать обновление на сайте плагина"));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        plugin.getEffectManager().stopPeriodicEffects(player);
        plugin.getAttributeManager().removeAllAttributes(player);

        lastProcessedClick.remove(playerId);
        playerCooldowns.remove(playerId);
        getChecker().onPlayerQuit(playerId);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        String customItemId = plugin.getItemHandler().getCustomItemId(item);
        if (customItemId == null) return;

        CustomItem customItem = plugin.getItemHandler().getCustomItem(customItemId);
        if (customItem == null) return;

        // Право на использование предмета
        if (customItem.hasPermission() && !player.hasPermission(customItem.getPermission())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessageManager().getMessage("items.no-permission",
                "&cУ вас нет прав на использование этого предмета."));
            return;
        }

        if (event.getAction().name().contains("PHYSICAL")) {
            return;
        }

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastClick = lastProcessedClick.get(playerId);

        if (lastClick != null && (currentTime - lastClick) < MIN_CLICK_DELAY) {
            return;
        }

        lastProcessedClick.put(playerId, currentTime);

        boolean isRightClick = event.getAction().name().contains("RIGHT");
        boolean isLeftClick = event.getAction().name().contains("LEFT");

        ItemStack actualItem = item;
        EquipmentSlot hand = event.getHand();
        if (hand == EquipmentSlot.HAND) {
            actualItem = player.getInventory().getItemInMainHand();
        } else if (hand == EquipmentSlot.OFF_HAND) {
            actualItem = player.getInventory().getItemInOffHand();
        }

        List<String> actions = null;
        if (isRightClick) {
            actions = customItem.getRightClickActions();
        } else if (isLeftClick) {
            actions = customItem.getLeftClickActions();
        }

        long cooldown = customItem.getClickCooldown();
        if (cooldown > 0) {
            Long lastClickTime = playerCooldowns.get(playerId);
            if (lastClickTime != null && (currentTime - lastClickTime) < cooldown) {
                long remaining = cooldown - (currentTime - lastClickTime);
                double secondsLeft = remaining / 1000.0;
                if (customItem.hasCooldownMessage()) {
                    String cdMsg = ColorUtils.processMessage(player, customItem.getCooldownMessage().replace("{seconds}", String.format("%.1f", secondsLeft)));
                    if (!cdMsg.trim().isEmpty()) {
                        player.sendMessage(cdMsg);
                    }
                }
                ItemStack updated = plugin.getItemHandler().updateItemWithCooldown(actualItem, lastClickTime, cooldown);
                if (hand == EquipmentSlot.HAND) {
                    player.getInventory().setItemInMainHand(updated);
                } else if (hand == EquipmentSlot.OFF_HAND) {
                    player.getInventory().setItemInOffHand(updated);
                }
                return;
            }
            playerCooldowns.put(playerId, currentTime);
        }

        if (customItem.hasMaxUses()) {
            int currentUses = plugin.getItemHandler().getItemUses(actualItem);
            if (currentUses == -1) {
                currentUses = customItem.getMaxUses();
                plugin.getItemHandler().setItemUses(actualItem, currentUses);
            }

            if (currentUses <= 0) {
                event.setCancelled(true);
                String msg = customItem.hasUsesDepletedMessage()
                    ? ColorUtils.processMessage(player, customItem.getUsesDepletedMessage())
                    : plugin.getMessageManager().getMessage("items.uses-depleted", "&cПредмет использован до конца и пропал!");
                player.sendMessage(msg);
                return;
            }

            plugin.getItemHandler().decrementItemUses(actualItem);

            if (hand == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(actualItem);
            } else if (hand == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(actualItem);
            }

            int newUses = plugin.getItemHandler().getItemUses(actualItem);
            if (newUses <= 0) {
                if (hand == EquipmentSlot.HAND) {
                    player.getInventory().setItemInMainHand(null);
                } else if (hand == EquipmentSlot.OFF_HAND) {
                    player.getInventory().setItemInOffHand(null);
                }

                String msg = customItem.hasUsesDepletedMessage()
                    ? ColorUtils.processMessage(player, customItem.getUsesDepletedMessage())
                    : plugin.getMessageManager().getMessage("items.uses-depleted", "&cПредмет использован до конца и пропал!");
                player.sendMessage(msg);
                return;
            }
        }

        // Событие использования — сторонние плагины могут отменить клик целиком
        me.dcplugin.dcustomitems.events.CustomItemUseEvent useEvent = new me.dcplugin.dcustomitems.events.CustomItemUseEvent(
                player, customItem,
                isRightClick
                        ? me.dcplugin.dcustomitems.events.CustomItemUseEvent.UseType.RIGHT_CLICK
                        : me.dcplugin.dcustomitems.events.CustomItemUseEvent.UseType.LEFT_CLICK);
        plugin.getServer().getPluginManager().callEvent(useEvent);
        if (useEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        if (customItem.getType().equals("CONSUMABLE")) {
            plugin.getEffectManager().applyEffects(player, customItem);
            actualItem.setAmount(actualItem.getAmount() - 1);
            event.setCancelled(true);
            return;
        }

        // Все действия выполняет единый ActionParser
        if (actions != null && !actions.isEmpty()) {
            for (String actionStr : actions) {
                ActionParser.execute(player, actionStr);
            }
            event.setCancelled(true);
        }

        if (isRightClick) {
            plugin.getTriggerListener().executeRightClickTriggers(player, customItem);
        } else if (isLeftClick) {
            plugin.getTriggerListener().executeLeftClickTriggers(player, customItem);
        }

        if (!customItem.getEffects().isEmpty()) {
            plugin.getEffectManager().applyEffects(player, customItem);
        }

        if (cooldown > 0) {
            ItemStack updated = plugin.getItemHandler().updateItemWithCooldown(actualItem, currentTime, cooldown);
            if (hand == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(updated);
            } else if (hand == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(updated);
            }
        }
    }

    // ===== Equipment change events → delegate to global checker =====

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof org.bukkit.entity.Player) {
            getChecker().markForRecalculation((org.bukkit.entity.Player) event.getWhoClicked());
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        getChecker().markForRecalculation(event.getPlayer());
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        getChecker().markForRecalculation(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        String itemId = plugin.getItemHandler().getCustomItemId(droppedItem);
        if (itemId != null) {
            CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
            if (customItem != null) {
                ItemFX.spawnEquipEffects(plugin, player, customItem, false);
            }
        }
        getChecker().markForRecalculation(player);
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getEntity();
        ItemStack pickedItem = event.getItem().getItemStack();

        String itemId = plugin.getItemHandler().getCustomItemId(pickedItem);
        if (itemId != null) {
            CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
            if (customItem != null) {
                // Устанавливаем время приобретения для duration-товаров
                plugin.getItemHandler().ensureAcquiredTime(pickedItem);

                // Проверяем ограничения по миру
                if (!customItem.isAllowedInWorld(player.getWorld().getName())) {
                    event.setCancelled(true);
                    player.sendMessage(me.dcplugin.dcustomitems.utils.ColorUtils.processMessage(player,
                        "&cПредмет " + customItem.getId() + " запрещён в этом мире!"));
                    return;
                }

                ItemFX.spawnEquipEffects(plugin, player, customItem, true);
            }
        }
        getChecker().markForRecalculation(player);
    }
}