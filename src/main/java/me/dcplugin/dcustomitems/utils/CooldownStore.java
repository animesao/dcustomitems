package me.dcplugin.dcustomitems.utils;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownStore {

    private final Map<String, Long> endsAt = new ConcurrentHashMap<>();
    private final File file;

    public CooldownStore(File dataFolder) {
        this.file = new File(dataFolder, "cooldowns.yml");
        load();
    }

    private static String key(String player, String itemId) { return player + "\u0000" + itemId; }

    public boolean isOnCooldown(String player, String itemId, long now) {
        Long end = endsAt.get(key(player, itemId));
        return end != null && end > now;
    }

    public long remaining(String player, String itemId, long now) {
        Long end = endsAt.get(key(player, itemId));
        if (end == null) return 0;
        return Math.max(0, end - now);
    }

    public void put(String player, String itemId, long endsAtMillis) {
        endsAt.put(key(player, itemId), endsAtMillis);
    }

    public void save() {
        try {
            org.bukkit.configuration.file.YamlConfiguration yml = new org.bukkit.configuration.file.YamlConfiguration();
            for (Map.Entry<String, Long> e : endsAt.entrySet()) {
                String[] parts = e.getKey().split("\u0000", 2);
                if (parts.length == 2) yml.set(parts[0] + "." + parts[1], e.getValue());
            }
            yml.save(file);
        } catch (Exception ignored) {}
    }

    private void load() {
        try {
            if (!file.exists()) return;
            org.bukkit.configuration.file.YamlConfiguration yml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
            long now = System.currentTimeMillis();
            for (String player : yml.getKeys(false)) {
                org.bukkit.configuration.ConfigurationSection sec = yml.getConfigurationSection(player);
                if (sec == null) continue;
                for (String item : sec.getKeys(false)) {
                    long end = sec.getLong(item, 0);
                    if (end > now) endsAt.put(key(player, item), end);
                }
            }
        } catch (Exception ignored) {}
    }
}
