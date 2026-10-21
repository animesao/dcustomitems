package me.dcplugin.dcustomitems.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранилище кулдаунов игроков (в памяти + на диск в YAML).
 *
 * Ключ хранится как список записей, а не как путь YAML: идентификаторы
 * предметов могут содержать точки ("my.item"), и путь "uuid.my.item"
 * создавал вложенные секции — после загрузки такие кулдауны терялись.
 */
public final class CooldownStore {

    private static final String SEPARATOR = "\u0000";
    private static final String LIST_PATH = "entries";

    private final Map<String, Long> endsAt = new ConcurrentHashMap<>();
    private final File file;

    public CooldownStore(File dataFolder) {
        this.file = new File(dataFolder, "cooldowns.yml");
        load();
    }

    private static String key(String player, String itemId) {
        return player + SEPARATOR + itemId;
    }

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

    public void remove(String player, String itemId) {
        endsAt.remove(key(player, itemId));
    }

    public void save() {
        try {
            org.bukkit.configuration.file.YamlConfiguration yml = new org.bukkit.configuration.file.YamlConfiguration();
            List<Map<String, Object>> entries = new ArrayList<>(endsAt.size());
            for (Map.Entry<String, Long> e : endsAt.entrySet()) {
                String[] parts = e.getKey().split(SEPARATOR, 2);
                if (parts.length != 2) continue;
                Map<String, Object> entry = new java.util.LinkedHashMap<>();
                entry.put("player", parts[0]);
                entry.put("item", parts[1]);
                entry.put("expires", e.getValue());
                entries.add(entry);
            }
            yml.set(LIST_PATH, entries);
            yml.save(file);
        } catch (Exception ignored) {
            // Не срываем игровую логику из-за ошибки сохранения
        }
    }

    private void load() {
        try {
            if (!file.exists()) return;
            org.bukkit.configuration.file.YamlConfiguration yml =
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
            long now = System.currentTimeMillis();

            // Новый формат: список записей
            for (Map<?, ?> entry : yml.getMapList(LIST_PATH)) {
                Object player = entry.get("player");
                Object item = entry.get("item");
                Object expires = entry.get("expires");
                if (!(player instanceof String) || !(item instanceof String)) continue;
                if (!(expires instanceof Number)) continue;
                long end = ((Number) expires).longValue();
                if (end > now) {
                    endsAt.put(key((String) player, (String) item), end);
                }
            }

            // Старый формат: записи в корне YAML по пути "<uuid>.<itemId>".
            // YAML всегда разворачивает такой путь во вложенные секции,
            // поэтому собираем путь обратно: корневой ключ — uuid,
            // вложенные секции — itemId (он может содержать точки).
            for (String rootKey : yml.getKeys(false)) {
                if (LIST_PATH.equals(rootKey)) continue;
                Object value = yml.get(rootKey);
                if (value instanceof Number number) {
                    long end = number.longValue();
                    if (end > now) {
                        int dot = rootKey.indexOf('.');
                        if (dot > 0) {
                            endsAt.put(key(rootKey.substring(0, dot), rootKey.substring(dot + 1)), end);
                        }
                    }
                    continue;
                }
                org.bukkit.configuration.ConfigurationSection section =
                        yml.getConfigurationSection(rootKey);
                if (section != null) {
                    collectLegacyEntries(section, rootKey, "", now);
                }
            }
        } catch (Exception ignored) {
            // Повреждённый файл не должен ломать запуск
        }
    }

    /**
     * Рекурсивно собирает записи старого формата.
     *
     * @param section  текущая секция YAML
     * @param player   UUID игрока (корневой ключ)
     * @param itemPath уже собранная часть пути itemId
     */
    private void collectLegacyEntries(org.bukkit.configuration.ConfigurationSection section,
                                      String player, String itemPath, long now) {
        for (String child : section.getKeys(false)) {
            Object value = section.get(child);
            String path = itemPath.isEmpty() ? child : itemPath + "." + child;
            if (value instanceof Number number) {
                long end = number.longValue();
                if (end > now) {
                    endsAt.put(key(player, path), end);
                }
                continue;
            }
            org.bukkit.configuration.ConfigurationSection nested = section.getConfigurationSection(child);
            if (nested != null) {
                collectLegacyEntries(nested, player, path, now);
            }
        }
    }
}
