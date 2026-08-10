import me.dcplugin.dcustomitems.api.modules.Module;
import me.dcplugin.dcustomitems.Main;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

/**
 * 🎨 DeluxeMenuX - Advanced Menu System
 *
 * Полноценная система меню как DeluxeMenus
 *
 * Функции:
 * - YAML конфигурация меню
 * - Предметы с командами
 * - Условия и проверки
 * - Анимации и звуки
 * - Плейсхолдеры
 * - Навигация между меню
 */
public class deluxemenuxModule extends Module implements Listener {

    private final Map<String, MenuData> menus = new LinkedHashMap<>();
    private final Map<UUID, String> openMenus = new HashMap<>();

    public deluxemenuxModule(Main plugin, String id, File folder) {
        super(plugin, id, folder);
    }

    @Override
    protected void onEnable() {
        // Регистрируем слушатель
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Загружаем все меню
        loadAllMenus();

        plugin.getLogger().info("[DeluxeMenuX] Loaded " + menus.size() + " menus");
    }

    @Override
    protected void onDisable() {
        HandlerList.unregisterAll(this);
        menus.clear();
        openMenus.clear();
    }

    /**
     * Загрузить все меню из папки menus/
     */
    private void loadAllMenus() {
        menus.clear();

        File menusDir = new File(folder, "menus");
        if (!menusDir.exists()) {
            menusDir.mkdirs();
        }

        // Создаём только отсутствующие примеры. Пользовательские YAML не перезаписываются.
        createDefaultMenus(menusDir);

        File[] menuFiles = menusDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (menuFiles == null) return;

        for (File menuFile : menuFiles) {
            try {
                loadMenu(menuFile);
            } catch (Exception e) {
                plugin.getLogger().warning("[DeluxeMenuX] Error loading menu: " + menuFile.getName() + " - " + e.getMessage());
            }
        }
    }

    /**
     * Загрузить меню из YAML файла
     */
    private void loadMenu(File menuFile) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(menuFile);

        String menuId = menuFile.getName().replace(".yml", "");
        String title = colorize(config.getString("title", "Menu"));
        int size = config.getInt("size", 27);
        String openSound = config.getString("open-sound", "");
        String closeSound = config.getString("close-sound", "");

        // Загружаем предметы
        Map<Integer, MenuItem> items = new HashMap<>();
        if (config.contains("items")) {
            for (String slotKey : config.getConfigurationSection("items").getKeys(false)) {
                try {
                    int slot = Integer.parseInt(slotKey);
                    MenuItem item = loadMenuItem(config, "items." + slotKey);
                    if (item != null) {
                        items.put(slot, item);
                    }
                } catch (NumberFormatException e) {
                    // Invalid slot
                }
            }
        }

        // Заполняем фон
        MenuItem fillItem = null;
        if (config.contains("fill")) {
            fillItem = loadMenuItem(config, "fill");
        }

