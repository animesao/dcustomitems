package me.dcplugin.dcustomitems.listeners;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.models.CustomItem;
import me.dcplugin.dcustomitems.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
    private final Map<UUID, String> lastEquipmentState = new HashMap<>();
    private final Map<UUID, Boolean> processingEquipment = new HashMap<>();
    private final Map<UUID, Set<String>> previousActiveItems = new HashMap<>();
    private static final long MIN_CLICK_DELAY = 200L;

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        checkEquippedItems(player);

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
        lastEquipmentState.remove(playerId);
        processingEquipment.remove(playerId);
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
                // Показываем оставшееся время кулдауна в lore
                long remaining = cooldown - (currentTime - lastClickTime);
                double secondsLeft = remaining / 1000.0;
                // Показываем кулдаун ТОЛЬКО если задан custom cooldown-message
                if (customItem.hasCooldownMessage()) {
                    String cdMsg = ColorUtils.colorize(customItem.getCooldownMessage().replace("{seconds}", String.format("%.1f", secondsLeft)));
                    if (!cdMsg.trim().isEmpty()) {
                        player.sendMessage(cdMsg);
                    }
                }
                // Обновляем lore с кулдауном
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
                    ? ColorUtils.colorize(customItem.getUsesDepletedMessage()) 
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
                    ? ColorUtils.colorize(customItem.getUsesDepletedMessage()) 
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

        // Вызываем триггеры кликов
        if (isRightClick) {
            plugin.getTriggerListener().executeRightClickTriggers(player, customItem);
        } else if (isLeftClick) {
            plugin.getTriggerListener().executeLeftClickTriggers(player, customItem);
        }

        if (!customItem.getEffects().isEmpty()) {
            plugin.getEffectManager().applyEffects(player, customItem);
        }

        // Обновляем lore с кулдауном после использования
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
        player.sendMessage(me.dcplugin.dcustomitems.utils.ColorUtils.colorize("&a❤ Вы исцелены!"));
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
                player.sendMessage(me.dcplugin.dcustomitems.utils.ColorUtils.colorize("&d✨ Телепортация!"));
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid teleport params: " + tpPart);
            }
        }
    }

    private void executeDamage(Player player, String actionStr) {
        // AoE: damage:УРОН:РАДИУС
        String dmgPart = actionStr.replace("damage:", "").trim();
        String[] parts = dmgPart.split(":");
        double amount = 4.0;
        double range = 3.0;
        try {
            amount = Double.parseDouble(parts[0]);
            if (parts.length > 1) {
                range = Double.parseDouble(parts[1]);
            }
        } catch (Exception ignored) {}
        // Бьём ВСЕХ nearby врагов, НЕ игрока
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof org.bukkit.entity.LivingEntity && entity != player) {
                ((org.bukkit.entity.LivingEntity) entity).damage(amount, player);
            }
        }
    }

    private void executeLightning(Player player, Block block, String actionStr) {
        int strikes = 1;
        if (actionStr.contains(":")) {
            try {
                strikes = Integer.parseInt(actionStr.split(":")[1]);
            } catch (Exception ignored) {}
        }

        // Определяем локацию для молнии
        org.bukkit.Location targetLocation;
        if (block != null) {
            // Если есть блок - бьем туда
            targetLocation = block.getLocation();
        } else {
            // Вычисляем точку далеко впереди игрока (куда он смотрит)
            org.bukkit.Location eyeLoc = player.getEyeLocation();
            org.bukkit.util.Vector direction = eyeLoc.getDirection();
            targetLocation = eyeLoc.clone().add(direction.multiply(100));
        }

        for (int i = 0; i < strikes; i++) {
            player.getWorld().strikeLightning(targetLocation);
        }
    }

    private void executeCommand(Player player, String actionStr) {
        String command = actionStr.replace("command:", "").trim();
        command = command.replace("%player%", player.getName());
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
    }

    private void executeMessage(Player player, String actionStr) {
        String message = actionStr.replace("message:", "").trim();
        message = message.replace("%player%", player.getName());
        message = me.dcplugin.dcustomitems.utils.ColorUtils.colorize(message);
        player.sendMessage(message);
    }

    private void executeEffect(Player player, String actionStr) {
        String effectPart = actionStr.replace("effect:", "").trim();
        String[] parts = effectPart.split(":");

        if (parts.length < 2) return;

        String effectName = parts[0].toUpperCase();
        int duration = Integer.parseInt(parts[1]);
        int amplifier = parts.length > 2 ? Integer.parseInt(parts[2]) - 1 : 0;

        // Маппинг кастомных названий
        PotionEffectType effectType = null;
        switch (effectName) {
            case "INCREASE_DAMAGE":
            case "STRENGTH":
                effectType = PotionEffectType.STRENGTH;
                break;
            case "DAMAGE_RESISTANCE":
            case "RESISTANCE":
                effectType = PotionEffectType.RESISTANCE;
                break;
            case "HASTE":
                effectType = PotionEffectType.HASTE;
                break;
            default:
                effectType = PotionEffectType.getByName(effectName);
                break;
        }

        if (effectType != null) {
            PotionEffect effect = new PotionEffect(effectType, duration * 20, amplifier, false, false);
            player.addPotionEffect(effect);
        } else {
            plugin.getLogger().warning("Unknown effect: " + effectName);
        }
    }

    private void breakBlock(Block block, String actionStr) {
        boolean dropItems = !actionStr.contains("nodrop");
        if (!dropItems) {
            block.setType(org.bukkit.Material.AIR);
        } else {
            block.breakNaturally();
        }
    }

    private void executeParticle(Player player, String actionStr) {
        String particlePart = actionStr.replace("particle:", "").trim();
        String[] parts = particlePart.split(":");

        if (parts.length < 1) return;

        String particleName = parts[0];
        int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 10;

        try {
            org.bukkit.Particle particle = org.bukkit.Particle.valueOf(particleName.toUpperCase());
            player.getWorld().spawnParticle(particle, player.getLocation(), count);
        } catch (Exception ignored) {}
    }

    private void executeSound(Player player, String actionStr) {
        String soundPart = actionStr.replace("sound:", "").trim();
        String[] parts = soundPart.split(":");

        if (parts.length < 1) return;

        String soundName = parts[0];
        float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
        float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;

        try {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception ignored) {}
    }

    private void executeKnockback(Player player, String actionStr) {
        // AoE knockback: knockback:РАДИУС
        String kbPart = actionStr.replace("knockback:", "").trim();
        double range = 3.0;
        try {
            range = Double.parseDouble(kbPart);
        } catch (Exception ignored) {}
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof org.bukkit.entity.LivingEntity && entity != player) {
                org.bukkit.Location loc = entity.getLocation();
                org.bukkit.Location playerLoc = player.getLocation();
                double dx = loc.getX() - playerLoc.getX();
                double dz = loc.getZ() - playerLoc.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 0) {
                    double strength = 1.5 / dist;
                    entity.setVelocity(entity.getVelocity().add(new org.bukkit.util.Vector(dx * strength, 0.5, dz * strength)));
                }
            }
        }
    }

    private void executeLaunch(Player player, String actionStr) {
        // AoE launch: launch:СИЛА
        String launchPart = actionStr.replace("launch:", "").trim();
        double power = 1.5;
        try {
            power = Double.parseDouble(launchPart);
        } catch (Exception ignored) {}
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(4, 4, 4)) {
            if (entity instanceof org.bukkit.entity.LivingEntity && entity != player) {
                entity.setVelocity(entity.getVelocity().add(new org.bukkit.util.Vector(0, power, 0)));
            }
        }
    }

    private void executeStun(Player player, String actionStr) {
        // AoE stun: stun:СЕКУНДЫ:РАДИУС
        String stunPart = actionStr.replace("stun:", "").trim();
        String[] parts = stunPart.split(":");
        int duration = 3;
        double range = 4.0;
        try {
            duration = Integer.parseInt(parts[0]);
            if (parts.length > 1) {
                range = Double.parseDouble(parts[1]);
            }
        } catch (Exception ignored) {}
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof org.bukkit.entity.LivingEntity && entity != player) {
                ((org.bukkit.entity.LivingEntity) entity).addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS, duration * 20, 127, false, false));
                ((org.bukkit.entity.LivingEntity) entity).addPotionEffect(new PotionEffect(
                    PotionEffectType.MINING_FATIGUE, duration * 20, 127, false, false));
            }
        }
    }

    private void executeTitle(Player player, String actionStr) {
        // Формат: "title:Заголовок|Подзаголовок|fadeIn|stay|fadeOut"
        // Или: "title:Текст" (только заголовок)
        String titlePart = actionStr.replace("title:", "").trim();
        
        // Используем | как разделитель для title/subtitle
        String[] parts = titlePart.split("\\|");
        
        if (parts.length >= 1) {
            String title = me.dcplugin.dcustomitems.utils.ColorUtils.colorize(parts[0].replace("%player%", player.getName()));
            String subtitle = parts.length > 1 ? me.dcplugin.dcustomitems.utils.ColorUtils.colorize(parts[1].replace("%player%", player.getName())) : "";
            int fadeIn = parts.length > 2 ? parseIntegerOrDefault(parts[2], 10) : 10;
            int stay = parts.length > 3 ? parseIntegerOrDefault(parts[3], 40) : 40;
            int fadeOut = parts.length > 4 ? parseIntegerOrDefault(parts[4], 10) : 10;
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        }
    }
    
    private int parseIntegerOrDefault(String str, int defaultValue) {
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void executeAnnounce(Player player, String actionStr) {
        // announce:Текст
        String text = actionStr.replace("announce:", "").trim();
        text = text.replace("%player%", player.getName());
        Bukkit.broadcastMessage(me.dcplugin.dcustomitems.utils.ColorUtils.colorize(text));
    }

    private void executeEffectNearby(Player player, String actionStr) {
        // effect-nearby:EFFECT:СЕКУНДЫ:УРОВЕНЬ:РАДИУС
        String effectPart = actionStr.replace("effect-nearby:", "").trim();
        String[] parts = effectPart.split(":");
        if (parts.length < 3) return;
        String effectName = parts[0].toUpperCase();
        int duration = Integer.parseInt(parts[1]);
        int amplifier = parts.length > 2 ? Integer.parseInt(parts[2]) - 1 : 0;
        double range = parts.length > 3 ? Double.parseDouble(parts[3]) : 4.0;
        PotionEffectType effectType = PotionEffectType.getByName(effectName);
        if (effectType == null) return;
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof org.bukkit.entity.LivingEntity && entity != player) {
                ((org.bukkit.entity.LivingEntity) entity).addPotionEffect(new PotionEffect(
                    effectType, duration * 20, amplifier, false, false));
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> checkEquippedItems(player), 1L);
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> checkEquippedItems(player), 1L);
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> checkEquippedItems(player), 1L);
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
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> checkEquippedItems(player), 1L);
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
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> checkEquippedItems(player), 1L);
    }

    /**
     * Спавнит частицы и звуки для предмета
     */
    private void spawnEquipEffects(Player player, CustomItem customItem, boolean isEquip) {
        List<String> particles = isEquip ? customItem.getEquipParticles() : customItem.getUnequipParticles();
        List<String> sounds = isEquip ? customItem.getEquipSounds() : customItem.getUnequipSounds();

        // Спавним частицы
        for (String particleStr : particles) {
            try {
                String[] parts = particleStr.split(":");
                String particleName = parts[0].toUpperCase();
                int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 10;
                Particle particle = Particle.valueOf(particleName);
                player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count);
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при спавне частиц: " + particleStr);
            }
        }

        // Воспроизводим звуки
        for (String soundStr : sounds) {
            try {
                String[] parts = soundStr.split(":");
                String soundName = parts[0].toUpperCase();
                float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
                float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
                Sound sound = Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при воспроизведении звука: " + soundStr);
            }
        }
    }

    /**
     * Создает хеш текущего состояния экипировки игрока
     */
    private String createEquipmentHash(Player player) {
        StringBuilder hash = new StringBuilder();
        
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        ItemStack[] armor = player.getInventory().getArmorContents();
        
        // Добавляем ID кастомных предметов в хеш
        hash.append("MAIN:").append(getItemId(mainHand)).append(";");
        hash.append("OFF:").append(getItemId(offHand)).append(";");
        
        String[] armorSlots = {"FEET", "LEGS", "CHEST", "HEAD"};
        for (int i = 0; i < armor.length; i++) {
            hash.append(armorSlots[i]).append(":").append(getItemId(armor[i])).append(";");
        }
        
        return hash.toString();
    }
    
    /**
     * Получает ID кастомного предмета или "null" если это не кастомный предмет
     */
    private String getItemId(ItemStack item) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) {
            return "null";
        }
        String customId = plugin.getItemHandler().getCustomItemId(item);
        return customId != null ? customId : "vanilla";
    }

    private void checkEquippedItems(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Защита от одновременного выполнения
        if (processingEquipment.getOrDefault(playerId, false)) {
            return;
        }
        
        try {
            processingEquipment.put(playerId, true);
            
            // Создаем хеш текущего состояния экипировки
            String currentEquipmentHash = createEquipmentHash(player);
            String lastHash = lastEquipmentState.get(playerId);
            
            // Если экипировка не изменилась, не пересчитываем
            if (Objects.equals(currentEquipmentHash, lastHash)) {
                return;
            }
            
            // Сохраняем новое состояние
            lastEquipmentState.put(playerId, currentEquipmentHash);
            
            // Удаляем старые эффекты и атрибуты
            plugin.getEffectManager().removeAllEffectsFromPlayer(player);
            plugin.getAttributeManager().removeAllAttributes(player);

            Map<PotionEffectType, Integer> totalEffects = new HashMap<>();
            Map<org.bukkit.attribute.Attribute, Double> totalAttributes = new HashMap<>();
            Set<String> activeSets = new HashSet<>();
            Set<String> currentActiveItemIds = new HashSet<>();

            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();
            ItemStack[] armor = player.getInventory().getArmorContents();

            // Проверяем предмет в основной руке
            if (mainHand != null && mainHand.getType() != org.bukkit.Material.AIR) {
                String itemId = plugin.getItemHandler().getCustomItemId(mainHand);
                if (itemId != null) {
                    CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
                    if (customItem != null) {
                        if (plugin.getConfig().getBoolean("debug-mode", false)) {
                            plugin.getLogger().info("Предмет в основной руке: " + itemId + ", activation-slot: " + customItem.getActivationSlot());
                        }
                        if ("HAND".equals(customItem.getActivationSlot())) {
                            plugin.getEffectManager().addEffectsToMap(totalEffects, customItem);
                            plugin.getAttributeManager().addAttributesToMap(totalAttributes, customItem);
                            currentActiveItemIds.add(itemId);
                            if (plugin.getConfig().getBoolean("debug-mode", false)) {
                                plugin.getLogger().info("✅ Активирован предмет в основной руке: " + itemId);
                            }
                        } else {
                            if (plugin.getConfig().getBoolean("debug-mode", false)) {
                                plugin.getLogger().info("❌ Предмет " + itemId + " НЕ активирован в основной руке (требуется " + customItem.getActivationSlot() + ")");
                            }
                        }
                    }
                }
            }

            // Проверяем предмет во второй руке
            if (offHand != null && offHand.getType() != org.bukkit.Material.AIR) {
                String itemId = plugin.getItemHandler().getCustomItemId(offHand);
                if (itemId != null) {
                    CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
                    if (customItem != null) {
                        if (plugin.getConfig().getBoolean("debug-mode", false)) {
                            plugin.getLogger().info("Предмет во второй руке: " + itemId + ", activation-slot: " + customItem.getActivationSlot());
                        }
                        if ("OFFHAND".equals(customItem.getActivationSlot())) {
                            plugin.getEffectManager().addEffectsToMap(totalEffects, customItem);
                            plugin.getAttributeManager().addAttributesToMap(totalAttributes, customItem);
                            currentActiveItemIds.add(itemId);
                            if (plugin.getConfig().getBoolean("debug-mode", false)) {
                                plugin.getLogger().info("✅ Активирован предмет во второй руке: " + itemId);
                            }
                        } else {
                            if (plugin.getConfig().getBoolean("debug-mode", false)) {
                                plugin.getLogger().info("❌ Предмет " + itemId + " НЕ активирован во второй руке (требуется " + customItem.getActivationSlot() + ")");
                            }
                        }
                    }
                }
            }

            // Проверяем броню
            String[] armorSlots = {"FEET", "LEGS", "CHEST", "HEAD"};
            for (int i = 0; i < armor.length; i++) {
                ItemStack armorPiece = armor[i];
                if (armorPiece != null && armorPiece.getType() != org.bukkit.Material.AIR) {
                    String itemId = plugin.getItemHandler().getCustomItemId(armorPiece);
                    if (itemId != null) {
                        CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
                        if (customItem != null && armorSlots[i].equals(customItem.getActivationSlot())) {
                            plugin.getEffectManager().addEffectsToMap(totalEffects, customItem);
                            plugin.getAttributeManager().addAttributesToMap(totalAttributes, customItem);
                            currentActiveItemIds.add(itemId);
                            if (customItem.getArmorSetId() != null) {
                                activeSets.add(customItem.getArmorSetId());
                            }
                        }
                    }
                }
            }

            // Проверяем сет-бонусы
            for (String setId : activeSets) {
                if (plugin.getArmorSetManager().hasFullSet(player, setId)) {
                    plugin.getEffectManager().addSetBonusEffectsToMap(totalEffects, setId);
                }
            }

            // Применяем все эффекты и атрибуты
            plugin.getEffectManager().applyEffectsFromMap(player, totalEffects);
            plugin.getAttributeManager().applyAttributesFromMap(player, totalAttributes);

            // Определяем экипированные и снятые предметы для эффектов
            Set<String> previousIds = previousActiveItems.getOrDefault(playerId, new HashSet<>());
            Set<String> newlyEquipped = new HashSet<>(currentActiveItemIds);
            newlyEquipped.removeAll(previousIds);
            Set<String> newlyUnequipped = new HashSet<>(previousIds);
            newlyUnequipped.removeAll(currentActiveItemIds);                    // Спавним эффекты и вызываем триггеры для экипированных предметов
            for (String itemId : newlyEquipped) {
                CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
                if (customItem != null) {
                    spawnEquipEffects(player, customItem, true);
                    // Показываем сообщение ТОЛЬКО если задан custom equip-message
                    if (customItem.hasEquipMessage()) {
                        String msg = ColorUtils.colorize(customItem.getEquipMessage());
                        if (!msg.trim().isEmpty()) {
                            player.sendMessage(msg);
                        }
                    }
                    // Вызываем триггер on_equip
                    plugin.getTriggerListener().executeEquipTriggers(player, customItem);
                }
            }

            // Спавним эффекты и вызываем триггеры для снятых предметов
            for (String itemId : newlyUnequipped) {
                CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
                if (customItem != null) {
                    spawnEquipEffects(player, customItem, false);
                    // Показываем сообщение ТОЛЬКО если задан custom unequip-message
                    if (customItem.hasUnequipMessage()) {
                        String msg = ColorUtils.colorize(customItem.getUnequipMessage());
                        if (!msg.trim().isEmpty()) {
                            player.sendMessage(msg);
                        }
                    }
                    // Вызываем триггер on_unequip
                    plugin.getTriggerListener().executeUnequipTriggers(player, customItem);
                }
            }

            // Сохраняем текущее состояние
            previousActiveItems.put(playerId, currentActiveItemIds);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при обновлении экипировки для игрока " + player.getName() + ": " + e.getMessage());
        } finally {
            processingEquipment.put(playerId, false);
        }
    }
}
