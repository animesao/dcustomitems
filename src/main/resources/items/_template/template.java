import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.modules.Module;
import me.dcplugin.dcustomitems.api.placeholders.PlaceholderManager;
import me.dcplugin.dcustomitems.models.CustomItem;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

/**
 * 📦 Template Module — Шаблон модуля DC-CustomItems
 *
 * ==========================================
 * КАК СОЗДАТЬ СВОЙ МОДУЛЬ:
 * ==========================================
 *
 * 1. Скопируйте папку _template/ → items/my-module/
 * 2. Переименуйте template.java → my-module.java
 * 3. В файле замените:
 *    - "templateModule" → "myModule"
 *    - "template" → "my-module" (везде где есть)
 * 4. Реализуйте нужную логику в onEnable/onDisable
 * 5. Выполните /ci reload
 *
 * ==========================================
 * ДОСТУПНЫЕ ХУКИ:
 * ==========================================
 *
 * - onEnable()     — вызывается при включении модуля
 * - onDisable()    — вызывается при выключении модуля
 * - config         — YamlConfiguration из config.yml
 * - itemsConfig    — YamlConfiguration из items.yml
 * - plugin         — главный класс плагина (Main)
 * - id             — ID модуля (имя папки)
 * - folder         — папка модуля
 *
 * ==========================================
 * РЕГИСТРАЦИЯ КОМАНД:
 * ==========================================
 *
 * Вариант 1: Через plugin.yml (рекомендуется)
 *   buy:
 *     description: Buy item
 *     usage: /buy <id>
 *     permission: mymodule.buy
 *
 * Вариант 2: Динамическая регистрация
 *   registerDynamicCommand("buy", this);
 *
 * ==========================================
 * РЕГИСТРАЦИЯ ПЛЕЙСХОЛДЕРОВ:
 * ==========================================
 *
 * plugin.getPlaceholderManager().register("mymodule_stat", (player) -> {
 *     return "value";
 * });
 *
 * Доступно как: %dci_mymodule_stat%
 *
 * ==========================================
 * РЕГИСТРАЦИЯ СЛУШАТЕЛЕЙ:
 * ==========================================
 *
 * plugin.getServer().getPluginManager().registerEvents(this, plugin);
 *
 * ==========================================
 */
public class templateModule extends Module implements Listener, CommandExecutor, TabCompleter {

    // ===== КОНСТРУКТОР (не трогать) =====

    public templateModule(Main plugin, String id, File folder) {
        super(plugin, id, folder);
    }

    // ===== ОСНОВНЫЕ ХУКИ МОДУЛЯ =====

    /**
     * Вызывается при ВКЛЮЧЕНИИ модуля.
     *
     * Здесь регистрируйте:
     * - Команды
     * - Слушатели событий
     * - Плейсхолдеры
     * - Таймеры/задачи
     * - Загрузку данных
     */
    @Override
    protected void onEnable() {
        // Регистрируем команды (базовый класс сам удалит её при disable)
        registerDynamicCommand("mycommand", this, "template.mycommand");

        // Регистрируем слушатель событий
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Регистрируем плейсхолдеры
        registerPlaceholders();

        // Логируем успешную загрузку
        String prefix = config.getString("settings.message-prefix", "&a[Module] &r");
        plugin.getLogger().info("[Template] Module enabled!");
    }

    /**
     * Вызывается при ВЫКЛЮЧЕНИИ модуля.
     *
     * Здесь очищайте:
     * - Отменяйте таймеры
     * - Сохраняйте данные
     * - Закрывайте соединения
     */
    @Override
    protected void onDisable() {
        // Отменяем все listeners этого модуля
        org.bukkit.event.HandlerList.unregisterAll(this);

        plugin.getLogger().info("[Template] Module disabled!");
    }

    // ===== РЕГИСТРАЦИЯ КОМАНД =====

    /**
     * Зарегистрировать команду.
     *
     * Способ 1: Добавить в plugin.yml (лучше):
     *   mycommand:
     *     description: My command
     *     usage: /mycommand <args>
     *     permission: template.mycommand
     *
     * Способ 2: Динамическая регистрация (базовый класс):
     *   registerDynamicCommand("mycommand", this, "template.mycommand");
     * Команда автоматически удаляется при disable модуля.
     */

    // ===== ОБРАБОТКА КОМАНД =====

