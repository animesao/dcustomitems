package me.dcplugin.dcustomitems.api.commands;

import me.dcplugin.dcustomitems.Main;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.*;

import java.util.*;

/**
 * Команда, загружаемая из YAML конфигурации
 */
public class YamlCommand extends CustomCommand {

    private final Main plugin;
    private final List<String> actions;
    private final int cooldown;
    private final boolean requiresTarget;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public YamlCommand(String name, Map<String, Object> config, Main plugin) {
        super(
            name,
            (String) config.getOrDefault("description", ""),
            (String) config.getOrDefault("usage", "/" + name),
            (String) config.getOrDefault("permission", ""),
            (List<String>) config.getOrDefault("aliases", Collections.emptyList()),
            (String) config.getOrDefault("permission-message", "\u00a7c\u0423 \u0432\u0430\u0441 \u043d\u0435\u0442 \u043f\u0440\u0430\u0432!")
        );
        this.plugin = plugin;
        this.actions = (List<String>) config.getOrDefault("actions", Collections.emptyList());
        this.cooldown = (int) config.getOrDefault("cooldown", 0);
        this.requiresTarget = (boolean) config.getOrDefault("requires-target", false);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Проверка кулдауна
        if (cooldown > 0 && sender instanceof Player) {
            Player player = (Player) sender;
            Long lastUsed = cooldowns.get(player.getUniqueId());
            if (lastUsed != null) {
                long timeLeft = (lastUsed + cooldown * 1000L) - System.currentTimeMillis();
                if (timeLeft > 0) {
                    sendMessage(sender, "\u00a7c\u041f\u043e\u0434\u043e\u0436\u0434\u0438\u0442\u0435 " + (timeLeft / 1000 + 1) + " \u0441\u0435\u043a\u0443\u043d\u0434!");
                    return true;
                }
            }
        }

        // Проверка цели если требуется
        Player target = null;
        if (requiresTarget) {
            target = getTarget(sender, args, 0);
            if (target == null) return true;
        } else if (args.length > 0) {
            target = getTarget(sender, args, 0);
        }

        Player player = (sender instanceof Player) ? (Player) sender : null;

        // Выполняем действия
        for (String action : actions) {
            executeAction(sender, player, target, action);
        }

        // Устанавливаем кулдаун
        if (cooldown > 0 && player != null) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        }

