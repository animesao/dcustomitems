package me.dcplugin.dcustomitems.listeners;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.handlers.EquippedItemsChecker;
import me.dcplugin.dcustomitems.models.CustomItem;
import me.dcplugin.dcustomitems.utils.ColorUtils;
import me.dcplugin.dcustomitems.utils.EnumCache;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
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
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Objects;

public class PlayerListener implements Listener {

    private final Main plugin;
    private final Map<UUID, Long> lastProcessedClick = new HashMap<>();
    private final Map<UUID, Long> playerCooldowns = new HashMap<>();
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
        Player player = event.getPlayer();
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
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        plugin.getEffectManager().stopPeriodicEffects(player);
        plugin.getAttributeManager().removeAllAttributes(player);
        
        lastProcessedClick.remove(playerId);
        playerCooldowns.remove(playerId);
        getChecker().onPlayerQuit(playerId);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null) return;

        String customItemId = plugin.getItemHandler().getCustomItemId(item);
        if (customItemId == null) return;

        CustomItem customItem = plugin.getItemHandler().getCustomItem(customItemId);
        if (customItem == null) return;

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

        if (customItem.getType().equals("CONSUMABLE")) {
            plugin.getEffectManager().applyEffects(player, customItem);
            actualItem.setAmount(actualItem.getAmount() - 1);
            event.setCancelled(true);
            return;
        }

        if (actions != null && !actions.isEmpty()) {
            Block block = event.getClickedBlock();
            for (String actionStr : actions) {
                executeAction(player, block, actionStr);
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

    private void executeAction(Player player, Block block, String actionStr) {
        try {
            if (actionStr.startsWith("lightning")) {
                executeLightning(player, block, actionStr);
            } else if (actionStr.startsWith("command:")) {
                executeCommand(player, actionStr);
            } else if (actionStr.startsWith("message:")) {
                executeMessage(player, actionStr);
            } else if (actionStr.startsWith("effect:")) {
                executeEffect(player, actionStr);
            } else if (actionStr.startsWith("break")) {
                if (block != null) {
                    breakBlock(block, actionStr);
                }
            } else if (actionStr.startsWith("particle:")) {
                executeParticle(player, actionStr);
            } else if (actionStr.startsWith("sound:")) {
                executeSound(player, actionStr);
            } else if (actionStr.startsWith("heal:")) {
                executeHeal(player, actionStr);
            } else if (actionStr.startsWith("teleport:")) {
                executeTeleport(player, actionStr);
            } else if (actionStr.startsWith("damage:")) {
                executeDamage(player, actionStr);
            } else if (actionStr.startsWith("knockback:")) {
                executeKnockback(player, actionStr);
            } else if (actionStr.startsWith("launch:")) {
                executeLaunch(player, actionStr);
            } else if (actionStr.startsWith("stun:")) {
                executeStun(player, actionStr);
            } else if (actionStr.startsWith("title:")) {
                executeTitle(player, actionStr);
            } else if (actionStr.startsWith("announce:")) {
                executeAnnounce(player, actionStr);
            } else if (actionStr.startsWith("effect-nearby:")) {
                executeEffectNearby(player, actionStr);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error executing action: " + actionStr);
        }
    }

    private void executeHeal(Player player, String actionStr) {
        String healPart = actionStr.replace("heal:", "").trim();
        double amount = 10.0;
        try {
            amount = Double.parseDouble(healPart);
        } catch (Exception ignored) {}
        if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
            double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            double newHealth = Math.min(player.getHealth() + amount, maxHealth);
            player.setHealth(newHealth);
        }
        player.sendMessage(ColorUtils.processMessage(player, "&a❤ Вы исцелены!"));
    }

    private void executeTeleport(Player player, String actionStr) {
        String tpPart = actionStr.replace("teleport:", "").trim();
        String[] parts = tpPart.split(":");
        Location loc = player.getLocation();
        if (parts.length >= 3) {
            try {
                double x = parts[0].startsWith("~") ? loc.getX() + Double.parseDouble(parts[0].substring(1)) : Double.parseDouble(parts[0]);
                double y = parts[1].startsWith("~") ? loc.getY() + Double.parseDouble(parts[1]) : Double.parseDouble(parts[1]);
                double z = parts[2].startsWith("~") ? loc.getZ() + Double.parseDouble(parts[2]) : Double.parseDouble(parts[2]);
                player.teleport(new org.bukkit.Location(loc.getWorld(), x, y, z));
                player.sendMessage(ColorUtils.processMessage(player, "&d✨ Телепортация!"));
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid teleport params: " + tpPart);
            }
        }
    }

    private void executeDamage(Player player, String actionStr) {
        String part = actionStr.replace("damage:", "").trim();
        try {
            double amount = Double.parseDouble(part);
            player.damage(amount);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid damage params: " + part);
        }
    }

    private void executeKnockback(Player player, String actionStr) {
        String part = actionStr.replace("knockback:", "").trim();
        try {
            String[] parts = part.split(":");
            double strength = parts.length > 0 ? Double.parseDouble(parts[0]) : 1.0;
            double height = parts.length > 1 ? Double.parseDouble(parts[1]) : 0.5;
            org.bukkit.util.Vector velocity = player.getLocation().getDirection().multiply(-strength).setY(height);
            player.setVelocity(velocity);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid knockback params: " + part);
        }
    }

    private void executeLaunch(Player player, String actionStr) {
        String part = actionStr.replace("launch:", "").trim();
        try {
            double height = Double.parseDouble(part);
            player.setVelocity(new org.bukkit.util.Vector(0, height, 0));
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid launch params: " + part);
        }
    }

    private void executeStun(Player player, String actionStr) {
        String part = actionStr.replace("stun:", "").trim();
        try {
            long duration = Long.parseLong(part);
            player.setWalkSpeed(0.0f);
            player.setFlySpeed(0.0f);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.setWalkSpeed(0.2f);
                player.setFlySpeed(0.1f);
            }, duration * 20L);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid stun params: " + part);
        }
    }

    private void executeTitle(Player player, String actionStr) {
        String part = actionStr.replace("title:", "").trim();
        String[] parts = part.split("\\|");
        String title = parts.length > 0 ? ColorUtils.processMessage(player, parts[0]) : "";
        String subtitle = parts.length > 1 ? ColorUtils.processMessage(player, parts[1]) : "";
        player.sendTitle(title, subtitle, 10, 40, 10);
    }

    private void executeAnnounce(Player player, String actionStr) {
        String msg = ColorUtils.processMessage(player, actionStr.replace("announce:", "").trim());
        Bukkit.broadcastMessage(msg);
    }

    private void executeEffectNearby(Player player, String actionStr) {
        String part = actionStr.replace("effect-nearby:", "").trim();
        try {
            String[] parts = part.split(":");
            PotionEffectType effectType = EnumCache.getPotionEffect(parts[0]);
            int radius = parts.length > 1 ? Integer.parseInt(parts[1]) : 5;
            int amplifier = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            if (effectType != null) {
                for (org.bukkit.entity.Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                    if (entity instanceof Player) {
                        ((Player) entity).addPotionEffect(new PotionEffect(effectType, 200, amplifier, false, false));
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid effect-nearby params: " + part);
        }
    }

    private void executeLightning(Player player, Block block, String actionStr) {
        if (block != null) {
            block.getWorld().strikeLightningEffect(block.getLocation());
        } else {
            player.getWorld().strikeLightningEffect(player.getLocation());
        }
    }

    private void executeCommand(Player player, String actionStr) {
        String cmd = actionStr.replace("command:", "").trim();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
    }

    private void executeMessage(Player player, String actionStr) {
        String msg = ColorUtils.processMessage(player, actionStr.replace("message:", "").trim());
        player.sendMessage(msg);
    }

    private void executeEffect(Player player, String actionStr) {
        String effectPart = actionStr.replace("effect:", "").trim();
        try {
            String[] parts = effectPart.split(":");
            PotionEffectType effectType = EnumCache.getPotionEffect(parts[0]);
            int duration = parts.length > 1 ? Integer.parseInt(parts[1]) : 60;
            int amplifier = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            if (effectType != null) {
                player.addPotionEffect(new PotionEffect(effectType, duration * 20, amplifier, false, false));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid effect params: " + effectPart);
        }
    }

    private void breakBlock(Block block, String actionStr) {
        if (block.getType() != Material.AIR) {
            block.breakNaturally();
        }
    }

    private void executeParticle(Player player, String actionStr) {
        String particlePart = actionStr.replace("particle:", "").trim();
        try {
            String[] parts = particlePart.split(":");
            Particle particle = EnumCache.getParticle(parts[0]);
            int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 10;
            player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid particle params: " + particlePart);
        }
    }

    private void executeSound(Player player, String actionStr) {
        String soundPart = actionStr.replace("sound:", "").trim();
        try {
            String[] parts = soundPart.split(":");
            Sound sound = EnumCache.getSound(parts[0]);
            float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
            float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid sound params: " + soundPart);
        }
    }

    // ===== Equipment change events → delegate to global checker =====

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            getChecker().markForRecalculation((Player) event.getWhoClicked());
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
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        
        String itemId = plugin.getItemHandler().getCustomItemId(droppedItem);
        if (itemId != null) {
            CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
            if (customItem != null) {
                spawnEquipEffects(player, customItem, false);
            }
        }
        getChecker().markForRecalculation(player);
    }

    @EventHandler
    public void onEntityPickupItem(org.bukkit.event.entity.EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        ItemStack pickedItem = event.getItem().getItemStack();
        
        String itemId = plugin.getItemHandler().getCustomItemId(pickedItem);
        if (itemId != null) {
            CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
            if (customItem != null) {
                spawnEquipEffects(player, customItem, true);
            }
        }
        getChecker().markForRecalculation(player);
    }

    /**
     * Спавнит частицы и звуки для предмета (локально, без пересчёта)
     */
    private void spawnEquipEffects(Player player, CustomItem customItem, boolean isEquip) {
        List<String> particles = isEquip ? customItem.getEquipParticles() : customItem.getUnequipParticles();
        List<String> sounds = isEquip ? customItem.getEquipSounds() : customItem.getUnequipSounds();

        for (String particleStr : particles) {
            try {
                String[] parts = particleStr.split(":");
                Particle particle = EnumCache.getParticle(parts[0]);
                if (particle == null) continue;
                int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 10;
                player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count);
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при спавне частиц: " + particleStr);
            }
        }

        for (String soundStr : sounds) {
            try {
                String[] parts = soundStr.split(":");
                Sound sound = EnumCache.getSound(parts[0]);
                if (sound == null) continue;
                float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
                float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при воспроизведении звука: " + soundStr);
            }
        }
    }
}
