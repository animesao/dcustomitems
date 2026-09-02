package me.dcplugin.dcustomitems.listeners;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.models.CustomItem;
import me.dcplugin.dcustomitems.utils.ActionParser;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Слушатель триггеров кастомных предметов.
 *
 * Все действия внутри триггеров выполняет единый {@link ActionParser}.
 * Формат строки: "название_триггера:действие" (например "on_kill:heal:20").
 */
public class TriggerListener implements Listener {

    private final Main plugin;
    private final Map<UUID, Long> jumpCooldowns = new WeakHashMap<>();
    private final Map<UUID, Long> actionCooldowns = new WeakHashMap<>();

    // Кулдауны триггеров. Значение = время истечения (System.currentTimeMillis() + мс).
    // Ключи: playerId:itemId:trigger   и   playerId:itemId:cooldown (для cooldown:)
    private final Map<String, Long> triggerCooldowns = new HashMap<>();
    private static final long JUMP_COOLDOWN_MS = 500;

    public TriggerListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player damaged = (Player) event.getEntity();
        checkTriggersForPlayer(damaged, "on_damage_taken", (player, item) -> {
            me.dcplugin.dcustomitems.events.CustomItemDamageTakenEvent ev =
                    new me.dcplugin.dcustomitems.events.CustomItemDamageTakenEvent(player, item, event);
            plugin.getServer().getPluginManager().callEvent(ev);
            return !ev.isCancelled();
        });

        Player damager = null;
        if (event.getDamager() instanceof Player) {
            damager = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile proj = (Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) {
                damager = (Player) proj.getShooter();
            }
        }

