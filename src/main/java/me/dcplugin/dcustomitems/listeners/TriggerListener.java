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
        for (String action : triggerActions) {
            // Формат: "on_kill:effect:REGENERATION:10:2"
            // Разделяем только по первому ":" для триггера
            int firstColon = action.indexOf(':');
            if (firstColon == -1) continue;

            String actionTrigger = action.substring(0, firstColon);
            String actionValue = action.substring(firstColon + 1);

            if (!trigger.equals(actionTrigger)) continue;

            executeAction(player, actionValue);
        }
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
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error executing action: " + actionStr + " - " + e.getMessage());
        }
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
        // Формат: "damage:5" - нанести 5 сердец урона
        String dmgPart = actionStr.replace("damage:", "").trim();
        double amount = 4.0;
        try {
            amount = Double.parseDouble(dmgPart);
        } catch (Exception ignored) {}
        player.damage(amount);
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
