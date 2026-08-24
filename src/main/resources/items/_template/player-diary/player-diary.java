import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.modules.Module;
import me.dcplugin.dcustomitems.api.placeholders.PlaceholderManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

/**
 * 📔 Player Diary Module — Дневник игрока
 *
 * Трекер статистики:
 *   - kills (убийства)
 *   - deaths (смерти)
 *   - playtime (время игры)
 *   - K/D соотношение
 *
 * Команды:
 *   /diary           — открыть GUI дневника
 *   /diary stats     — показать статистику в чате
 *   /diary top       — топ игроков по kills
 *
 * Плейсхолдеры:
 *   %dci_diary_kills%
 *   %dci_diary_deaths%
 *   %dci_diary_playtime%
 *   %dci_diary_kd%
 */
public class playerDiary extends Module implements Listener, CommandExecutor, TabCompleter {

    // Кэш статистики: uuid -> { kills, deaths, playtime (сек), loginTime (мс) }
    private final Map<UUID, PlayerStats> statsCache = new HashMap<>();

    public playerDiary(Main plugin, String id, File folder) {
        super(plugin, id, folder);
    }

    @Override
    protected void onEnable() {
        // Создаём таблицу
        plugin.getDatabaseManager().execute(
            "CREATE TABLE IF NOT EXISTS diary_stats (" +
            "  uuid TEXT PRIMARY KEY," +
            "  kills INTEGER DEFAULT 0," +
            "  deaths INTEGER DEFAULT 0," +
            "  playtime INTEGER DEFAULT 0" +
            ")"
        );

        // Регистрируем команду
        registerCommand("diary", this);

        // Регистрируем слушатель
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Регистрируем плейсхолдеры
        registerPlaceholders();

        // Загружаем данные онлайн-игроков
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadPlayerStats(player.getUniqueId());
        }

        // Периодическое сохранение
        long saveInterval = config.getLong("settings.save-interval", 600);
        Bukkit.getScheduler().runTaskTimer(plugin, this::saveAll, saveInterval, saveInterval);

