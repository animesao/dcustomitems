package me.dcplugin.dcustomitems.listeners;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.models.CustomItem;
import me.dcplugin.dcustomitems.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TriggerListener implements Listener {

    private final Main plugin;
    private final Map<UUID, Long> jumpCooldowns = new HashMap<>();
    private final Map<UUID, Long> actionCooldowns = new HashMap<>();
    // Кулдауны для триггеров: playerId:itemId -> lastTriggerTime
    private final Map<String, Long> triggerCooldowns = new HashMap<>();
    private static final long JUMP_COOLDOWN_MS = 500;

    public TriggerListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player damaged = (Player) event.getEntity();
        checkTriggersForPlayer(damaged, "on_damage_taken");

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
            checkTriggersForPlayer(damager, "on_damage_dealt");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        checkTriggersForPlayer(player, "on_death");

        if (player.getKiller() != null) {
            checkTriggersForPlayer(player.getKiller(), "on_kill");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (event.getFrom().getY() < event.getTo().getY()) {
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

    // === ПУБЛИЧНЫЕ МЕТОДЫ ДЛЯ ВЫЗОВА ИЗ DRAGOLISTENER ===

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

    private void checkTriggersForPlayer(Player player, String trigger) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        ItemStack[] armor = player.getInventory().getArmorContents();

        checkItemTriggers(player, mainHand, trigger);
        checkItemTriggers(player, offHand, trigger);
        for (ItemStack armorPiece : armor) {
            checkItemTriggers(player, armorPiece, trigger);
        }
    }

    private void checkItemTriggers(Player player, ItemStack item, String trigger) {
        if (item == null || item.getType() == Material.AIR) return;

        String itemId = plugin.getItemHandler().getCustomItemId(item);
        if (itemId == null) return;

        CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
        if (customItem == null || !customItem.hasTriggerActions()) return;

        executeTriggerActions(player, trigger, customItem);
    }

    private void executeTriggerActions(Player player, String trigger, CustomItem item) {
        List<String> triggerActions = item.getTriggerActions();
        String playerItemKey = player.getUniqueId() + ":" + item.getId();
        
        for (String action : triggerActions) {
            // Формат: "on_kill:effect:REGENERATION:10:2"
            // Разделяем только по первому ":" для триггера
            int firstColon = action.indexOf(':');
            if (firstColon == -1) continue;

            String actionTrigger = action.substring(0, firstColon);
            String actionValue = action.substring(firstColon + 1);

            if (!trigger.equals(actionTrigger)) continue;

            // Проверяем кулдаун триггера
            if (actionValue.startsWith("cooldown:")) {
                executeCooldown(player, item, actionValue);
                continue;
            }

            // Проверяем кулдаун для этого триггера
            String triggerKey = playerItemKey + ":" + trigger;
            Long lastTrigger = triggerCooldowns.get(triggerKey);
            long now = System.currentTimeMillis();
            if (lastTrigger != null && (now - lastTrigger) < item.getClickCooldown()) {
                continue; // Пропускаем если кулдаун активен
            }

            executeAction(player, actionValue);
        }
    }

    private void executeCooldown(Player player, CustomItem item, String actionStr) {
        // Формат: "cooldown:5000" - установить кулдаун 5 секунд
        String cooldownStr = actionStr.replace("cooldown:", "").trim();
        try {
            long cooldownMs = Long.parseLong(cooldownStr);
            String triggerKey = player.getUniqueId() + ":" + item.getId() + ":cooldown";
            triggerCooldowns.put(triggerKey, System.currentTimeMillis());
            // Планируем сброс кулдауна
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                triggerCooldowns.remove(triggerKey);
            }, cooldownMs / 50); // Конвертация мс в тики
        } catch (Exception ignored) {}
    }

    public boolean isOnCooldown(Player player, CustomItem item) {
        String triggerKey = player.getUniqueId() + ":" + item.getId() + ":cooldown";
        Long lastTrigger = triggerCooldowns.get(triggerKey);
        if (lastTrigger == null) return false;
        long now = System.currentTimeMillis();
        return (now - lastTrigger) < item.getClickCooldown();
    }

    public long getCooldownRemaining(Player player, CustomItem item) {
        String triggerKey = player.getUniqueId() + ":" + item.getId() + ":cooldown";
        Long lastTrigger = triggerCooldowns.get(triggerKey);
        if (lastTrigger == null) return 0;
        long now = System.currentTimeMillis();
        long remaining = item.getClickCooldown() - (now - lastTrigger);
        return Math.max(0, remaining);
    }

    private void executeAction(Player player, String actionStr) {
        try {
            if (actionStr.startsWith("lightning")) {
                executeLightning(player, actionStr);
            } else if (actionStr.startsWith("command:")) {
                executeCommand(player, actionStr);
            } else if (actionStr.startsWith("message:")) {
                executeMessage(player, actionStr);
            } else if (actionStr.startsWith("effect:")) {
                executeEffect(player, actionStr);
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
            } else if (actionStr.startsWith("fireworks:")) {
                executeFireworks(player, actionStr);
            } else if (actionStr.startsWith("title:")) {
                executeTitle(player, actionStr);
            } else if (actionStr.startsWith("actionbar:")) {
                executeActionbar(player, actionStr);
            } else if (actionStr.startsWith("exp:")) {
                executeExp(player, actionStr);
            } else if (actionStr.startsWith("give:")) {
                executeGive(player, actionStr);
            } else if (actionStr.startsWith("remove:")) {
                executeRemove(player, actionStr);
            } else if (actionStr.startsWith("announce:")) {
                executeAnnounce(player, actionStr);
            } else if (actionStr.startsWith("sethealth:")) {
                executeSetHealth(player, actionStr);
            } else if (actionStr.startsWith("setfood:")) {
                executeSetFood(player, actionStr);
            } else if (actionStr.startsWith("vanish:")) {
                executeVanish(player, actionStr);
            } else if (actionStr.startsWith("glow:")) {
                executeGlow(player, actionStr);
            } else if (actionStr.startsWith("stun:")) {
                executeStun(player, actionStr);
            } else if (actionStr.startsWith("knockback:")) {
                executeKnockback(player, actionStr);
            } else if (actionStr.startsWith("launch:")) {
                executeLaunch(player, actionStr);
            } else if (actionStr.startsWith("effect-nearby:")) {
                executeEffectNearby(player, actionStr);
            } else if (actionStr.startsWith("damage-mobs:")) {
                executeDamageMobs(player, actionStr);
            } else if (actionStr.startsWith("damage-players:")) {
                executeDamagePlayers(player, actionStr);
            } else if (actionStr.startsWith("knockback-mobs:")) {
                executeKnockbackMobs(player, actionStr);
            } else if (actionStr.startsWith("knockback-players:")) {
                executeKnockbackPlayers(player, actionStr);
            } else if (actionStr.startsWith("launch-mobs:")) {
                executeLaunchMobs(player, actionStr);
            } else if (actionStr.startsWith("launch-players:")) {
                executeLaunchPlayers(player, actionStr);
            } else if (actionStr.startsWith("stun-mobs:")) {
                executeStunMobs(player, actionStr);
            } else if (actionStr.startsWith("stun-players:")) {
                executeStunPlayers(player, actionStr);
            } else if (actionStr.startsWith("effect-nearby-mobs:")) {
                executeEffectNearbyMobs(player, actionStr);
            } else if (actionStr.startsWith("effect-nearby-players:")) {
                executeEffectNearbyPlayers(player, actionStr);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error executing action: " + actionStr + " - " + e.getMessage());
        }
    }

    private void executeStun(Player player, String actionStr) {
        // Формат: "stun:3:4" - оглушить nearby врагов на 3 сек, радиус 4 блока
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
        // Оглушаем ВСЕХ nearby врагов (кроме владельца)
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof org.bukkit.entity.LivingEntity && entity != player) {
                ((org.bukkit.entity.LivingEntity) entity).addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS, duration * 20, 127, false, false));
                ((org.bukkit.entity.LivingEntity) entity).addPotionEffect(new PotionEffect(
                    PotionEffectType.MINING_FATIGUE, duration * 20, 127, false, false));
            }
        }
    }

    private void executeKnockback(Player player, String actionStr) {
        // Формат: "knockback:2" - отбросить врагов nearby
        String kbPart = actionStr.replace("knockback:", "").trim();
        double range = 3.0;
        try {
            range = Double.parseDouble(kbPart);
        } catch (Exception ignored) {}
        // Отбрасываем всех мобов nearby
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
        // Формат: "launch:3" - подбросить nearby врагов вверх
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

    private void executeTitle(Player player, String actionStr) {
        // Формат: "title:Заголовок:Подзаголовок:fadeIn:stay:fadeOut"
        String titlePart = actionStr.replace("title:", "").trim();
        String[] parts = titlePart.split(":");
        if (parts.length >= 1) {
            String title = ColorUtils.colorize(parts[0].replace("%player%", player.getName()));
            String subtitle = parts.length > 1 ? ColorUtils.colorize(parts[1].replace("%player%", player.getName())) : "";
            int fadeIn = parts.length > 2 ? Integer.parseInt(parts[2]) : 10;
            int stay = parts.length > 3 ? Integer.parseInt(parts[3]) : 40;
            int fadeOut = parts.length > 4 ? Integer.parseInt(parts[4]) : 10;
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    private void executeActionbar(Player player, String actionStr) {
        // Формат: "actionbar:Текст"
        String text = actionStr.replace("actionbar:", "").trim();
        text = text.replace("%player%", player.getName());
        try {
            player.spigot().sendMessage(
                net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(ColorUtils.colorize(text))
            );
        } catch (Exception e) {
            player.sendMessage(ColorUtils.colorize(text));
        }
    }

    private void executeExp(Player player, String actionStr) {
        // Формат: "exp:100" или "exp:5:1" (опыт + уровни)
        String expPart = actionStr.replace("exp:", "").trim();
        String[] parts = expPart.split(":");
        if (parts.length >= 1) {
            int exp = Integer.parseInt(parts[0]);
            int levels = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            player.giveExp(exp);
            if (levels > 0) {
                player.giveExpLevels(levels);
            }
        }
    }

    private void executeGive(Player player, String actionStr) {
        // Формат: "give:DIAMOND:5" или "give:DIAMOND"
        String givePart = actionStr.replace("give:", "").trim();
        String[] parts = givePart.split(":");
        if (parts.length >= 1) {
            try {
                Material material = Material.valueOf(parts[0].toUpperCase());
                int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                ItemStack item = new ItemStack(material, amount);
                player.getInventory().addItem(item);
                player.sendMessage(ColorUtils.colorize("&a+" + amount + " " + material.name()));
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid give action: " + actionStr);
            }
        }
    }

    private void executeRemove(Player player, String actionStr) {
        // Формат: "remove:DIAMOND:5"
        String removePart = actionStr.replace("remove:", "").trim();
        String[] parts = removePart.split(":");
        if (parts.length >= 1) {
            try {
                Material material = Material.valueOf(parts[0].toUpperCase());
                int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                int remaining = amount;
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && item.getType() == material && remaining > 0) {
                        int toRemove = Math.min(item.getAmount(), remaining);
                        item.setAmount(item.getAmount() - toRemove);
                        remaining -= toRemove;
                    }
                }
                player.sendMessage(ColorUtils.colorize("&c-" + (amount - remaining) + " " + material.name()));
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid remove action: " + actionStr);
            }
        }
    }

    private void executeAnnounce(Player player, String actionStr) {
        // Формат: "announce:Текст"
        String text = actionStr.replace("announce:", "").trim();
        text = text.replace("%player%", player.getName());
        Bukkit.broadcastMessage(ColorUtils.colorize(text));
    }

    private void executeSetHealth(Player player, String actionStr) {
        // Формат: "sethealth:20"
        String healthPart = actionStr.replace("sethealth:", "").trim();
        try {
            double health = Double.parseDouble(healthPart);
            if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
                double max = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                player.setHealth(Math.min(health, max));
            }
        } catch (Exception ignored) {}
    }

    private void executeSetFood(Player player, String actionStr) {
        // Формат: "setfood:20"
        String foodPart = actionStr.replace("setfood:", "").trim();
        try {
            int food = Integer.parseInt(foodPart);
            player.setFoodLevel(Math.min(food, 20));
        } catch (Exception ignored) {}
    }

    private void executeVanish(Player player, String actionStr) {
        // Формат: "vanish:10" (секунды невидимости)
        String vanishPart = actionStr.replace("vanish:", "").trim();
        int duration = 10;
        try {
            duration = Integer.parseInt(vanishPart);
        } catch (Exception ignored) {}
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.INVISIBILITY, duration * 20, 0, false, false));
    }

    private void executeGlow(Player player, String actionStr) {
        // Формат: "glow:10" (секунды свечения)
        String glowPart = actionStr.replace("glow:", "").trim();
        int duration = 10;
        try {
            duration = Integer.parseInt(glowPart);
        } catch (Exception ignored) {}
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.GLOWING, duration * 20, 0, false, false));
    }

    private void executeLightning(Player player, String actionStr) {
        int strikes = 1;
        String[] parts = actionStr.split(":");
        if (parts.length > 1) {
            try { strikes = Integer.parseInt(parts[1]); } catch (Exception ignored) {}
        }
        for (int i = 0; i < strikes; i++) {
            player.getWorld().strikeLightning(player.getLocation());
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
        player.sendMessage(ColorUtils.colorize(message));
    }

    private void executeEffect(Player player, String actionStr) {
        // Формат: "effect:INCREASE_DAMAGE:10:2" или "effect:SPEED:5:1"
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
            case "SPEED":
            case "HASTE":
                if (effectName.equals("HASTE")) {
                    effectType = PotionEffectType.HASTE;
                } else {
                    effectType = PotionEffectType.SPEED;
                }
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

    private void executeParticle(Player player, String actionStr) {
        String particlePart = actionStr.replace("particle:", "").trim();
        String[] parts = particlePart.split(":");

        if (parts.length < 1) return;

        String particleName = parts[0].toUpperCase();
        int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 10;

        try {
            Particle particle = Particle.valueOf(particleName);
            player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count);
        } catch (Exception e) {
            plugin.getLogger().warning("Unknown particle: " + particleName);
        }
    }

    private void executeSound(Player player, String actionStr) {
        String soundPart = actionStr.replace("sound:", "").trim();
        String[] parts = soundPart.split(":");

        if (parts.length < 1) return;

        String soundName = parts[0].toUpperCase();
        float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
        float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;

        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception e) {
            plugin.getLogger().warning("Unknown sound: " + soundName);
        }
    }

    private void executeHeal(Player player, String actionStr) {
        // Формат: "heal:5" - исцелить на 5 сердец
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
        player.sendMessage(ColorUtils.colorize("&a❤ Вы исцелены на " + (amount / 2) + " сердец!"));
    }

    private void executeTeleport(Player player, String actionStr) {
        // Формат: "teleport:~:~:5:~" - телепортировать относительно
        String tpPart = actionStr.replace("teleport:", "").trim();
        String[] parts = tpPart.split(":");

        Location loc = player.getLocation();
        if (parts.length >= 4) {
            try {
                double x = parts[0].startsWith("~") ? loc.getX() + Double.parseDouble(parts[0].substring(1)) : Double.parseDouble(parts[0]);
                double y = parts[1].startsWith("~") ? loc.getY() + Double.parseDouble(parts[1]) : Double.parseDouble(parts[1]);
                double z = parts[2].startsWith("~") ? loc.getZ() + Double.parseDouble(parts[2]) : Double.parseDouble(parts[2]);
                float yaw = parts.length > 3 ? Float.parseFloat(parts[3]) : loc.getYaw();
                loc = new Location(loc.getWorld(), x, y, z, yaw, loc.getPitch());
                player.teleport(loc);
                player.sendMessage(ColorUtils.colorize("&d✨ Вы телепортированы!"));
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid teleport params: " + tpPart);
            }
        }
    }

    private void executeDamage(Player player, String actionStr) {
        // Формат: "damage:5:3" - AoE урон 5 по площади 3 блока
        // Формат: "damage:5" - AoE урон 5 по площади 3 блока (по умолчанию)
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
        // Наносим урон ВСЕМ nearby врагам (кроме игрока)
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof org.bukkit.entity.LivingEntity && entity != player) {
                ((org.bukkit.entity.LivingEntity) entity).damage(amount, player);
            }
        }
    }

    private void executeEffectNearby(Player player, String actionStr) {
        // Формат: "effect-nearby:SPEED:10:2:4" - эффект nearby врагам
        String effectPart = actionStr.replace("effect-nearby:", "").trim();
        String[] parts = effectPart.split(":");
        if (parts.length < 3) return;
        String effectName = parts[0].toUpperCase();
        int duration = Integer.parseInt(parts[1]);
        int amplifier = Integer.parseInt(parts[2]) - 1;
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

    // === MOB-ONLY METHODS ===
    private void executeDamageMobs(Player player, String actionStr) {
        String dmgPart = actionStr.replace("damage-mobs:", "").trim();
        String[] parts = dmgPart.split(":");
        double amount = 4.0;
        double range = 3.0;
        try {
            amount = Double.parseDouble(parts[0]);
            if (parts.length > 1) range = Double.parseDouble(parts[1]);
        } catch (Exception ignored) {}
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof org.bukkit.entity.LivingEntity && !(entity instanceof Player)) {
                ((org.bukkit.entity.LivingEntity) entity).damage(amount, player);
            }
        }
    }

    private void executeKnockbackMobs(Player player, String actionStr) {
        String kbPart = actionStr.replace("knockback-mobs:", "").trim();
        double range = 3.0;
        try { range = Double.parseDouble(kbPart); } catch (Exception ignored) {}
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof org.bukkit.entity.LivingEntity && !(entity instanceof Player)) {
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

    private void executeLaunchMobs(Player player, String actionStr) {
        String launchPart = actionStr.replace("launch-mobs:", "").trim();
        double power = 1.5;
        try { power = Double.parseDouble(launchPart); } catch (Exception ignored) {}
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(4, 4, 4)) {
            if (entity instanceof org.bukkit.entity.LivingEntity && !(entity instanceof Player)) {
                entity.setVelocity(entity.getVelocity().add(new org.bukkit.util.Vector(0, power, 0)));
            }
        }
    }

    private void executeStunMobs(Player player, String actionStr) {
        String stunPart = actionStr.replace("stun-mobs:", "").trim();
        String[] parts = stunPart.split(":");
        int duration = 3;
        double range = 4.0;
        try {
            duration = Integer.parseInt(parts[0]);
            if (parts.length > 1) range = Double.parseDouble(parts[1]);
        } catch (Exception ignored) {}
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof org.bukkit.entity.LivingEntity && !(entity instanceof Player)) {
                ((org.bukkit.entity.LivingEntity) entity).addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS, duration * 20, 127, false, false));
                ((org.bukkit.entity.LivingEntity) entity).addPotionEffect(new PotionEffect(
                    PotionEffectType.MINING_FATIGUE, duration * 20, 127, false, false));
            }
        }
    }

    private void executeEffectNearbyMobs(Player player, String actionStr) {
        String effectPart = actionStr.replace("effect-nearby-mobs:", "").trim();
        String[] parts = effectPart.split(":");
        if (parts.length < 3) return;
        String effectName = parts[0].toUpperCase();
        int duration = Integer.parseInt(parts[1]);
        int amplifier = Integer.parseInt(parts[2]) - 1;
        double range = parts.length > 3 ? Double.parseDouble(parts[3]) : 4.0;
        PotionEffectType effectType = PotionEffectType.getByName(effectName);
        if (effectType == null) return;
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof org.bukkit.entity.LivingEntity && !(entity instanceof Player)) {
                ((org.bukkit.entity.LivingEntity) entity).addPotionEffect(new PotionEffect(
                    effectType, duration * 20, amplifier, false, false));
            }
        }
    }

    // === PLAYER-ONLY METHODS ===
    private void executeDamagePlayers(Player player, String actionStr) {
        String dmgPart = actionStr.replace("damage-players:", "").trim();
        String[] parts = dmgPart.split(":");
        double amount = 4.0;
        double range = 3.0;
        try {
            amount = Double.parseDouble(parts[0]);
            if (parts.length > 1) range = Double.parseDouble(parts[1]);
        } catch (Exception ignored) {}
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof Player && entity != player) {
                ((Player) entity).damage(amount, player);
            }
        }
    }

    private void executeKnockbackPlayers(Player player, String actionStr) {
        String kbPart = actionStr.replace("knockback-players:", "").trim();
        double range = 3.0;
        try { range = Double.parseDouble(kbPart); } catch (Exception ignored) {}
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof Player && entity != player) {
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

    private void executeLaunchPlayers(Player player, String actionStr) {
        String launchPart = actionStr.replace("launch-players:", "").trim();
        double power = 1.5;
        try { power = Double.parseDouble(launchPart); } catch (Exception ignored) {}
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(4, 4, 4)) {
            if (entity instanceof Player && entity != player) {
                entity.setVelocity(entity.getVelocity().add(new org.bukkit.util.Vector(0, power, 0)));
            }
        }
    }

    private void executeStunPlayers(Player player, String actionStr) {
        String stunPart = actionStr.replace("stun-players:", "").trim();
        String[] parts = stunPart.split(":");
        int duration = 3;
        double range = 4.0;
        try {
            duration = Integer.parseInt(parts[0]);
            if (parts.length > 1) range = Double.parseDouble(parts[1]);
        } catch (Exception ignored) {}
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof Player && entity != player) {
                ((Player) entity).addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS, duration * 20, 127, false, false));
                ((Player) entity).addPotionEffect(new PotionEffect(
                    PotionEffectType.MINING_FATIGUE, duration * 20, 127, false, false));
            }
        }
    }

    private void executeEffectNearbyPlayers(Player player, String actionStr) {
        String effectPart = actionStr.replace("effect-nearby-players:", "").trim();
        String[] parts = effectPart.split(":");
        if (parts.length < 3) return;
        String effectName = parts[0].toUpperCase();
        int duration = Integer.parseInt(parts[1]);
        int amplifier = Integer.parseInt(parts[2]) - 1;
        double range = parts.length > 3 ? Double.parseDouble(parts[3]) : 4.0;
        PotionEffectType effectType = PotionEffectType.getByName(effectName);
        if (effectType == null) return;
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof Player && entity != player) {
                ((Player) entity).addPotionEffect(new PotionEffect(
                    effectType, duration * 20, amplifier, false, false));
            }
        }
    }

    private void executeFireworks(Player player, String actionStr) {
        // Спавн фейерверка
        try {
            org.bukkit.entity.Firework fw = player.getWorld().spawn(player.getLocation().add(0, 2, 0), org.bukkit.entity.Firework.class);
            org.bukkit.inventory.meta.FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(org.bukkit.FireworkEffect.builder()
                .with(org.bukkit.FireworkEffect.Type.BALL_LARGE)
                .withColor(org.bukkit.Color.RED, org.bukkit.Color.ORANGE)
                .withFade(org.bukkit.Color.YELLOW)
                .withFlicker()
                .withTrail()
                .build());
            meta.setPower(1);
            fw.setFireworkMeta(meta);
        } catch (Exception e) {
            plugin.getLogger().warning("Error spawning firework: " + e.getMessage());
        }
    }
}
