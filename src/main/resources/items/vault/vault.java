import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.modules.Module;
import me.dcplugin.dcustomitems.api.placeholders.PlaceholderManager;
import me.dcplugin.dcustomitems.models.CustomItem;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.util.*;

/**
 * 💰 Vault Economy Module
 *
 * Полноценная система экономики для DC-CustomItems:
 * - Покупка/продажа предметов
 * - Плейсхолдеры баланса
 * - Настройка цен в YAML
 *
 * Включение/выключение: existence папки items/vault/
 * Настройки: items/vault/config.yml
 */
public class vaultModule extends Module implements Listener, CommandExecutor, TabCompleter {

    private Economy economy = null;
    private boolean vaultEnabled = false;

    // Сообщения из конфига
    private String msgInsufficientFunds;
    private String msgBuySuccess;
    private String msgSellSuccess;
    private String msgNotBuyable;
    private String msgNotSellable;
    private String msgInventoryFull;
    private String msgNothingToSell;

    // Настройки
    private boolean buyEnabled;
    private boolean sellEnabled;
    private int maxAmount;

    public vaultModule(Main plugin, String id, File folder) {
        super(plugin, id, folder);
    }

    @Override
    protected void onEnable() {
        // Загружаем настройки
        loadShopConfig();

        // Подключаем Vault
        if (!setupVault()) {
            plugin.getLogger().severe("[Vault] Vault not found! Module disabled.");
            plugin.getLogger().severe("[Vault] Install Vault: https://www.spigotmc.org/resources/vault.34315/");
            return;
        }

        // Регистрируем слушатель
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Регистрируем команды (базовый класс сам удалит их при disable)
        registerDynamicCommand("buy", this);
        registerDynamicCommand("sell", this);

        // Регистрируем плейсхолдеры
        registerPlaceholders();

        plugin.getLogger().info("[Vault] Economy enabled: " + economy.getName());
    }

    @Override
    protected void onDisable() {
        // Отменяем всеregistered things
        org.bukkit.event.HandlerList.unregisterAll(this);
        economy = null;
        vaultEnabled = false;
    }

    // ===== VAULT SETUP =====