        plugin.getLogger().info("[Diary] Player Diary module enabled!");
    }

    @Override
    protected void onDisable() {
        org.bukkit.event.HandlerList.unregisterAll(this);
        saveAll();
        statsCache.clear();
        plugin.getLogger().info("[Diary] Player Diary module disabled!");
    }

    // ===== Команды =====

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("diary")) return false;

        if (!(sender instanceof Player)) {
            sender.sendMessage("Только для игроков!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("stats")) {
            showStats(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("top")) {
            showTop(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("gui") || args[0].equalsIgnoreCase("open")) {
            openDiary(player);
            return true;
        }

        String prefix = config.getString("settings.message-prefix", "&6📔 &r");
        player.sendMessage(colorize(prefix + "&7Использование: &e/diary [stats|top|gui]"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("stats", "top", "gui");
        }
        return Collections.emptyList();
    }

    // ===== Статистика =====

    private void showStats(Player player) {
        PlayerStats stats = getStats(player.getUniqueId());
        String prefix = config.getString("settings.message-prefix", "&6📔 &r");

        player.sendMessage(colorize(prefix + "&e━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(colorize(prefix + "&6Статистика: &f" + player.getName()));
        player.sendMessage(colorize(prefix + "&e━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(colorize(prefix + "&7Убийства: &c" + stats.kills));
        player.sendMessage(colorize(prefix + "&7Смерти: &4" + stats.deaths));
        player.sendMessage(colorize(prefix + "&7K/D: &e" + String.format("%.2f", stats.kd)));
        player.sendMessage(colorize(prefix + "&7Время игры: &a" + formatTime(stats.playtime)));
        player.sendMessage(colorize(prefix + "&e━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    private void showTop(Player player) {
        String prefix = config.getString("settings.message-prefix", "&6📔 &r");

        List<Map<String, Object>> results = plugin.getDatabaseManager().queryList(
            "SELECT uuid, kills, deaths FROM diary_stats ORDER BY kills DESC LIMIT 10"
        );

        player.sendMessage(colorize(prefix + "&e━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(colorize(prefix + "&6Топ по убийствам:"));
        player.sendMessage(colorize(prefix + "&e━━━━━━━━━━━━━━━━━━━━━━━━━━"));

        if (results.isEmpty()) {
            player.sendMessage(colorize(prefix + "&7Пока нет данных"));
        } else {
            int rank = 1;
            for (Map<String, Object> row : results) {
                String uuidStr = (String) row.get("uuid");
                int kills = row.get("kills") != null ? ((Number) row.get("kills")).intValue() : 0;
                int deaths = row.get("deaths") != null ? ((Number) row.get("deaths")).intValue() : 0;
                double kd = deaths > 0 ? (double) kills / deaths : kills;

                // Получаем имя игрока
                UUID uuid = UUID.fromString(uuidStr);
                Player target = Bukkit.getPlayer(uuid);
                String name = target != null ? target.getName() : uuid.toString().substring(0, 8);

                player.sendMessage(colorize(prefix + "&e#" + rank + " &f" + name +
                    " &7— &c" + kills + " &7kills &e(K/D: " + String.format("%.2f", kd) + ")"));
                rank++;
            }
        }

        player.sendMessage(colorize(prefix + "&e━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    // ===== GUI =====

    private void openDiary(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, colorize("&6📔 Дневник игрока"));

        // Заполняем фон
        ItemStack glass = new ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < 27; i++) {
            menu.setItem(i, glass);
        }

        PlayerStats stats = getStats(player.getUniqueId());

        // Kills
        ItemStack kills = createMenuItem(org.bukkit.Material.IRON_SWORD,
            "&c⚔ Убийства", "&7Всего: &f" + stats.kills);
        menu.setItem(11, kills);

        // Deaths
        ItemStack deaths = createMenuItem(org.bukkit.Material.WITHER_SKELETON_SKULL,
            "&4💀 Смерти", "&7Всего: &f" + stats.deaths);
        menu.setItem(13, deaths);

        // K/D
        ItemStack kd = createMenuItem(org.bukkit.Material.GOLD_INGOT,
            "&e📊 K/D", "&7Соотношение: &f" + String.format("%.2f", stats.kd));
        menu.setItem(15, kd);

        // Playtime
        ItemStack playtime = createMenuItem(org.bukkit.Material.CLOCK,
            "&a⏰ Время игры", "&7Всего: &f" + formatTime(stats.playtime));
        menu.setItem(22, playtime);

        player.openInventory(menu);
    }

    private ItemStack createMenuItem(org.bukkit.Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize(name));
            meta.setLore(Collections.singletonList(colorize(lore)));
            item.setItemMeta(meta);
        }
        return item;
    }

    // ===== События =====

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Player killer = dead.getKiller();

        // Увеличиваем смерти
        PlayerStats deadStats = getStats(dead.getUniqueId());
        deadStats.deaths++;
        savePlayerStats(dead.getUniqueId());

        // Увеличиваем убийства киллера
        if (killer != null && killer != dead) {
            PlayerStats killerStats = getStats(killer.getUniqueId());
            killerStats.kills++;
            savePlayerStats(killer.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        loadPlayerStats(player.getUniqueId());

        // Устанавливаем время входа
        PlayerStats stats = getStats(player.getUniqueId());
        stats.loginTime = System.currentTimeMillis();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerStats stats = getStats(player.getUniqueId());

        // Добавляем время игры
        if (stats.loginTime > 0) {
            long sessionTime = (System.currentTimeMillis() - stats.loginTime) / 1000;
            stats.playtime += sessionTime;
        }

        savePlayerStats(player.getUniqueId());
        statsCache.remove(player.getUniqueId());
    }

    // ===== База данных =====

    private void loadPlayerStats(UUID uuid) {
        if (statsCache.containsKey(uuid)) return;

        PlayerStats stats = new PlayerStats();
        String uuidStr = uuid.toString();

        try {
            Integer kills = plugin.getDatabaseManager().queryInt(
                "SELECT kills FROM diary_stats WHERE uuid=?", uuidStr);
            Integer deaths = plugin.getDatabaseManager().queryInt(
                "SELECT deaths FROM diary_stats WHERE uuid=?", uuidStr);
            Integer playtime = plugin.getDatabaseManager().queryInt(
                "SELECT playtime FROM diary_stats WHERE uuid=?", uuidStr);

            stats.kills = kills != null ? kills : 0;
            stats.deaths = deaths != null ? deaths : 0;
            stats.playtime = playtime != null ? playtime : 0;
        } catch (Exception e) {
            plugin.getLogger().warning("[Diary] Error loading stats for " + uuidStr + ": " + e.getMessage());
        }

        stats.loginTime = System.currentTimeMillis();
        statsCache.put(uuid, stats);
    }

    private void savePlayerStats(UUID uuid) {
        PlayerStats stats = statsCache.get(uuid);
        if (stats == null) return;

        String uuidStr = uuid.toString();
        try {
            plugin.getDatabaseManager().execute(
                "INSERT OR REPLACE INTO diary_stats (uuid, kills, deaths, playtime) VALUES (?, ?, ?, ?)",
                uuidStr, stats.kills, stats.deaths, stats.playtime
            );
        } catch (Exception e) {
            plugin.getLogger().warning("[Diary] Error saving stats for " + uuidStr + ": " + e.getMessage());
        }
    }

    private void saveAll() {
        for (Map.Entry<UUID, PlayerStats> entry : statsCache.entrySet()) {
            savePlayerStats(entry.getKey());
        }
    }

    private PlayerStats getStats(UUID uuid) {
        return statsCache.computeIfAbsent(uuid, k -> {
            loadPlayerStats(k);
            return statsCache.get(k);
        });
    }

    // ===== Плейсхолдеры =====

    private void registerPlaceholders() {
        PlaceholderManager pm = plugin.getPlaceholderManager();
        if (pm == null) return;

        pm.register("diary_kills", (player) -> {
            return String.valueOf(getStats(player.getUniqueId()).kills);
        });

        pm.register("diary_deaths", (player) -> {
            return String.valueOf(getStats(player.getUniqueId()).deaths);
        });

        pm.register("diary_playtime", (player) -> {
            return formatTime(getStats(player.getUniqueId()).playtime);
        });

        pm.register("diary_kd", (player) -> {
            PlayerStats stats = getStats(player.getUniqueId());
            return String.format("%.2f", stats.kd);
        });
    }

    // ===== Утилиты =====

    private String formatTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return hours + "ч " + minutes + "м";
        } else if (minutes > 0) {
            return minutes + "м " + seconds + "с";
        } else {
            return seconds + "с";
        }
    }

    private String colorize(String msg) {
        return msg == null ? "" : ChatColor.translateAlternateColorCodes('&', msg);
    }

    // ===== Модель данных =====

    private static class PlayerStats {
        int kills = 0;
        int deaths = 0;
        long playtime = 0;
        long loginTime = 0;

        double getKd() {
            return deaths > 0 ? (double) kills / deaths : kills;
        }
    }
}