    /**
     * Обработка команды /mycommand
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        switch (cmdName) {
            case "mycommand":
                return handleMyCommand(sender, args);
            default:
                return false;
        }
    }

    private boolean handleMyCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Только для игроков!");
            return true;
        }

        Player player = (Player) sender;

        // Получаем префикс из конфига
        String prefix = config.getString("settings.message-prefix", "&a[Module] &r");

        if (args.length == 0) {
            player.sendMessage(colorize(prefix + "&7Использование: &e/mycommand <arg>"));
            return true;
        }

        // Ваша логика здесь
        player.sendMessage(colorize(prefix + "&aВыполнили команду с аргументом: &e" + args[0]));
        return true;
    }

    // ===== TAB COMPLETE =====

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Варианты для первого аргумента
            completions.addAll(Arrays.asList("option1", "option2", "option3"));
        }

        return completions;
    }

    // ===== СЛУШАТЕЛИ СОБЫТИЙ =====

    /**
     * Пример: при входе игрока на сервер
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String prefix = config.getString("settings.message-prefix", "&a[Module] &r");

        // Ваша логика
        // player.sendMessage(colorize(prefix + "Добро пожаловать!"));
    }

    /**
     * Пример: при выходе игрока
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Сохраняем данные, чистим кэш и т.д.
    }

    // ===== ПЛЕЙСХОЛДЕРЫ =====

    /**
     * Регистрация плейсхолдеров модуля.
     * Доступны как: %dci_<identifier>%
     */
    private void registerPlaceholders() {
        PlaceholderManager pm = plugin.getPlaceholderManager();
        if (pm == null) return;

        // Пример: %dci_mymodule_stat%
        pm.register("mymodule_stat", (player) -> {
            // Вернуть значение плейсхолдера
            return "42";
        });

        // Пример: %dci_mymodule_balance%
        pm.register("mymodule_balance", (player) -> {
            // Запрос к БД, кэшу и т.д.
            return "1000";
        });
    }

    // ===== РАБОТА С ПРЕДМЕТАМИ =====

    /**
     * Пример: получить кастомный предмет по ID
     */
    private CustomItem getCustomItem(String itemId) {
        return plugin.getItemHandler().getCustomItem(itemId);
    }

    /**
     * Пример: выдать предмет игроку
     */
    private void giveItem(Player player, String itemId, int amount) {
        CustomItem item = getCustomItem(itemId);
        if (item == null) return;

        ItemStack stack = item.getItemStack().clone();
        stack.setAmount(amount);
        plugin.getItemHandler().updateItemWithUses(stack);
        player.getInventory().addItem(stack);
    }

    /**
     * Пример: проверить есть ли предмет у игрока
     */
    private boolean hasItem(Player player, String itemId) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && itemId.equals(plugin.getItemHandler().getCustomItemId(item))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Пример: забрать предмет у игрока
     */
    private boolean removeItem(Player player, String itemId, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (remaining <= 0) break;
            if (item != null && itemId.equals(plugin.getItemHandler().getCustomItemId(item))) {
                int toRemove = Math.min(item.getAmount(), remaining);
                item.setAmount(item.getAmount() - toRemove);
                remaining -= toRemove;
            }
        }
        return remaining == 0;
    }

    // ===== РАБОТА С БАЗОЙ ДАННЫХ =====

    /**
     * Пример: запрос к БД
     */
    private void databaseExample() {
        if (plugin.getDatabaseManager() == null) return;

        // Создать таблицу
        plugin.getDatabaseManager().execute(
            "CREATE TABLE IF NOT EXISTS my_data (uuid TEXT, key TEXT, value TEXT)"
        );

        // Вставить данные
        plugin.getDatabaseManager().execute(
            "INSERT INTO my_data (uuid, key, value) VALUES (?, ?, ?)",
            "player-uuid", "mykey", "myvalue"
        );

        // Получить данные
        String value = plugin.getDatabaseManager().queryString(
            "SELECT value FROM my_data WHERE uuid=? AND key=?",
            "player-uuid", "mykey"
        );
    }

    // ===== GUI / МЕНЮ =====

    /**
     * Пример: создать GUI-меню
     */
    private void openMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, colorize("&6My Menu"));

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

        // Добавляем предметы
        ItemStack diamond = new ItemStack(org.bukkit.Material.DIAMOND);
        ItemMeta diamondMeta = diamond.getItemMeta();
        if (diamondMeta != null) {
            diamondMeta.setDisplayName(colorize("&bDiamond"));
            diamond.setItemMeta(diamondMeta);
        }
        menu.setItem(13, diamond);

        player.openInventory(menu);
    }

    // ===== ТАЙМЕРЫ / ЗАДАЧИ =====

    /**
     * Пример: запустить повторяющуюся задачу
     */
    private void startRepeatingTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Выполняется каждые 20 тиков (1 секунда)
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Ваша логика
            }
        }, 0L, 20L); // delay=0, period=20
    }

    /**
     * Пример: отложенная задача
     */
    private void scheduleDelayedTask(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Выполнится через 100 тиков (5 секунд)
            player.sendMessage(colorize("&a5 секунд прошло!"));
        }, 100L);
    }

    // ===== УТИЛИТЫ =====

    private String colorize(String msg) {
        return msg == null ? "" : msg.replace("&", "§");
    }
}