        MenuData menu = new MenuData(menuId, title, size, items, fillItem, openSound, closeSound);
        menus.put(menuId, menu);
    }

    /**
     * Загрузить предмет меню
     */
    private MenuItem loadMenuItem(YamlConfiguration config, String path) {
        String materialName = config.getString(path + ".material", "STONE");
        String name = config.getString(path + ".name", "");
        List<String> lore = config.getStringList(path + ".lore");
        String command = config.getString(path + ".command", "");
        String message = config.getString(path + ".message", "");
        String permission = config.getString(path + ".permission", "");
        String sound = config.getString(path + ".sound", "");
        String close = config.getString(path + ".close", "false");
        int amount = config.getInt(path + ".amount", 1);
        int data = config.getInt(path + ".data", 0);

        Material material = Material.matchMaterial(materialName.toUpperCase());
        if (material == null) material = Material.STONE;

        return new MenuItem(material, name, lore, command, message, permission, sound,
                           close.equalsIgnoreCase("true"), amount, (byte) data);
    }

    /**
     * Создать дефолтные меню
     */
    private void createDefaultMenus(File menusDir) {
        // Создаём только отсутствующие файлы, чтобы не затирать настройки владельца.
        if (!new File(menusDir, "shop.yml").exists()) createShopMenu(menusDir);
        if (!new File(menusDir, "main.yml").exists()) createMainMenu(menusDir);
        if (!new File(menusDir, "kits.yml").exists()) createKitsMenu(menusDir);
    }

    private void createShopMenu(File menusDir) {
        YamlConfiguration config = new YamlConfiguration();

        config.set("title", "&6🛒 Магазин");
        config.set("size", 45);
        config.set("open-sound", "UI_BUTTON_CLICK");
        config.set("close-sound", "UI_BUTTON_CLICK");

        // Фон
        config.set("fill.material", "GRAY_STAINED_GLASS_PANE");
        config.set("fill.name", " ");

        // Заголовок
        config.set("items.4.material", "PLAYER_HEAD");
        config.set("items.4.name", "&6&lМагазин");
        config.set("items.4.lore", Arrays.asList("", "&7Выберите категорию", ""));

        // Алмазы
        config.set("items.19.material", "DIAMOND");
        config.set("items.19.name", "&bАлмазы");
        config.set("items.19.lore", Arrays.asList("", "&7Нажмите чтобы купить", ""));
        config.set("items.19.command", "give %player% diamond 1");
        config.set("items.19.message", "&aВы купили алмаз!");
        config.set("items.19.sound", "ENTITY_PLAYER_LEVELUP");

        // Изумруды
        config.set("items.22.material", "EMERALD");
        config.set("items.22.name", "&aИзумруды");
        config.set("items.22.lore", Arrays.asList("", "&7Нажмите чтобы купить", ""));
        config.set("items.22.command", "give %player% emerald 1");
        config.set("items.22.message", "&aВы купили изумруд!");
        config.set("items.22.sound", "ENTITY_PLAYER_LEVELUP");

        // Золото
        config.set("items.25.material", "GOLD_INGOT");
        config.set("items.25.name", "&6Золото");
        config.set("items.25.lore", Arrays.asList("", "&7Нажмите чтобы купить", ""));
        config.set("items.25.command", "give %player% gold_ingot 1");
        config.set("items.25.message", "&aВы купили золото!");
        config.set("items.25.sound", "ENTITY_PLAYER_LEVELUP");

        // Закрыть
        config.set("items.40.material", "BARRIER");
        config.set("items.40.name", "&cЗакрыть");
        config.set("items.40.lore", Arrays.asList("", "&7Закрыть меню", ""));
        config.set("items.40.close", "true");
        config.set("items.40.sound", "UI_BUTTON_CLICK");

        try {
            config.save(new File(menusDir, "shop.yml"));
        } catch (Exception e) {
            plugin.getLogger().warning("[DeluxeMenuX] Error creating shop menu");
        }
    }

    private void createKitsMenu(File menusDir) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("title", "&6&lНаборы");
        config.set("size", 27);
        config.set("open-sound", "UI_BUTTON_CLICK");
        config.set("close-sound", "UI_BUTTON_CLICK");
        config.set("fill.material", "BLACK_STAINED_GLASS_PANE");
        config.set("fill.name", " ");
        config.set("items.11.material", "STONE_SWORD");
        config.set("items.11.name", "&a&lСтартовый набор");
        config.set("items.11.lore", Arrays.asList("", "&7Получить стартовый комплект", "", "&eНажмите, чтобы получить"));
        config.set("items.11.permission", "deluxemenux.kit.start");
        config.set("items.11.command", "give %player% stone_sword 1");
        config.set("items.11.message", "&aСтартовый набор выдан!");
        config.set("items.11.sound", "ENTITY_PLAYER_LEVELUP");
        config.set("items.13.material", "IRON_CHESTPLATE");
        config.set("items.13.name", "&b&lЖелезный набор");
        config.set("items.13.lore", Arrays.asList("", "&7Железная броня и инструменты", "", "&eНажмите, чтобы получить"));
        config.set("items.13.permission", "deluxemenux.kit.iron");
        config.set("items.13.command", "give %player% iron_chestplate 1");
        config.set("items.13.message", "&bЖелезный набор выдан!");
        config.set("items.13.sound", "ENTITY_PLAYER_LEVELUP");
        config.set("items.15.material", "DIAMOND");
        config.set("items.15.name", "&b&lАлмазный набор");
        config.set("items.15.lore", Arrays.asList("", "&7Премиальный набор", "", "&eНажмите, чтобы получить"));
        config.set("items.15.permission", "deluxemenux.kit.diamond");
        config.set("items.15.command", "give %player% diamond 3");
        config.set("items.15.message", "&bАлмазный набор выдан!");
        config.set("items.15.sound", "ENTITY_PLAYER_LEVELUP");
        config.set("items.22.material", "BARRIER");
        config.set("items.22.name", "&cЗакрыть");
        config.set("items.22.close", true);
        try { config.save(new File(menusDir, "kits.yml")); }
        catch (Exception e) { plugin.getLogger().warning("[DeluxeMenuX] Error creating kits menu"); }
    }

    private void createMainMenu(File menusDir) {
        YamlConfiguration config = new YamlConfiguration();

        config.set("title", "&6&lГлавное Меню");
        config.set("size", 27);
        config.set("open-sound", "UI_BUTTON_CLICK");

        // Фон
        config.set("fill.material", "BLACK_STAINED_GLASS_PANE");
        config.set("fill.name", " ");

        // Магазин
        config.set("items.11.material", "EMERALD_BLOCK");
        config.set("items.11.name", "&a&lМагазин");
        config.set("items.11.lore", Arrays.asList("", "&7Открыть магазин", ""));
        config.set("items.11.command", "menu shop");
        config.set("items.11.sound", "UI_BUTTON_CLICK");

        // Инвентарь
        config.set("items.13.material", "CHEST");
        config.set("items.13.name", "&b&lИнвентарь");
        config.set("items.13.lore", Arrays.asList("", "&7Управление предметами", ""));
        config.set("items.13.command", "ci list");
        config.set("items.13.sound", "UI_BUTTON_CLICK");

        // Команды
        config.set("items.15.material", "COMMAND_BLOCK");
        config.set("items.15.name", "&e&lКоманды");
        config.set("items.15.lore", Arrays.asList("", "&7Список команд", ""));
        config.set("items.15.command", "help");
        config.set("items.15.sound", "UI_BUTTON_CLICK");

        // Закрыть
        config.set("items.22.material", "BARRIER");
        config.set("items.22.name", "&cЗакрыть");
        config.set("items.22.lore", Arrays.asList("", "&7Закрыть меню", ""));
        config.set("items.22.close", "true");
        config.set("items.22.sound", "UI_BUTTON_CLICK");

        try {
            config.save(new File(menusDir, "main.yml"));
        } catch (Exception e) {
            plugin.getLogger().warning("[DeluxeMenuX] Error creating main menu");
        }
    }

    /**
     * Открыть меню игроку
     */
    @Override
    public boolean openMenu(Player player, String menuId) {
        MenuData menu = menus.get(menuId.toLowerCase());
        if (menu == null) {
            player.sendMessage(colorize("&cМеню не найдено: " + menuId));
            return false;
        }

        // Проверяем права
        // TODO:权限检查

        // Создаём инвентарь
        Inventory inventory = plugin.getServer().createInventory(
            new MenuHolder(menuId),
            menu.size,
            menu.title
        );

        // Устанавливаем фон
        if (menu.fillItem != null) {
            ItemStack fillStack = createItemStack(menu.fillItem);
            for (int i = 0; i < menu.size; i++) {
                inventory.setItem(i, fillStack);
            }
        }

        // Устанавливаем предметы
        for (Map.Entry<Integer, MenuItem> entry : menu.items.entrySet()) {
            ItemStack item = createItemStack(entry.getValue());
            inventory.setItem(entry.getKey(), item);
        }

        // Воспроизводим звук открытия
        if (!menu.openSound.isEmpty()) {
            playSound(player, menu.openSound);
        }

        player.openInventory(inventory);
        openMenus.put(player.getUniqueId(), menuId);

        return true;
    }

    /**
     * Создать ItemStack из MenuItem
     */
    private ItemStack createItemStack(MenuItem menuItem) {
        ItemStack item = new ItemStack(menuItem.material, menuItem.amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (!menuItem.name.isEmpty()) {
                meta.setDisplayName(colorize(menuItem.name));
            }
            if (!menuItem.lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : menuItem.lore) {
                    coloredLore.add(colorize(line));
                }
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Обработать клик в меню
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory topInventory = event.getView().getTopInventory();

        if (topInventory == null || !(topInventory.getHolder() instanceof MenuHolder)) return;

        event.setCancelled(true);

        MenuHolder holder = (MenuHolder) topInventory.getHolder();
        String menuId = holder.getMenuId();
        MenuData menu = menus.get(menuId);

        if (menu == null) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= menu.size) return;

        MenuItem menuItem = menu.items.get(slot);
        if (menuItem == null) return;

        // Проверяем права
        if (!menuItem.permission.isEmpty() && !player.hasPermission(menuItem.permission)) {
            player.sendMessage(colorize("&cУ вас нет прав!"));
            return;
        }

        // Выполняем команду
        if (!menuItem.command.isEmpty()) {
            String cmd = menuItem.command.replace("%player%", player.getName());
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
        }

        // Отправляем сообщение
        if (!menuItem.message.isEmpty()) {
            String msg = menuItem.message.replace("%player%", player.getName());
            player.sendMessage(colorize(msg));
        }

        // Воспроизводим звук
        if (!menuItem.sound.isEmpty()) {
            playSound(player, menuItem.sound);
        }

        // Закрываем меню
        if (menuItem.close) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        openMenus.remove(player.getUniqueId());
    }

    private void playSound(Player player, String soundName) {
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (Exception ignored) {}
    }

    private String colorize(String msg) {
        return msg == null ? "" : msg.replace("&", "§");
    }

    // ===== ВНУТРЕННИЕ КЛАССЫ =====

    public static class MenuData {
        final String id;
        final String title;
        final int size;
        final Map<Integer, MenuItem> items;
        final MenuItem fillItem;
        final String openSound;
        final String closeSound;

        public MenuData(String id, String title, int size, Map<Integer, MenuItem> items,
                       MenuItem fillItem, String openSound, String closeSound) {
            this.id = id;
            this.title = title;
            this.size = size;
            this.items = items;
            this.fillItem = fillItem;
            this.openSound = openSound;
            this.closeSound = closeSound;
        }
    }

    public static class MenuItem {
        final Material material;
        final String name;
        final List<String> lore;
        final String command;
        final String message;
        final String permission;
        final String sound;
        final boolean close;
        final int amount;
        final byte data;

        public MenuItem(Material material, String name, List<String> lore, String command,
                       String message, String permission, String sound, boolean close,
                       int amount, byte data) {
            this.material = material;
            this.name = name;
            this.lore = lore;
            this.command = command;
            this.message = message;
            this.permission = permission;
            this.sound = sound;
            this.close = close;
            this.amount = amount;
            this.data = data;
        }
    }

    public static class MenuHolder implements InventoryHolder {
        private final String menuId;

        public MenuHolder(String menuId) {
            this.menuId = menuId;
        }

        public String getMenuId() {
            return menuId;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
