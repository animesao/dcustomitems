package me.dcplugin.dcustomitems.api.placeholders;

import me.dcplugin.dcustomitems.Main;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.*;

/**
 * Менеджер плейсхолдеров
 */
public class PlaceholderManager {

    private final Main plugin;
    private final Map<String, Function<Player, String>> placeholders = new HashMap<>();
    private final Map<String, String> staticPlaceholders = new HashMap<>();
    private boolean placeholderApiEnabled = false;

    public PlaceholderManager(Main plugin) {
        this.plugin = plugin;
        registerDefaults();
        checkPlaceholderApi();
    }

    // ===== РЕГИСТРАЦИЯ =====

    public void register(String identifier, Function<Player, String> resolver) {
        placeholders.put(identifier.toLowerCase(), resolver);
    }

    public void registerStatic(String identifier, String value) {
        staticPlaceholders.put(identifier.toLowerCase(), value);
    }

    public void registerAll(Map<String, Function<Player, String>> map) {
        placeholders.putAll(map);
    }

    public void unregister(String identifier) {
        placeholders.remove(identifier.toLowerCase());
        staticPlaceholders.remove(identifier.toLowerCase());
    }

    public void clear() {
        placeholders.clear();
        staticPlaceholders.clear();
    }

    // ===== ЗАМЕНА =====

    public String replace(String text, Player player) {
        if (text == null || player == null) return text;

        // Сначала статические
        for (Map.Entry<String, String> entry : staticPlaceholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
            text = text.replace("%" + entry.getKey() + "%", entry.getValue());
        }

        // Затем динамические
        for (Map.Entry<String, Function<Player, String>> entry : placeholders.entrySet()) {
            try {
                String value = entry.getValue().apply(player);
                if (value != null) {
                    text = text.replace("{" + entry.getKey() + "}", value);
                    text = text.replace("%" + entry.getKey() + "%", value);
                }
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "Error in placeholder " + entry.getKey() + ": " + e.getMessage(), e);
            }
        }

        return text;
    }

    public String replaceStatic(String text) {
        if (text == null) return text;
        for (Map.Entry<String, String> entry : staticPlaceholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
            text = text.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return text;
    }

    // ===== ВСТРОЕННЫЕ ПЛЕЙСХОЛДЕРЫ =====

    private void registerDefaults() {
        register("player", (p) -> p.getName());
        register("player_uuid", (p) -> p.getUniqueId().toString());
        register("display_name", (p) -> p.getDisplayName());
        register("health", (p) -> String.valueOf((int) p.getHealth()));
        register("max_health", (p) -> String.valueOf((int) p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()));
        register("health_percent", (p) -> String.valueOf((int) ((p.getHealth() / p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()) * 100)));
        register("food", (p) -> String.valueOf(p.getFoodLevel()));
        register("saturation", (p) -> String.valueOf((int) p.getSaturation()));
        register("level", (p) -> String.valueOf(p.getLevel()));
        register("exp", (p) -> String.valueOf((int) (p.getExp() * 100)));
        register("x", (p) -> String.valueOf((int) p.getLocation().getX()));
        register("y", (p) -> String.valueOf((int) p.getLocation().getY()));
        register("z", (p) -> String.valueOf((int) p.getLocation().getZ()));
        register("world", (p) -> p.getWorld().getName());
        register("gamemode", (p) -> p.getGameMode().name());
        register("fly_status", (p) -> p.getAllowFlight() ? "on" : "off");
        register("item_in_hand", (p) -> p.getInventory().getItemInMainHand().getType().name());
        register("online", (p) -> String.valueOf(p.getServer().getOnlinePlayers().size()));
        register("max_players", (p) -> String.valueOf(p.getServer().getMaxPlayers()));
    }

    // ===== ПРОВЕРКА PlaceholderAPI =====

    private void checkPlaceholderApi() {
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            placeholderApiEnabled = true;
            plugin.getLogger().info("PlaceholderAPI integration enabled!");
        } catch (ClassNotFoundException e) {
            placeholderApiEnabled = false;
        }
    }

    public boolean isPlaceholderApiEnabled() {
        return placeholderApiEnabled;
    }

    // ===== ГЕТТЕРЫ =====

    public Set<String> getRegisteredPlaceholders() {
        Set<String> all = new HashSet<>(placeholders.keySet());
        all.addAll(staticPlaceholders.keySet());
        return all;
    }

    public boolean hasPlaceholder(String identifier) {
        return placeholders.containsKey(identifier.toLowerCase()) || 
               staticPlaceholders.containsKey(identifier.toLowerCase());
    }
}