    private boolean setupVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        economy = rsp.getProvider();
        vaultEnabled = (economy != null && economy.isEnabled());
        return vaultEnabled;
    }

    // ===== CONFIG =====

    private void loadShopConfig() {
        buyEnabled = config.getBoolean("shop.buy-enabled", true);
        sellEnabled = config.getBoolean("shop.sell-enabled", true);
        maxAmount = config.getInt("shop.max-amount", 64);

        msgInsufficientFunds = config.getString("shop.insufficient-funds", "&c[DCI] &cНедостаточно средств!");
        msgBuySuccess = config.getString("shop.buy-success", "&a[DCI] &aВы купили &e{item} x{amount} &aза &e{price}&a!");
        msgSellSuccess = config.getString("shop.sell-success", "&a[DCI] &aВы продали &e{item} x{amount} &aза &e{price}&a!");
        msgNotBuyable = config.getString("shop.not-buyable", "&c[DCI] &cЭтот предмет нельзя купить!");
        msgNotSellable = config.getString("shop.not-sellable", "&c[DCI] &cЭтот предмет нельзя продать!");
        msgInventoryFull = config.getString("shop.inventory-full", "&c[DCI] &cИнвентарь полон!");
        msgNothingToSell = config.getString("shop.nothing-to-sell", "&c[DCI] &cУ вас нет этого предмета!");
    }

    // ===== COMMANDS =====

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("&cТолько для игроков!");
            return true;
        }

        Player player = (Player) sender;
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("buy")) {
            return handleBuy(player, args);
        } else if (cmdName.equals("sell")) {
            return handleSell(player, args);
        }

        return false;
    }

    // ===== BUY =====

    private boolean handleBuy(Player player, String[] args) {
        if (!vaultEnabled) {
            player.sendMessage(colorize("&c[DCI] &cЭкономика не подключена!"));
            return true;
        }

        if (!buyEnabled) {
            player.sendMessage(colorize("&c[DCI] &cПокупка отключена!"));
            return true;
        }

        if (args.length < 1) {
            sendBuyHelp(player);
            return true;
        }

        String itemId = args[0].toLowerCase();
        int amount = 1;

        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
                if (amount < 1 || amount > maxAmount) {
                    player.sendMessage(colorize("&c[DCI] &cКоличество: 1-" + maxAmount));
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(colorize("&c[DCI] &cНеверное количество"));
                return true;
            }
        }

        CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
        if (customItem == null) {
            player.sendMessage(colorize("&c[DCI] &cПредмет '" + itemId + "' не найден!"));
            return true;
        }

        if (!customItem.isBuyable()) {
            player.sendMessage(colorize(msgNotBuyable));
            return true;
        }

        if (customItem.hasPermission() && !player.hasPermission(customItem.getPermission())) {
            player.sendMessage(colorize("&c[DCI] &cНет прав!"));
            return true;
        }

        double price = customItem.getBuyPrice() * amount;

        if (!economy.has(player, price)) {
            String msg = msgInsufficientFunds
                .replace("{price}", economy.format(price))
                .replace("{balance}", economy.format(economy.getBalance(player)));
            player.sendMessage(colorize(msg));
            return true;
        }

        ItemStack itemStack = customItem.getItemStack().clone();
        itemStack.setAmount(amount);

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(colorize(msgInventoryFull));
            return true;
        }

        EconomyResponse response = economy.withdrawPlayer(player, price);
        if (!response.transactionSuccess()) {
            player.sendMessage(colorize("&c[DCI] &cОшибка при списании средств!"));
            return true;
        }

        plugin.getItemHandler().updateItemWithUses(itemStack);
        player.getInventory().addItem(itemStack);

        String msg = msgBuySuccess
            .replace("{item}", itemId)
            .replace("{amount}", String.valueOf(amount))
            .replace("{price}", economy.format(price));
        player.sendMessage(colorize(msg));

        return true;
    }

    // ===== SELL =====

    private boolean handleSell(Player player, String[] args) {
        if (!vaultEnabled) {
            player.sendMessage(colorize("&c[DCI] &cЭкономика не подключена!"));
            return true;
        }

        if (!sellEnabled) {
            player.sendMessage(colorize("&c[DCI] &cПродажа отключена!"));
            return true;
        }

        if (args.length < 1) {
            sendSellHelp(player);
            return true;
        }

        String itemId = args[0].toLowerCase();
        int amount = 1;

        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
                if (amount < 1) {
                    player.sendMessage(colorize("&c[DCI] &cКоличество должно быть > 0"));
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(colorize("&c[DCI] &cНеверное количество"));
                return true;
            }
        }

        CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
        if (customItem == null) {
            player.sendMessage(colorize("&c[DCI] &cПредмет '" + itemId + "' не найден!"));
            return true;
        }

        if (!customItem.isSellable()) {
            player.sendMessage(colorize(msgNotSellable));
            return true;
        }

        // Считаем количество
        int found = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && itemId.equals(plugin.getItemHandler().getCustomItemId(item))) {
                found += item.getAmount();
            }
        }

        if (found < amount) {
            String msg = msgNothingToSell + " &7(у вас: &e" + found + "&7)";
            player.sendMessage(colorize(msg));
            return true;
        }

        double totalPrice = customItem.getSellPrice() * amount;

        // Забираем предметы
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (remaining <= 0) break;
            if (item != null && itemId.equals(plugin.getItemHandler().getCustomItemId(item))) {
                int toRemove = Math.min(item.getAmount(), remaining);
                item.setAmount(item.getAmount() - toRemove);
                remaining -= toRemove;
            }
        }

        EconomyResponse response = economy.depositPlayer(player, totalPrice);
        if (!response.transactionSuccess()) {
            player.sendMessage(colorize("&c[DCI] &cОшибка при начислении средств!"));
            return true;
        }

        String msg = msgSellSuccess
            .replace("{item}", itemId)
            .replace("{amount}", String.valueOf(amount))
            .replace("{price}", economy.format(totalPrice));
        player.sendMessage(colorize(msg));

        return true;
    }

    // ===== HELP =====

    private void sendBuyHelp(Player player) {
        player.sendMessage(colorize("&6=== Vault Economy ==="));
        player.sendMessage(colorize("&e/buy <id> &7- Купить предмет"));
        player.sendMessage(colorize("&e/buy <id> <кол-во> &7- Купить несколько"));
        player.sendMessage(colorize("&7Баланс: &e" + economy.format(economy.getBalance(player))));
        player.sendMessage(colorize("&7"));

        List<String> buyable = new ArrayList<>();
        for (String id : plugin.getItemHandler().getAllCustomItems().keySet()) {
            CustomItem item = plugin.getItemHandler().getCustomItem(id);
            if (item != null && item.isBuyable()) {
                buyable.add(id);
            }
        }

        if (!buyable.isEmpty()) {
            player.sendMessage(colorize("&7Доступные товары:"));
            for (String id : buyable) {
                CustomItem item = plugin.getItemHandler().getCustomItem(id);
                player.sendMessage(colorize("  &e" + id + " &7- &a" + economy.format(item.getBuyPrice())));
            }
        }
    }

    private void sendSellHelp(Player player) {
        player.sendMessage(colorize("&6=== Vault Economy ==="));
        player.sendMessage(colorize("&e/sell <id> &7- Продать предмет"));
        player.sendMessage(colorize("&e/sell <id> <кол-во> &7- Продать несколько"));
        player.sendMessage(colorize("&7Баланс: &e" + economy.format(economy.getBalance(player))));
    }

    // ===== TAB COMPLETE =====

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        String cmdName = command.getName().toLowerCase();

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String id : plugin.getItemHandler().getAllCustomItems().keySet()) {
                CustomItem item = plugin.getItemHandler().getCustomItem(id);
                if (item == null) continue;
                if (cmdName.equals("buy") && !item.isBuyable()) continue;
                if (cmdName.equals("sell") && !item.isSellable()) continue;
                if (id.toLowerCase().startsWith(prefix)) {
                    completions.add(id);
                }
            }
        } else if (args.length == 2) {
            completions.addAll(Arrays.asList("1", "2", "3", "5", "10", "16", "32", "64"));
        }

        return completions;
    }

    // ===== PLACEHOLDERS =====

    private void registerPlaceholders() {
        if (plugin.getPlaceholderManager() == null) return;

        PlaceholderManager pm = plugin.getPlaceholderManager();

        // %vault_balance% - баланс (целое число)
        pm.register("vault_balance", (p) -> {
            if (!vaultEnabled || economy == null) return "0";
            return String.valueOf((int) economy.getBalance(p));
        });

        // %vault_balance_formatted% - баланс с форматированием
        pm.register("vault_balance_formatted", (p) -> {
            if (!vaultEnabled || economy == null) return "$0";
            return economy.format(economy.getBalance(p));
        });

        // %vault_currency% - название валюты
        pm.register("vault_currency", (p) -> {
            if (!vaultEnabled || economy == null) return "dollars";
            return economy.currencyNamePlural();
        });

        // %vault_currency_singular% - название валюты (ед. ч.)
        pm.register("vault_currency_singular", (p) -> {
            if (!vaultEnabled || economy == null) return "dollar";
            return economy.currencyNameSingular();
        });
    }

    // ===== EVENTS =====

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Можно добавить логику при входе (приветственные деньги и т.д.)
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Cleanup if needed
    }

    // ===== PUBLIC API =====

    /**
     * Проверить, включена ли экономика
     */
    public boolean isEconomyEnabled() {
        return vaultEnabled && economy != null && economy.isEnabled();
    }

    /**
     * Получить баланс игрока
     */
    public double getBalance(Player player) {
        if (!isEconomyEnabled()) return 0;
        return economy.getBalance(player);
    }

    /**
     * Снять деньги
     */
    public boolean withdraw(Player player, double amount, String reason) {
        if (!isEconomyEnabled()) return false;
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    /**
     * Начислить деньги
     */
    public boolean deposit(Player player, double amount, String reason) {
        if (!isEconomyEnabled()) return false;
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    /**
     * Форматировать сумму
     */
    public String format(double amount) {
        if (!isEconomyEnabled()) return String.valueOf(amount);
        return economy.format(amount);
    }

    // ===== UTILS =====

    private String colorize(String msg) {
        return msg == null ? "" : msg.replace("&", "§");
    }
}
