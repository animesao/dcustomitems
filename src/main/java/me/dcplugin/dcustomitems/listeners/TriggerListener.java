package me.dcplugin.dcustomitems.listeners;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TriggerListener implements Listener {

    private final Main plugin;
    private final Map<UUID, Long> jumpCooldowns = new HashMap<>();
    private static final long JUMP_COOLDOWN_MS = 500; // 500ms cooldown between jump triggers

    public TriggerListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player damaged = (Player) event.getEntity();

        // Проверяем триггеры на получение урона
        checkTriggersForPlayer(damaged, "on_damage_taken");

        // Проверяем триггеры на нанесение уронa
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

        // Проверяем убийцу
        if (player.getKiller() != null) {
            checkTriggersForPlayer(player.getKiller(), "on_kill");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Проверяем только если игрок реально прыгнул (изменил Y вверх)
        if (event.getFrom().getY() < event.getTo().getY()) {
            // Проверяем кулдаун для прыжков
            Long lastJump = jumpCooldowns.get(playerId);
            long now = System.currentTimeMillis();
            if (lastJump != null && (now - lastJump) < JUMP_COOLDOWN_MS) {
                return;
            }
            jumpCooldowns.put(playerId, now);
            checkTriggersForPlayer(player, "on_jump");
        }
    }

    private void checkTriggersForPlayer(Player player, String trigger) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        ItemStack[] armor = player.getInventory().getArmorContents();

        // Проверяем все экипированные предметы
        checkItemTriggers(player, mainHand, trigger);
        checkItemTriggers(player, offHand, trigger);
        for (ItemStack armorPiece : armor) {
            checkItemTriggers(player, armorPiece, trigger);
        }
    }

    private void checkItemTriggers(Player player, ItemStack item, String trigger) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) return;

        String itemId = plugin.getItemHandler().getCustomItemId(item);
        if (itemId == null) return;

        CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
        if (customItem == null || !customItem.hasTriggerActions()) return;

        List<String> triggerActions = customItem.getTriggerActions();
        for (String action : triggerActions) {
            String[] parts = action.split(":");
            if (parts.length < 2) continue;

            String actionTrigger = parts[0];
            String actionValue = parts.length > 1 ? parts[1] : "";

            if (!trigger.equals(actionTrigger)) continue;

            // Выполняем действие
            executeTriggerAction(player, actionValue, customItem);
        }
    }

    private void executeTriggerAction(Player player, String actionStr, CustomItem item) {
        try {
            if (actionStr.startsWith("lightning")) {
                int strikes = 1;
                String[] parts = actionStr.split(":");
                if (parts.length > 1) {
                    try { strikes = Integer.parseInt(parts[1]); } catch (Exception ignored) {}
                }
                for (int i = 0; i < strikes; i++) {
                    player.getWorld().strikeLightning(player.getLocation());
                }
            } else if (actionStr.startsWith("command:")) {
                String command = actionStr.replace("command:", "").trim();
                command = command.replace("%player%", player.getName());
                org.bukkit.Bukkit.getServer().dispatchCommand(org.bukkit.Bukkit.getServer().getConsoleSender(), command);
            } else if (actionStr.startsWith("message:")) {
                String message = actionStr.replace("message:", "").trim();
                message = message.replace("%player%", player.getName());
                message = me.dcplugin.dcustomitems.utils.ColorUtils.colorize(message);
                player.sendMessage(message);
            } else if (actionStr.startsWith("effect:")) {
                String effectPart = actionStr.replace("effect:", "").trim();
                String[] parts = effectPart.split(":");
                if (parts.length < 2) return;
                String effectName = parts[0];
                int duration = Integer.parseInt(parts[1]);
                int amplifier = parts.length > 2 ? Integer.parseInt(parts[2]) - 1 : 0;
                org.bukkit.potion.PotionEffectType effectType = org.bukkit.potion.PotionEffectType.getByName(effectName);
                if (effectType != null) {
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(effectType, duration * 20, amplifier, false, false));
                }
            } else if (actionStr.startsWith("particle:")) {
                String particlePart = actionStr.replace("particle:", "").trim();
                String[] parts = particlePart.split(":");
                if (parts.length < 1) return;
                String particleName = parts[0];
                int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 10;
                try {
                    org.bukkit.Particle particle = org.bukkit.Particle.valueOf(particleName.toUpperCase());
                    player.getWorld().spawnParticle(particle, player.getLocation(), count);
                } catch (Exception ignored) {}
            } else if (actionStr.startsWith("sound:")) {
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
        } catch (Exception e) {
            plugin.getLogger().warning("Error executing trigger action: " + actionStr);
        }
    }
}
