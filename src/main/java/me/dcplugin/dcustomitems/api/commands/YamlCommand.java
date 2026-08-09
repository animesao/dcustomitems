package me.dcplugin.dcustomitems.api.commands;

import me.dcplugin.dcustomitems.Main;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.*;

import java.util.*;

public class YamlCommand extends CustomCommand {

    private final Main plugin;
    private final List<String> actions;
    private final int cooldown;
    private final boolean requiresTarget;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public YamlCommand(String name, Map<String, Object> config, Main plugin) {
        super();
        this.plugin = plugin;
        setName(name);
        setDescription((String) config.getOrDefault("description", ""));
        setUsage((String) config.getOrDefault("usage", "/" + name));
        setPermission((String) config.getOrDefault("permission", ""));
        setPermissionMessage((String) config.getOrDefault("permission-message", "\u00a7c\u0423 \u0432\u0430\u0441 \u043d\u0435\u0442 \u043f\u0440\u0430\u0432!"));
        this.actions = (List<String>) config.getOrDefault("actions", Collections.emptyList());
        this.cooldown = (int) config.getOrDefault("cooldown", 0);
        this.requiresTarget = (boolean) config.getOrDefault("requires-target", false);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (cooldown > 0 && sender instanceof Player) {
            Player player = (Player) sender;
            Long lastUsed = cooldowns.get(player.getUniqueId());
            if (lastUsed != null) {
                long timeLeft = (lastUsed + cooldown * 1000L) - System.currentTimeMillis();
                if (timeLeft > 0) {
                    msg(sender, "\u00a7c\u041f\u043e\u0434\u043e\u0436\u0434\u0438\u0442\u0435 " + (timeLeft / 1000 + 1) + "s!");
                    return true;
                }
            }
        }

        Player target = null;
        if (requiresTarget) {
            target = getTarget(sender, args, 0);
            if (target == null) return true;
        } else if (args.length > 0) {
            target = getTarget(sender, args, 0);
        }

        Player player = (sender instanceof Player) ? (Player) sender : null;

        for (String action : actions) {
            executeAction(sender, player, target, action);
        }

        if (cooldown > 0 && player != null) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        }

        return true;
    }

    private void executeAction(CommandSender sender, Player player, Player target, String actionStr) {
        String[] parts = actionStr.split(":", 2);
        String action = parts[0].toLowerCase();
        String value = parts.length > 1 ? parts[1] : "";
        value = replacePlaceholders(value, sender, player, target);

        switch (action) {
            case "heal":
                if (target != null) {
                    double amount = parseDouble(value, 20);
                    double max = target.getAttribute(Attribute.MAX_HEALTH).getValue();
                    target.setHealth(Math.min(target.getHealth() + amount, max));
                }
                break;
            case "effect":
                if (target != null) {
                    String[] ep = value.split(",");
                    PotionEffectType type = PotionEffectType.getByName(ep[0].toUpperCase());
                    if (type != null) {
                        int dur = parseInt(ep, 1, 30) * 20;
                        int lvl = parseInt(ep, 2, 0);
                        target.addPotionEffect(new PotionEffect(type, dur, lvl));
                    }
                }
                break;
            case "cleareffects":
                if (target != null) {
                    for (PotionEffect e : target.getActivePotionEffects()) {
                        target.removePotionEffect(e.getType());
                    }
                }
                break;
            case "message":
                if (player != null) player.sendMessage(colorize(value));
                else msg(sender, value);
                break;
            case "broadcast":
                Bukkit.broadcastMessage(colorize(value));
                break;
            case "title":
                if (player != null) {
                    String[] tp = value.split("::");
                    player.sendTitle(colorize(tp[0]), colorize(tp.length > 1 ? tp[1] : ""), 10, 40, 10);
                }
                break;
            case "teleport":
                if (player != null) {
                    String[] c = value.split(",");
                    if (c.length >= 3) {
                        player.teleport(new Location(player.getWorld(),
                            parseDouble(c[0], 0), parseDouble(c[1], 0), parseDouble(c[2], 0)));
                    }
                }
                break;
            case "gamemode":
                if (player != null) player.setGameMode(GameMode.valueOf(value.toUpperCase()));
                break;
            case "fly":
                if (player != null) {
                    if (value.equalsIgnoreCase("toggle")) {
                        player.setAllowFlight(!player.getAllowFlight());
                    } else {
                        player.setAllowFlight(Boolean.parseBoolean(value));
                    }
                }
                break;
            case "give":
                if (player != null) {
                    String[] gp = value.split(",");
                    Material mat = Material.getMaterial(gp[0].toUpperCase());
                    if (mat != null) player.getInventory().addItem(new ItemStack(mat, parseInt(gp, 1, 1)));
                }
                break;
            case "cmd":
            case "console":
                Bukkit.dispatchCommand(sender, value);
                break;
            case "fire":
                if (target != null) target.setFireTicks(parseInt(new String[]{value}, 0, 100));
                break;
        }
    }

    private String replacePlaceholders(String text, CommandSender sender, Player player, Player target) {
        if (text == null) return "";
        text = text.replace("%player%", sender.getName());
        if (player != null) {
            text = text.replace("%health%", String.valueOf((int) player.getHealth()));
            text = text.replace("%food%", String.valueOf(player.getFoodLevel()));
            text = text.replace("%level%", String.valueOf(player.getLevel()));
            text = text.replace("%world%", player.getWorld().getName());
            text = text.replace("%x%", String.valueOf((int) player.getLocation().getX()));
            text = text.replace("%y%", String.valueOf((int) player.getLocation().getY()));
            text = text.replace("%z%", String.valueOf((int) player.getLocation().getZ()));
        }
        if (target != null) text = text.replace("%target%", target.getName());
        return text;
    }

    private double parseDouble(String s, double def) { try { return Double.parseDouble(s); } catch (Exception e) { return def; } }
    private double parseDouble(String[] a, int i, double def) { return a.length > i ? parseDouble(a[i], def) : def; }
    private int parseInt(String[] a, int i, int def) { return a.length > i ? Integer.parseInt(a[i], 10) : def; }
}
