package me.dcplugin.dcustomitems.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

/**
 * Универсальный парсер действий для кастомных предметов.
 * Поддерживает ВСЕ действия из конфига, без хардкода.
 */
public class ActionParser {

    // Маппинг кастомных названий эффектов на Minecraft
    private static final Map<String, PotionEffectType> EFFECT_MAP = new HashMap<>();
    
    static {
        // Стандартные названия
        EFFECT_MAP.put("SPEED", PotionEffectType.SPEED);
        EFFECT_MAP.put("SLOW", PotionEffectType.SLOWNESS);
        EFFECT_MAP.put("SLOWNESS", PotionEffectType.SLOWNESS);
        EFFECT_MAP.put("HASTE", PotionEffectType.HASTE);
        EFFECT_MAP.put("MINING_FATIGUE", PotionEffectType.MINING_FATIGUE);
        EFFECT_MAP.put("STRENGTH", PotionEffectType.STRENGTH);
        EFFECT_MAP.put("INCREASE_DAMAGE", PotionEffectType.STRENGTH);
        EFFECT_MAP.put("JUMP", PotionEffectType.JUMP_BOOST);
        EFFECT_MAP.put("JUMP_BOOST", PotionEffectType.JUMP_BOOST);
        EFFECT_MAP.put("NAUSEA", PotionEffectType.NAUSEA);
        EFFECT_MAP.put("CONFUSION", PotionEffectType.NAUSEA);
        EFFECT_MAP.put("REGENERATION", PotionEffectType.REGENERATION);
        EFFECT_MAP.put("RESISTANCE", PotionEffectType.RESISTANCE);
        EFFECT_MAP.put("DAMAGE_RESISTANCE", PotionEffectType.RESISTANCE);
        EFFECT_MAP.put("FIRE_RESISTANCE", PotionEffectType.FIRE_RESISTANCE);
        EFFECT_MAP.put("WATER_BREATHING", PotionEffectType.WATER_BREATHING);
        EFFECT_MAP.put("INVISIBILITY", PotionEffectType.INVISIBILITY);
        EFFECT_MAP.put("BLINDNESS", PotionEffectType.BLINDNESS);
        EFFECT_MAP.put("NIGHT_VISION", PotionEffectType.NIGHT_VISION);
        EFFECT_MAP.put("HEALTH_BOOST", PotionEffectType.HEALTH_BOOST);
        EFFECT_MAP.put("ABSORPTION", PotionEffectType.ABSORPTION);
        EFFECT_MAP.put("SATURATION", PotionEffectType.SATURATION);
        EFFECT_MAP.put("GLOWING", PotionEffectType.GLOWING);
        EFFECT_MAP.put("LEVITATION", PotionEffectType.LEVITATION);
        EFFECT_MAP.put("LUCK", PotionEffectType.LUCK);
        EFFECT_MAP.put("UNLUCK", PotionEffectType.UNLUCK);
        EFFECT_MAP.put("SLOW_FALLING", PotionEffectType.SLOW_FALLING);
        EFFECT_MAP.put("CONDUIT_POWER", PotionEffectType.CONDUIT_POWER);
        EFFECT_MAP.put("DOLPHINS_GRACE", PotionEffectType.DOLPHINS_GRACE);
        EFFECT_MAP.put("BAD_OMEN", PotionEffectType.BAD_OMEN);
        EFFECT_MAP.put("HERO_OF_THE_VILLAGE", PotionEffectType.HERO_OF_THE_VILLAGE);
        EFFECT_MAP.put("DARKNESS", PotionEffectType.DARKNESS);
        EFFECT_MAP.put("OOZING", PotionEffectType.OOZING);
        EFFECT_MAP.put("WEAVING", PotionEffectType.WEAVING);
        EFFECT_MAP.put("INFESTED", PotionEffectType.INFESTED);
    }