        if (damager != null) {
            checkTriggersForPlayer(damager, "on_damage_dealt", (player, item) -> {
                me.dcplugin.dcustomitems.events.CustomItemDamageDealtEvent ev =
                        new me.dcplugin.dcustomitems.events.CustomItemDamageDealtEvent(player, item, event);
                plugin.getServer().getPluginManager().callEvent(ev);
                return !ev.isCancelled();
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        checkTriggersForPlayer(player, "on_death", (victim, item) -> {
            me.dcplugin.dcustomitems.events.CustomItemDeathEvent ev =
                    new me.dcplugin.dcustomitems.events.CustomItemDeathEvent(victim, item, event);
            plugin.getServer().getPluginManager().callEvent(ev);
            return !ev.isCancelled();
        });

        if (player.getKiller() != null) {
            Player killer = player.getKiller();
            checkTriggersForPlayer(killer, "on_kill", (k, item) -> {
                me.dcplugin.dcustomitems.events.CustomItemKillEvent ev =
                        new me.dcplugin.dcustomitems.events.CustomItemKillEvent(k, item, player);
                plugin.getServer().getPluginManager().callEvent(ev);
                return !ev.isCancelled();
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Игнорируем микро-движения (поворот камеры, покачивание)
        double dx = event.getTo().getX() - event.getFrom().getX();
        double dz = event.getTo().getZ() - event.getFrom().getZ();
        double dy = event.getTo().getY() - event.getFrom().getY();
        double horizontalDistSq = dx * dx + dz * dz;

        // Прыжок: движение вверх более чем на 0.25 блока
        // с горизонтальным перемещением более 0.5 блока
        if (dy > 0.25 && horizontalDistSq > 0.25) {
            Long lastJump = jumpCooldowns.get(playerId);
            long now = System.currentTimeMillis();
            if (lastJump != null && (now - lastJump) < JUMP_COOLDOWN_MS) {
                return;
            }
            jumpCooldowns.put(playerId, now);
            checkTriggersForPlayer(player, "on_jump");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
        // Только начало спринта (isSprinting() = true при начале)
        if (!event.isSprinting()) return;
        Player player = event.getPlayer();
        checkTriggersForPlayer(player, "on_sprint");
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        jumpCooldowns.remove(playerId);
        actionCooldowns.remove(playerId);
        // Чистим triggerCooldowns для этого игрока
        triggerCooldowns.keySet().removeIf(key -> key.startsWith(playerId.toString()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        String itemId = plugin.getItemHandler().getCustomItemId(droppedItem);
        if (itemId != null) {
            CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
            if (customItem != null && customItem.hasTriggerActions()) {
                executeTriggerActions(player, "on_drop", customItem);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        ItemStack pickedItem = event.getItem().getItemStack();

        String itemId = plugin.getItemHandler().getCustomItemId(pickedItem);
        if (itemId != null) {
            CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
            if (customItem != null && customItem.hasTriggerActions()) {
                executeTriggerActions(player, "on_pickup", customItem);
            }
        }
    }

    // === ПУБЛИЧНЫЕ МЕТОДЫ (вызываются из PlayerListener и EquippedItemsChecker) ===

    public void executeEquipTriggers(Player player, CustomItem item) {
        if (item != null && item.hasTriggerActions()) {
            executeTriggerActions(player, "on_equip", item);
        }
    }

    public void executeUnequipTriggers(Player player, CustomItem item) {
        if (item != null && item.hasTriggerActions()) {
            executeTriggerActions(player, "on_unequip", item);
        }
    }

    public void executeRightClickTriggers(Player player, CustomItem item) {
        if (item != null && item.hasTriggerActions()) {
            executeTriggerActions(player, "on_click_right", item);
        }
    }

    public void executeLeftClickTriggers(Player player, CustomItem item) {
        if (item != null && item.hasTriggerActions()) {
            executeTriggerActions(player, "on_click_left", item);
        }
    }

    // === ВНУТРЕННЯЯ ЛОГИКА ===

    private void checkTriggersForPlayer(Player player, String trigger) {
        checkTriggersForPlayer(player, trigger, null);
    }

    private void checkTriggersForPlayer(Player player, String trigger, TriggerEventGate gate) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        ItemStack[] armor = player.getInventory().getArmorContents();

        checkItemTriggers(player, mainHand, trigger, gate);
        checkItemTriggers(player, offHand, trigger, gate);
        for (ItemStack armorPiece : armor) {
            checkItemTriggers(player, armorPiece, trigger, gate);
        }
    }

    private void checkItemTriggers(Player player, ItemStack item, String trigger, TriggerEventGate gate) {
        if (item == null || item.getType() == Material.AIR) return;

        String itemId = plugin.getItemHandler().getCustomItemId(item);
        if (itemId == null) return;

        CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
        if (customItem == null || !customItem.hasTriggerActions()) return;

        // Bukkit-событие для сторонних плагинов: отмена пропускает триггеры
        if (gate != null && !gate.check(player, customItem)) return;

        executeTriggerActions(player, trigger, customItem);
    }

    /**
     * Пропускает триггеры предмета, если Bukkit-событие отменено.
     */
    private interface TriggerEventGate {
        /** true = выполнять триггеры. */
        boolean check(Player player, CustomItem item);
    }

    /**
     * Выполняет действия триггера. Формат строки в конфиге:
     * "on_kill:effect:REGENERATION:10:2" — разделяем по первому ":".
     */
    private void executeTriggerActions(Player player, String trigger, CustomItem item) {
        List<String> triggerActions = item.getTriggerActions();
        String playerItemKey = player.getUniqueId() + ":" + item.getId();
        long now = System.currentTimeMillis();

        boolean executed = false;

        for (String action : triggerActions) {
            int firstColon = action.indexOf(':');
            if (firstColon == -1) continue;

            String actionTrigger = action.substring(0, firstColon);
            String actionValue = action.substring(firstColon + 1);

            if (!trigger.equals(actionTrigger)) continue;

            // Псевдо-действие cooldown:МС — устанавливает кулдаун для этого триггера
            if (actionValue.startsWith("cooldown:")) {
                long customCooldown = parseLong(actionValue.substring("cooldown:".length()).trim(), 3000);
                setTriggerExpiry(playerItemKey + ":cooldown", now + customCooldown);
                setTriggerExpiry(playerItemKey + ":" + trigger, now + customCooldown);
                continue;
            }

            // Кулдаун триггера (click-cooldown предмета) между срабатываниями
            long cooldownMs = item.getClickCooldown();
            if (cooldownMs > 0 && isTriggerOnCooldown(playerItemKey + ":" + trigger, now)) {
                continue;
            }

            ActionParser.execute(player, actionValue);
            executed = true;
        }

        // Обновляем кулдаун после выполнения (иначе события ловят повторно)
        if (executed && item.getClickCooldown() > 0) {
            setTriggerExpiry(playerItemKey + ":" + trigger, now + item.getClickCooldown());
        }
    }

    private boolean isTriggerOnCooldown(String key, long now) {
        Long expiry = triggerCooldowns.get(key);
        if (expiry == null) return false;
        if (now >= expiry) {
            triggerCooldowns.remove(key);
            return false;
        }
        return true;
    }

    private void setTriggerExpiry(String key, long expiry) {
        triggerCooldowns.put(key, expiry);
    }

    // === ПУБЛИЧНЫЙ API КУЛДАУНОВ ===

    /**
     * Кулдаун, установленный действием "cooldown:МС" в триггерах предмета.
     */
    public boolean isOnCooldown(Player player, CustomItem item) {
        String triggerKey = player.getUniqueId() + ":" + item.getId() + ":cooldown";
        return isTriggerOnCooldown(triggerKey, System.currentTimeMillis());
    }

    /**
     * Оставшееся время кулдауна предмета (мс) в рамках click-cooldown.
     */
    public long getCooldownRemaining(Player player, CustomItem item) {
        String triggerKey = player.getUniqueId() + ":" + item.getId() + ":cooldown";
        Long expiry = triggerCooldowns.get(triggerKey);
        if (expiry == null) return 0;
        return Math.max(0, expiry - System.currentTimeMillis());
    }

    private long parseLong(String raw, long def) {
        try {
            return Long.parseLong(raw.trim());
        } catch (Exception e) {
            return def;
        }
    }
}