        return true;
    }

    private void executeAction(CommandSender sender, Player player, Player target, String actionStr) {
        String[] parts = actionStr.split(":", 2);
        String action = parts[0].toLowerCase();
        String value = parts.length > 1 ? parts[1] : "";

        // Заменяем плейсхолдеры
        value = replacePlaceholders(value, sender, player, target);

        switch (action) {
            case "heal":
                if (target != null) {
                    double amount = parseDouble(value, 20);
                    double maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getValue();
                    target.setHealth(Math.min(target.getHealth() + amount, maxHealth));
                    if (player != null && target != player) {
                        sendMessage(sender, "\u00a7a\u0412\u044b \u0438\u0441\u0446\u0435\u043b\u0438\u043b\u0438 " + target.getName() + "!");
                    }
                }
                break;

            case "sethealth":
                if (target != null) {
                    double amount = parseDouble(value, 20);
                    target.setHealth(Math.min(amount, target.getAttribute(Attribute.MAX_HEALTH).getValue()));
                }
                break;

            case "effect":
                if (target != null) {
                    String[] effectParts = value.split(",");
                    if (effectParts.length >= 1) {
                        PotionEffectType type = PotionEffectType.getByName(effectParts[0].toUpperCase());
                        if (type != null) {
                            int duration = parseInt(effectParts, 1, 30) * 20;
                            int level = parseInt(effectParts, 2, 0);
                            target.addPotionEffect(new PotionEffect(type, duration, level));
                        }
                    }
                }
                break;

            case "cleareffects":
                if (target != null) {
                    for (PotionEffect effect : target.getActivePotionEffects()) {
                        target.removePotionEffect(effect.getType());
                    }
                }
                break;

            case "particle":
                if (target != null) {
                    String[] particleParts = value.split(",");
                    try {
                        Particle particle = Particle.valueOf(particleParts[0].toUpperCase());
                        int count = parseInt(particleParts, 1, 20);
                        target.getWorld().spawnParticle(particle, target.getLocation().add(0, 1, 0), count);
                    } catch (Exception e) {
                        // Ignore invalid particle
                    }
                }
                break;

            case "sound":
                if (target != null) {
                    String[] soundParts = value.split(",");
                    try {
                        Sound sound = Sound.valueOf(soundParts[0].toUpperCase());
                        float volume = parseFloat(soundParts, 1, 1f);
                        float pitch = parseFloat(soundParts, 2, 1f);
                        target.playSound(target.getLocation(), sound, volume, pitch);
                    } catch (Exception e) {
                        // Ignore invalid sound
                    }
                }
                break;

            case "message":
                if (player != null) {
                    player.sendMessage(colorize(value));
                } else {
                    sendMessage(sender, value);
                }
                break;

            case "broadcast":
                Bukkit.broadcastMessage(colorize(value));
                break;

            case "title":
                if (player != null) {
                    String[] titleParts = value.split("::");
                    String title = titleParts.length > 0 ? titleParts[0] : "";
                    String subtitle = titleParts.length > 1 ? titleParts[1] : "";
                    player.sendTitle(colorize(title), colorize(subtitle), 10, 40, 10);
                }
                break;

            case "teleport":
                if (player != null) {
                    String[] coords = value.split(",");
                    if (coords.length >= 3) {
                        double x = parseDouble(coords[0], player.getLocation().getX());
                        double y = parseDouble(coords[1], player.getLocation().getY());
                        double z = parseDouble(coords[2], player.getLocation().getZ());
                        player.teleport(new Location(player.getWorld(), x, y, z));
                    }
                }
                break;

            case "gamemode":
                if (player != null) {
                    GameMode mode = GameMode.valueOf(value.toUpperCase());
                    player.setGameMode(mode);
                }
                break;

            case "fly":
                if (player != null) {
                    if (value.equalsIgnoreCase("toggle")) {
                        player.setAllowFlight(!player.getAllowFlight());
                        player.setFlying(!player.isFlying());
                    } else {
                        boolean enable = Boolean.parseBoolean(value);
                        player.setAllowFlight(enable);
                        if (!enable) player.setFlying(false);
                    }
                }
                break;

            case "setfood":
                if (player != null) {
                    int food = parseInt(new String[]{value}, 0, 20);
                    player.setFoodLevel(food);
                }
                break;

            case "xp":
                if (player != null) {
                    int amount = parseInt(new String[]{value}, 0, 10);
                    player.giveExpLevels(amount);
                }
                break;

            case "give":
                if (player != null) {
                    String[] giveParts = value.split(",");
                    Material mat = Material.getMaterial(giveParts[0].toUpperCase());
                    if (mat != null) {
                        int amount = parseInt(giveParts, 1, 1);
                        player.getInventory().addItem(new ItemStack(mat, amount));
                    }
                }
                break;

            case "remove":
                if (player != null) {
                    Material mat = Material.getMaterial(value.toUpperCase());
                    if (mat != null) {
                        player.getInventory().remove(mat);
                    }
                }
                break;

            case "cmd":
            case "command":
                Bukkit.dispatchCommand(sender, value);
                break;

            case "console":
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), value);
                break;

            case "fire":
                if (target != null) {
                    int ticks = parseInt(new String[]{value}, 0, 100);
                    target.setFireTicks(ticks);
                }
                break;

            case "log":
                plugin.getLogger().info("[CMD] " + value);
                break;

            default:
                plugin.getLogger().warning("\u041d\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043d\u043e\u0435 \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0435: " + action);
                break;
        }
    }

    private String replacePlaceholders(String text, CommandSender sender, Player player, Player target) {
        if (text == null) return "";
        
        text = text.replace("%player%", sender.getName());
        text = text.replace("%sender%", sender.getName());
        
        if (player != null) {
            text = text.replace("%health%", String.valueOf((int) player.getHealth()));
            text = text.replace("%max_health%", String.valueOf((int) player.getAttribute(Attribute.MAX_HEALTH).getValue()));
            text = text.replace("%food%", String.valueOf(player.getFoodLevel()));
            text = text.replace("%level%", String.valueOf(player.getLevel()));
            text = text.replace("%world%", player.getWorld().getName());
            text = text.replace("%x%", String.valueOf((int) player.getLocation().getX()));
            text = text.replace("%y%", String.valueOf((int) player.getLocation().getY()));
            text = text.replace("%z%", String.valueOf((int) player.getLocation().getZ()));
            text = text.replace("%fly_status%", player.getAllowFlight() ? "\u0432\u043a\u043b" : "\u0432\u044b\u043a\u043b");
            text = text.replace("%gamemode%", player.getGameMode().name());
        }
        
        if (target != null) {
            text = text.replace("%target%", target.getName());
            text = text.replace("%target_health%", String.valueOf((int) target.getHealth()));
        }
        
        return text;
    }

    private double parseDouble(String str, double def) {
        try { return Double.parseDouble(str); } catch (NumberFormatException e) { return def; }
    }

    private double parseDouble(String[] arr, int index, double def) {
        if (arr.length <= index) return def;
        return parseDouble(arr[index], def);
    }

    private int parseInt(String[] arr, int index, int def) {
        if (arr.length <= index) return def;
        try { return Integer.parseInt(arr[index]); } catch (NumberFormatException e) { return def; }
    }

    private float parseFloat(String[] arr, int index, float def) {
        if (arr.length <= index) return def;
        try { return Float.parseFloat(arr[index]); } catch (NumberFormatException e) { return def; }
    }

    public List<String> getActions() { return actions; }
    public int getCooldown() { return cooldown; }
    public boolean isRequiresTarget() { return requiresTarget; }
}