    /**
     * Парсит и выполняет действие
     * @param player Игрок
     * @param action Строка действия из конфига
     * @return true если действие выполнено
     */
    public static boolean execute(Player player, String action) {
        if (action == null || action.isEmpty()) return false;

        String[] parts = action.split(":", 2);
        String actionType = parts[0].toLowerCase();
        String actionValue = parts.length > 1 ? parts[1] : "";

        try {
            switch (actionType) {
                // === КОММУНИКАЦИЯ ===
                case "message":
                    executeMessage(player, actionValue);
                    return true;
                case "title":
                    executeTitle(player, actionValue);
                    return true;
                case "actionbar":
                    executeActionbar(player, actionValue);
                    return true;
                case "broadcast":
                case "announce":
                    executeBroadcast(actionValue);
                    return true;

                // === ЭФФЕКТЫ ===
                case "effect":
                    executeEffect(player, actionValue);
                    return true;
                case "heal":
                    executeHeal(player, actionValue);
                    return true;
                case "damage":
                    executeDamage(player, actionValue);
                    return true;
                case "damage_nearby":
                    executeDamageNearby(player, actionValue);
                    return true;
                case "damage_mobs":
                    executeDamageMobs(player, actionValue);
                    return true;
                case "damage_players":
                    executeDamagePlayers(player, actionValue);
                    return true;
                case "heal_nearby":
                    executeHealNearby(player, actionValue);
                    return true;

                // === ТЕЛЕПОРТАЦИЯ ===
                case "teleport":
                    executeTeleport(player, actionValue);
                    return true;
                case "teleport_relative":
                    executeTeleportRelative(player, actionValue);
                    return true;

                // === ПРЕДМЕТЫ ===
                case "give":
                    executeGive(player, actionValue);
                    return true;
                case "remove":
                    executeRemove(player, actionValue);
                    return true;
                case "exp":
                    executeExp(player, actionValue);
                    return true;

                // === МИРОВЫЕ ===
                case "lightning":
                    executeLightning(player, actionValue);
                    return true;
                case "lightning_forward":
                    executeLightningForward(player, actionValue);
                    return true;
                case "particles":
                case "particle":
                    executeParticle(player, actionValue);
                    return true;
                case "sound":
                    executeSound(player, actionValue);
                    return true;
                case "fireworks":
                    executeFireworks(player, actionValue);
                    return true;

                // === СТАТУС ===
                case "sethealth":
                    executeSetHealth(player, actionValue);
                    return true;
                case "setfood":
                    executeSetFood(player, actionValue);
                    return true;
                case "set_xp":
                    executeSetXp(player, actionValue);
                    return true;
                case "vanish":
                    executeVanish(player, actionValue);
                    return true;
                case "glow":
                    executeGlow(player, actionValue);
                    return true;
                case "speed":
                    executeSpeed(player, actionValue);
                    return true;
                case "flight":
                    executeFlight(player, actionValue);
                    return true;
                case "knockback":
                    executeKnockback(player, actionValue);
                    return true;
                case "launch":
                    executeLaunch(player, actionValue);
                    return true;
                case "stun":
                    executeStun(player, actionValue);
                    return true;

                // === КОМАНДЫ ===
                case "command":
                    executeCommand(player, actionValue);
                    return true;
                case "console_command":
                    executeConsoleCommand(actionValue);
                    return true;

                default:
                    return false;
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[DC-CustomItems] Error executing action: " + action + " - " + e.getMessage());
            return false;
        }
    }

    // === КОММУНИКАЦИЯ ===

    private static void executeMessage(Player player, String value) {
        if (value.isEmpty()) return;
        value = value.replace("%player%", player.getName());
        player.sendMessage(ColorUtils.colorize(value));
    }

    private static void executeTitle(Player player, String value) {
        String[] parts = value.split(":");
        String title = parts.length > 0 ? ColorUtils.colorize(parts[0].replace("%player%", player.getName())) : "";
        String subtitle = parts.length > 1 ? ColorUtils.colorize(parts[1].replace("%player%", player.getName())) : "";
        int fadeIn = parts.length > 2 ? Integer.parseInt(parts[2]) : 10;
        int stay = parts.length > 3 ? Integer.parseInt(parts[3]) : 40;
        int fadeOut = parts.length > 4 ? Integer.parseInt(parts[4]) : 10;
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    private static void executeActionbar(Player player, String value) {
        if (value.isEmpty()) return;
        value = value.replace("%player%", player.getName());
        try {
            player.spigot().sendMessage(
                net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(ColorUtils.colorize(value))
            );
        } catch (Exception e) {
            player.sendMessage(ColorUtils.colorize(value));
        }
    }

    private static void executeBroadcast(String value) {
        Bukkit.broadcastMessage(ColorUtils.colorize(value));
    }

    // === ЭФФЕКТЫ ===

    private static void executeEffect(Player player, String value) {
        // Формат: EFFECT:SECONDS:LEVEL:AMBIENT:SHOW_PARTICLES
        String[] parts = value.split(":");
        if (parts.length < 2) return;

        String effectName = parts[0].toUpperCase();
        int duration = Integer.parseInt(parts[1]);
        int amplifier = parts.length > 2 ? Integer.parseInt(parts[2]) - 1 : 0;
        boolean ambient = parts.length > 3 ? Boolean.parseBoolean(parts[3]) : true;
        boolean showParticles = parts.length > 4 ? Boolean.parseBoolean(parts[4]) : true;

        PotionEffectType effectType = EFFECT_MAP.get(effectName);
        if (effectType == null) {
            effectType = PotionEffectType.getByName(effectName);
        }

        if (effectType != null) {
            player.addPotionEffect(new PotionEffect(effectType, duration * 20, amplifier, ambient, showParticles));
        }
    }

    private static void executeHeal(Player player, String value) {
        double amount = value.isEmpty() ? 10.0 : Double.parseDouble(value);
        if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
            double max = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            player.setHealth(Math.min(player.getHealth() + amount, max));
        }
    }

    private static void executeDamage(Player player, String value) {
        double amount = value.isEmpty() ? 5.0 : Double.parseDouble(value);
        player.damage(amount);
    }

    private static void executeDamageNearby(Player player, String value) {
        // Формат: DAMAGE:RADIUS
        String[] parts = value.split(":");
        double amount = parts.length > 0 ? Double.parseDouble(parts[0]) : 5.0;
        double range = parts.length > 1 ? Double.parseDouble(parts[1]) : 3.0;
        
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof org.bukkit.entity.LivingEntity && entity != player) {
                ((org.bukkit.entity.LivingEntity) entity).damage(amount, player);
            }
        }
    }

    private static void executeDamageMobs(Player player, String value) {
        String[] parts = value.split(":");
        double amount = parts.length > 0 ? Double.parseDouble(parts[0]) : 5.0;
        double range = parts.length > 1 ? Double.parseDouble(parts[1]) : 3.0;
        
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof org.bukkit.entity.LivingEntity && !(entity instanceof Player)) {
                ((org.bukkit.entity.LivingEntity) entity).damage(amount, player);
            }
        }
    }

    private static void executeDamagePlayers(Player player, String value) {
        String[] parts = value.split(":");
        double amount = parts.length > 0 ? Double.parseDouble(parts[0]) : 5.0;
        double range = parts.length > 1 ? Double.parseDouble(parts[1]) : 3.0;
        
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof Player && entity != player) {
                ((Player) entity).damage(amount, player);
            }
        }
    }

    private static void executeHealNearby(Player player, String value) {
        // Формат: HEAL:RADIUS
        String[] parts = value.split(":");
        double amount = parts.length > 0 ? Double.parseDouble(parts[0]) : 10.0;
        double range = parts.length > 1 ? Double.parseDouble(parts[1]) : 5.0;
        
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof Player && entity != player) {
                Player target = (Player) entity;
                if (target.getAttribute(Attribute.MAX_HEALTH) != null) {
                    double max = target.getAttribute(Attribute.MAX_HEALTH).getValue();
                    target.setHealth(Math.min(target.getHealth() + amount, max));
                }
            }
        }
    }

    // === ТЕЛЕПОРТАЦИЯ ===

    private static void executeTeleport(Player player, String value) {
        String[] parts = value.split(":");
        if (parts.length >= 3) {
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            player.teleport(new Location(player.getWorld(), x, y, z));
        }
    }

    private static void executeTeleportRelative(Player player, String value) {
        String[] parts = value.split(":");
        Location loc = player.getLocation();
        double x = parts[0].startsWith("~") ? loc.getX() + Double.parseDouble(parts[0].substring(1)) : Double.parseDouble(parts[0]);
        double y = parts.length > 1 && parts[1].startsWith("~") ? loc.getY() + Double.parseDouble(parts[1].substring(1)) : (parts.length > 1 ? Double.parseDouble(parts[1]) : loc.getY());
        double z = parts.length > 2 && parts[2].startsWith("~") ? loc.getZ() + Double.parseDouble(parts[2].substring(1)) : (parts.length > 2 ? Double.parseDouble(parts[2]) : loc.getZ());
        player.teleport(new Location(player.getWorld(), x, y, z));
    }

    // === ПРЕДМЕТЫ ===

    private static void executeGive(Player player, String value) {
        String[] parts = value.split(":");
        if (parts.length >= 1) {
            Material material = Material.valueOf(parts[0].toUpperCase());
            int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            player.getInventory().addItem(new ItemStack(material, amount));
        }
    }

    private static void executeRemove(Player player, String value) {
        String[] parts = value.split(":");
        if (parts.length >= 1) {
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
        }
    }

    private static void executeExp(Player player, String value) {
        String[] parts = value.split(":");
        int exp = parts.length > 0 ? Integer.parseInt(parts[0]) : 100;
        int levels = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        player.giveExp(exp);
        if (levels > 0) {
            player.giveExpLevels(levels);
        }
    }

    // === МИРОВЫЕ ===

    private static void executeLightning(Player player, String value) {
        int strikes = value.isEmpty() ? 1 : Integer.parseInt(value);
        for (int i = 0; i < strikes; i++) {
            player.getWorld().strikeLightning(player.getLocation());
        }
    }

    private static void executeLightningForward(Player player, String value) {
        int distance = value.isEmpty() ? 100 : Integer.parseInt(value);
        Location eyeLoc = player.getEyeLocation();
        org.bukkit.util.Vector direction = eyeLoc.getDirection();
        Location targetLocation = eyeLoc.clone().add(direction.multiply(distance));
        player.getWorld().strikeLightning(targetLocation);
    }

    private static void executeParticle(Player player, String value) {
        String[] parts = value.split(":");
        if (parts.length < 1) return;
        
        Particle particle = Particle.valueOf(parts[0].toUpperCase());
        int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 10;
        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count);
    }

    private static void executeSound(Player player, String value) {
        String[] parts = value.split(":");
        if (parts.length < 1) return;
        
        Sound sound = Sound.valueOf(parts[0].toUpperCase());
        float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
        float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private static void executeFireworks(Player player, String value) {
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
            Bukkit.getLogger().warning("Error spawning firework: " + e.getMessage());
        }
    }

    // === СТАТУС ===

    private static void executeSetHealth(Player player, String value) {
        double health = Double.parseDouble(value);
        if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
            double max = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            player.setHealth(Math.min(health, max));
        }
    }

    private static void executeSetFood(Player player, String value) {
        int food = Integer.parseInt(value);
        player.setFoodLevel(Math.min(food, 20));
    }

    private static void executeSetXp(Player player, String value) {
        int xp = Integer.parseInt(value);
        player.setExp(Math.min(xp / 1000.0f, 1.0f));
    }

    private static void executeVanish(Player player, String value) {
        int duration = value.isEmpty() ? 30 : Integer.parseInt(value);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration * 20, 0, false, false));
    }

    private static void executeGlow(Player player, String value) {
        int duration = value.isEmpty() ? 10 : Integer.parseInt(value);
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration * 20, 0, false, false));
    }

    private static void executeSpeed(Player player, String value) {
        // Формат: SECONDS:LEVEL
        String[] parts = value.split(":");
        int duration = parts.length > 0 ? Integer.parseInt(parts[0]) : 30;
        int level = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration * 20, level - 1, false, false));
    }

    private static void executeFlight(Player player, String value) {
        boolean enable = value.isEmpty() || Boolean.parseBoolean(value);
        player.setAllowFlight(enable);
        player.setFlying(enable);
    }

    private static void executeKnockback(Player player, String value) {
        double range = value.isEmpty() ? 3.0 : Double.parseDouble(value);
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

    private static void executeLaunch(Player player, String value) {
        double power = value.isEmpty() ? 1.5 : Double.parseDouble(value);
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(4, 4, 4)) {
            if (entity instanceof org.bukkit.entity.LivingEntity && entity != player) {
                entity.setVelocity(entity.getVelocity().add(new org.bukkit.util.Vector(0, power, 0)));
            }
        }
    }

    private static void executeStun(Player player, String value) {
        // Формат: SECONDS:RADIUS
        String[] parts = value.split(":");
        int duration = parts.length > 0 ? Integer.parseInt(parts[0]) : 3;
        double range = parts.length > 1 ? Double.parseDouble(parts[1]) : 4.0;
        
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof org.bukkit.entity.LivingEntity && entity != player) {
                ((org.bukkit.entity.LivingEntity) entity).addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS, duration * 20, 127, false, false));
                ((org.bukkit.entity.LivingEntity) entity).addPotionEffect(new PotionEffect(
                    PotionEffectType.MINING_FATIGUE, duration * 20, 127, false, false));
            }
        }
    }

    // === КОМАНДЫ ===

    private static void executeCommand(Player player, String value) {
        String command = value.replace("%player%", player.getName());
        Bukkit.getServer().dispatchCommand(player, command);
    }

    private static void executeConsoleCommand(String value) {
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), value);
    }
}
