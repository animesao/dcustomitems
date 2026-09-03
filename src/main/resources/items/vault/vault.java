import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.database.DatabaseManager;
import me.dcplugin.dcustomitems.api.modules.Module;
import me.dcplugin.dcustomitems.api.placeholders.PlaceholderManager;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.util.*;

/**
 * 💰 Vault Economy Module — простая валюта для игроков (в стиле Essentials)
 *
 * Покупка/продажа предметов — это отдельные плагины, работающие с валютой
 * через Vault или публичный API модуля (getBalance / withdraw / deposit / format).
 *
 * Экономика (Essentials-style):
 *   /balance [игрок]     (алиасы: /bal, /money)
 *   /pay <игрок> <сумма>
 *   /eco give|take|set|reset <игрок> <сумма>   (админ)
 *
 * Плюс: стартовый баланс новому игроку, плейсхолдеры баланса.
 *
 * Включение/выключение: наличие папки items/vault/
 * Настройки: items/vault/config.yml
 */
public class vaultModule extends Module implements Listener, CommandExecutor, TabCompleter {

    private Economy economy = null;
    private boolean vaultEnabled = false;

    // Настройки экономики
    private boolean payEnabled;
    private double minPay;
    private double startingBalance;

    // Хранилище: false = экономика Vault (EssentialsX и т.п.),
    // true = собственная БД плагина (data.db / MySQL)
    private boolean ownStorage;
    private boolean isMySqlDb;
    private String currencySymbol = "$";

    // Кому уже выдан стартовый баланс (items/vault/starting-paid.yml)
    private File startingPaidFile;
    private YamlConfiguration startingPaid;

    public vaultModule(Main plugin, String id, File folder) {
        super(plugin, id, folder);
    }

    @Override
    protected void onEnable() {
        loadSettings();

        ownStorage = "own".equalsIgnoreCase(config.getString("economy.storage", "vault"));
        if (ownStorage) {
            // Собственной БД достаточно для работы экономики, но для КОМПИЛЯЦИИ
            // этого .java-модуля нужны классы VaultAPI из jar плагина Vault.
            if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
                plugin.getLogger().severe("[Vault] storage: own не требует Vault-экономику,");
                plugin.getLogger().severe("[Vault] но jar плагина Vault нужен для компиляции модуля.");
                plugin.getLogger().severe("[Vault] Установите Vault: https://www.spigotmc.org/resources/vault.34315/");
                return;
            }
            // Собственная БД — плагин экономики (EssentialsX и т.п.) не нужен
            if (!setupOwnStorage()) {
                plugin.getLogger().severe("[Vault] Own database storage unavailable! Модуль отключён.");
                plugin.getLogger().severe("[Vault] Проверьте database.* в config.yml ядра (data.db / MySQL).");
                return;
            }
            vaultEnabled = true;
        } else if (!setupVault()) {
            plugin.getLogger().severe("[Vault] Vault/экономика не найдены! Модуль отключён.");
            plugin.getLogger().severe("[Vault] Установите Vault + плагин экономики (EssentialsX и т.п.),");
            plugin.getLogger().severe("[Vault] либо включите economy.storage: own для собственной БД.");
            return;
        }

        // Права: команды для игроков — у всех, админские — у OP
        ensurePermission("dci.balance", PermissionDefault.TRUE);
        ensurePermission("dci.balance.others", PermissionDefault.TRUE);
        ensurePermission("dci.pay", PermissionDefault.TRUE);
        ensurePermission("dci.eco", PermissionDefault.OP);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Экономика
        registerDynamicCommand("balance", this);
        registerDynamicCommand("bal", this);
        registerDynamicCommand("money", this);
        registerDynamicCommand("pay", this);
        registerDynamicCommand("eco", this, "dci.eco");

        registerPlaceholders();
        loadStartingPaid();

        plugin.getLogger().info("[Vault] Economy enabled: "
                + (ownStorage ? "own-database (" + plugin.getDatabaseManager().getType() + ")" : economy.getName())
                + " | pay=" + payEnabled + ", starting-balance=" + startingBalance);
    }

    @Override
    protected void onDisable() {
        org.bukkit.event.HandlerList.unregisterAll(this);
        economy = null;
        vaultEnabled = false;
    }

    // ===== VAULT SETUP =====

    private boolean setupVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        vaultEnabled = (economy != null && economy.isEnabled());
        return vaultEnabled;
    }

    private void ensurePermission(String node, PermissionDefault def) {
        try {
            Bukkit.getPluginManager().addPermission(new Permission(node, def));
        } catch (IllegalArgumentException ignored) {
            // право уже зарегистрировано — оставляем как есть
        }
    }

    // ===== СОБСТВЕННАЯ БАЗА ДАННЫХ =====

    private boolean setupOwnStorage() {
        DatabaseManager db = plugin.getDatabaseManager();
        if (db == null || !db.isConnected()) return false;
        isMySqlDb = db.isMySql();

        String balanceCols = "uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(32) NOT NULL,"
                + " balance DOUBLE NOT NULL DEFAULT 0";
        String txCols = isMySqlDb
                ? "id BIGINT PRIMARY KEY AUTO_INCREMENT, ts BIGINT NOT NULL, actor VARCHAR(36),"
                  + " target VARCHAR(36), action VARCHAR(16), amount DOUBLE, details VARCHAR(128)"
                : "id INTEGER PRIMARY KEY, ts BIGINT NOT NULL, actor VARCHAR(36),"
                  + " target VARCHAR(36), action VARCHAR(16), amount DOUBLE, details VARCHAR(128)";

        return db.createTable("dci_vault_balances", balanceCols)
                && db.createTable("dci_vault_transactions", txCols);
    }

    /** Создать строку баланса, если её ещё нет (только для собственной БД). */
    private void ensureBalanceRow(OfflinePlayer p) {
        DatabaseManager db = plugin.getDatabaseManager();
        String uuid = p.getUniqueId().toString();
        String name = p.getName() != null ? p.getName() : "unknown";
        String insert = isMySqlDb
                ? "INSERT IGNORE INTO dci_vault_balances (uuid, name, balance) VALUES (?,?,0)"
                : "INSERT OR IGNORE INTO dci_vault_balances (uuid, name, balance) VALUES (?,?,0)";
        db.executeUpdate(insert, uuid, name);
        db.executeUpdate("UPDATE dci_vault_balances SET name=? WHERE uuid=? AND name<>?", name, uuid, name);
    }

    /** Баланс игрока через активное хранилище. */
    private double bal(OfflinePlayer p) {
        if (!ownStorage) return economy.getBalance(p);
        ensureBalanceRow(p);
        return plugin.getDatabaseManager().queryDouble(
                "SELECT balance FROM dci_vault_balances WHERE uuid=?", p.getUniqueId().toString());
    }

    private boolean hasMoney(OfflinePlayer p, double amount) {
        return bal(p) >= amount;
    }

    private boolean withdrawOp(OfflinePlayer p, double amount) {
        if (!ownStorage) return economy.withdrawPlayer(p, amount).transactionSuccess();
        DatabaseManager db = plugin.getDatabaseManager();
        ensureBalanceRow(p);
        // защита от ухода в минус: списываем только если хватает
        int rows = db.executeUpdate(
                "UPDATE dci_vault_balances SET balance = balance - ? WHERE uuid=? AND balance >= ?",
                amount, p.getUniqueId().toString(), amount);
        if (rows > 0) {
            logTx(p, null, "withdraw", amount);
            return true;
        }
        return false;
    }

    private boolean depositOp(OfflinePlayer p, double amount) {
        if (!ownStorage) return economy.depositPlayer(p, amount).transactionSuccess();
        DatabaseManager db = plugin.getDatabaseManager();
        ensureBalanceRow(p);
        int rows = db.executeUpdate(
                "UPDATE dci_vault_balances SET balance = balance + ? WHERE uuid=?",
                amount, p.getUniqueId().toString());
        if (rows > 0) {
            logTx(null, p, "deposit", amount);
            return true;
        }
        return false;
    }

    private void setBalanceOp(OfflinePlayer p, double amount) {
        if (!ownStorage) {
            setBalance(p, amount);
            return;
        }
        ensureBalanceRow(p);
        double current = bal(p);
        if (amount > current) {
            depositOp(p, amount - current);
        } else if (amount < current) {
            withdrawOp(p, current - amount);
        }
    }

    private String formatMoney(double amount) {
        if (!ownStorage) return economy.format(amount);
        return currencySymbol + String.format(java.util.Locale.US, "%.2f", amount);
    }

    /** Журнал транзакций в dci_vault_transactions (own-режим). */
    private void logTx(OfflinePlayer actor, OfflinePlayer target, String action, double amount) {
        try {
            DatabaseManager db = plugin.getDatabaseManager();
            db.executeUpdate(
                    "INSERT INTO dci_vault_transactions (ts, actor, target, action, amount, details)"
                    + " VALUES (?,?,?,?,?,?)",
                    System.currentTimeMillis(),
                    actor != null ? actor.getUniqueId().toString() : null,
                    target != null ? target.getUniqueId().toString() : null,
                    action, amount, "");
        } catch (Exception e) {
            plugin.getLogger().warning("[Vault] Transaction log failed: " + e.getMessage());
        }
    }

    // ===== SETTINGS =====

    private void loadSettings() {
        payEnabled = config.getBoolean("economy.pay-enabled", true);
        minPay = config.getDouble("economy.min-pay", 1.0);
        startingBalance = config.getDouble("economy.starting-balance", 0.0);
        ownStorage = "own".equalsIgnoreCase(config.getString("economy.storage", "vault"));
        currencySymbol = config.getString("economy.currency-symbol", "$");
    }

    private String msg(String path, String def) {
        return config.getString(path, def);
    }

    // ===== COMMAND ROUTING =====

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();

        switch (cmd) {
            case "balance":
            case "bal":
            case "money":
                return handleBalance(sender, args);
            case "pay":
                if (sender instanceof Player) return handlePay((Player) sender, args);
                break;
            case "eco":
                return handleEco(sender, args);
            default:
                return false;
        }

        sender.sendMessage(colorize(msg("economy.players-only", "&c[DCI] &cТолько для игроков!")));
        return true;
    }

    private boolean requireEconomy(CommandSender sender) {
        if (!vaultEnabled) {
            sender.sendMessage(colorize(msg("economy.economy-not-loaded",
                    "&c[DCI] &cЭкономика не подключена! Установите Vault + плагин экономики.")));
            return false;
        }
        return true;
    }

    /** Число >= 0 или null, если некорректно. */
    private Double parseAmount(String raw) {
        try {
            double value = Double.parseDouble(raw.replace(',', '.'));
            if (value < 0 || !Double.isFinite(value)) return null;
            return value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ===== /balance =====

    private boolean handleBalance(CommandSender sender, String[] args) {
        if (!requireEconomy(sender)) return true;

        // Свой баланс
        if (args.length < 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(colorize(msg("economy.players-only", "&c[DCI] &cТолько для игроков!")));
                return true;
            }
            Player player = (Player) sender;
            player.sendMessage(colorize(msg("economy.balance-own",
                    "&a[DCI] &aВаш баланс: &e{balance}")
                    .replace("{balance}", formatMoney(bal(player)))));
            return true;
        }

        // Чужой баланс
        if (!sender.hasPermission("dci.balance.others")) {
            sender.sendMessage(colorize(msg("economy.no-permission", "&c[DCI] &cНет прав!")));
            return true;
        }

        OfflinePlayer target = resolveTarget(args[0]);
        if (target == null) {
            sender.sendMessage(colorize(msg("economy.player-not-found",
                    "&c[DCI] &cИгрок '&e{player}&c' не найден.").replace("{player}", args[0])));
            return true;
        }

        String name = target.getName() != null ? target.getName() : args[0];
        sender.sendMessage(colorize(msg("economy.balance-others",
                "&a[DCI] &aБаланс игрока &e{player}&a: &e{balance}")
                .replace("{player}", name)
                .replace("{balance}", formatMoney(bal(target)))));
        return true;
    }

    // ===== /pay =====

    private boolean handlePay(Player player, String[] args) {
        if (!requireEconomy(player)) return true;

        if (!payEnabled) {
            player.sendMessage(colorize(msg("economy.pay-disabled", "&c[DCI] &cПереводы отключены!")));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(colorize(msg("economy.pay-usage",
                    "&c[DCI] &cИспользование: /pay <игрок> <сумма>")));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(colorize(msg("economy.pay-offline", "&c[DCI] &cИгрок должен быть онлайн!")));
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(colorize(msg("economy.pay-self", "&c[DCI] &cНельзя переводить самому себе!")));
            return true;
        }

        Double amount = parseAmount(args[1]);
        if (amount == null || amount < minPay) {
            player.sendMessage(colorize(msg("economy.pay-min",
                    "&c[DCI] &cМинимальная сумма перевода: &e{min}")
                    .replace("{min}", formatMoney(minPay))));
            return true;
        }
        if (!hasMoney(player, amount)) {
            player.sendMessage(colorize(msg("economy.insufficient-funds",
                    "&c[DCI] &cНедостаточно средств! Нужно: &e{price} &c| У вас: &e{balance}")
                    .replace("{price}", formatMoney(amount))
                    .replace("{balance}", formatMoney(bal(player)))));
            return true;
        }

        if (!withdrawOp(player, amount)) {
            player.sendMessage(colorize(msg("economy.transaction-error", "&c[DCI] &cОшибка транзакции!")));
            return true;
        }
        if (!depositOp(target, amount)) {
            depositOp(player, amount); // откат при ошибке зачисления
            player.sendMessage(colorize(msg("economy.transaction-error", "&c[DCI] &cОшибка транзакции!")));
            return true;
        }

        player.sendMessage(colorize(msg("economy.pay-sent",
                "&a[DCI] &aВы перевели &e{amount} &aигроку &e{player}&a. Баланс: &e{balance}")
                .replace("{amount}", formatMoney(amount))
                .replace("{player}", target.getName())
                .replace("{balance}", formatMoney(bal(player)))));
        target.sendMessage(colorize(msg("economy.pay-received",
                "&a[DCI] &e{player} &aперевёл вам &e{amount}&a. Баланс: &e{balance}")
                .replace("{player}", player.getName())
                .replace("{amount}", formatMoney(amount))
                .replace("{balance}", formatMoney(bal(target)))));
        return true;
    }

    // ===== /eco (админ) =====

    private boolean handleEco(CommandSender sender, String[] args) {
        if (!requireEconomy(sender)) return true;

        if (args.length < 2) {
            sender.sendMessage(colorize(msg("economy.eco-usage",
                    "&c[DCI] &cИспользование: /eco <give|take|set|reset> <игрок> [сумма]")));
            return true;
        }

        String action = args[0].toLowerCase();
        OfflinePlayer target = resolveTarget(args[1]);
        if (target == null) {
            sender.sendMessage(colorize(msg("economy.player-not-found",
                    "&c[DCI] &cИгрок '&e{player}&c' не найден.").replace("{player}", args[1])));
            return true;
        }
        String name = target.getName() != null ? target.getName() : args[1];

        // /eco reset <игрок> — без суммы
        if (action.equals("reset")) {
            setBalanceOp(target, 0);
            sender.sendMessage(colorize(msg("economy.eco-done",
                    "&a[DCI] &aБаланс &e{player}&a: &e{balance}")
                    .replace("{player}", name)
                    .replace("{balance}", formatMoney(0))));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(colorize(msg("economy.eco-usage",
                    "&c[DCI] &cИспользование: /eco <give|take|set|reset> <игрок> [сумма]")));
            return true;
        }

        Double amount = parseAmount(args[2]);
        if (amount == null) {
            sender.sendMessage(colorize("&c[DCI] &cНеверная сумма"));
            return true;
        }

        switch (action) {
            case "give":
                depositOp(target, amount);
                break;
            case "take":
                // нельзя увести в минус больше, чем есть
                withdrawOp(target, Math.min(amount, bal(target)));
                break;
            case "set":
                setBalanceOp(target, amount);
                break;
            default:
                sender.sendMessage(colorize(msg("economy.eco-usage",
                        "&c[DCI] &cИспользование: /eco <give|take|set|reset> <игрок> [сумма]")));
                return true;
        }

        sender.sendMessage(colorize(msg("economy.eco-done",
                "&a[DCI] &aБаланс &e{player}&a: &e{balance}")
                .replace("{player}", name)
                .replace("{balance}", formatMoney(bal(target)))));
        return true;
    }

    private void setBalance(OfflinePlayer target, double amount) {
        double current = economy.getBalance(target);
        if (amount > current) {
            economy.depositPlayer(target, amount - current);
        } else if (amount < current) {
            economy.withdrawPlayer(target, current - amount);
        }
    }

    /** Онлайн-игрок по имени, иначе закэшированный offline-игрок, иначе null. */
    private OfflinePlayer resolveTarget(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        try {
            return Bukkit.getOfflinePlayerIfCached(name);
        } catch (NoSuchMethodError e) {
            return null; // не-Paper сервер
        }
    }


    // ===== СТАРТОВЫЙ БАЛАНС =====

    private void loadStartingPaid() {
        startingPaidFile = new File(folder, "starting-paid.yml");
        startingPaid = startingPaidFile.exists()
                ? YamlConfiguration.loadConfiguration(startingPaidFile)
                : new YamlConfiguration();
    }

    private void saveStartingPaid() {
        try {
            startingPaid.save(startingPaidFile);
        } catch (Exception e) {
            plugin.getLogger().warning("[Vault] Cannot save starting-paid.yml: " + e.getMessage());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!vaultEnabled || startingBalance <= 0) return;
        Player player = event.getPlayer();

        if (ownStorage) {
            // Стартовый баланс сразу в БД: строка создаётся с нужной суммой
            // только если её ещё нет — повторно не выдаётся
            DatabaseManager db = plugin.getDatabaseManager();
            String insert = isMySqlDb
                    ? "INSERT IGNORE INTO dci_vault_balances (uuid, name, balance) VALUES (?,?,?)"
                    : "INSERT OR IGNORE INTO dci_vault_balances (uuid, name, balance) VALUES (?,?,?)";
            int rows = db.executeUpdate(insert,
                    player.getUniqueId().toString(), player.getName(), startingBalance);
            if (rows > 0) {
                logTx(null, player, "starting", startingBalance);
                player.sendMessage(colorize(msg("economy.starting-balance-given",
                        "&a[DCI] &aВам начислен стартовый баланс: &e{amount}")
                        .replace("{amount}", formatMoney(startingBalance))));
            }
            return;
        }

        if (startingPaid.contains("paid." + player.getUniqueId())) return;

        EconomyResponse response = economy.depositPlayer(player, startingBalance);
        if (!response.transactionSuccess()) return;

        startingPaid.set("paid." + player.getUniqueId(), player.getName());
        saveStartingPaid();
        player.sendMessage(colorize(msg("economy.starting-balance-given",
                "&a[DCI] &aВам начислен стартовый баланс: &e{amount}")
                .replace("{amount}", formatMoney(startingBalance))));
    }

    // ===== PLACEHOLDERS =====

    private void registerPlaceholders() {
        if (plugin.getPlaceholderManager() == null) return;
        PlaceholderManager pm = plugin.getPlaceholderManager();

        pm.register("vault_balance", (p) ->
                (!vaultEnabled) ? "0" : String.valueOf((long) bal(p)));
        pm.register("vault_balance_formatted", (p) ->
                (!vaultEnabled) ? "$0" : formatMoney(bal(p)));
        pm.register("vault_currency", (p) ->
                (!vaultEnabled) ? "dollars"
                        : (ownStorage ? msg("economy.currency-name-plural", "dollars")
                                      : economy.currencyNamePlural()));
        pm.register("vault_currency_singular", (p) ->
                (!vaultEnabled) ? "dollar"
                        : (ownStorage ? msg("economy.currency-name", "dollar")
                                      : economy.currencyNameSingular()));

    }

    // ===== TAB COMPLETE =====

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        String cmd = command.getName().toLowerCase();
        String prefix = args.length > 0 ? args[0].toLowerCase() : "";

        switch (cmd) {
            case "pay":
                if (args.length == 1) {
                    fillOnlinePlayerNames(completions, prefix);
                } else if (args.length == 2) {
                    completions.addAll(Arrays.asList("10", "100", "1000"));
                }
                break;
            case "balance":
            case "bal":
            case "money":
                if (args.length == 1) fillOnlinePlayerNames(completions, prefix);
                break;
            case "eco":
                if (args.length == 1) {
                    for (String sub : Arrays.asList("give", "take", "set", "reset")) {
                        if (sub.startsWith(prefix)) completions.add(sub);
                    }
                } else if (args.length == 2) {
                    fillOnlinePlayerNames(completions, prefix);
                } else if (args.length == 3) {
                    completions.addAll(Arrays.asList("100", "1000", "10000"));
                }
                break;
            default:
                break;
        }
        return completions;
    }

    private void fillOnlinePlayerNames(List<String> out, String prefix) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase().startsWith(prefix)) out.add(p.getName());
        }
    }

    // ===== PUBLIC API (для других Java-модулей) =====

    /** Включена ли экономика (Vault или собственная БД). */
    public boolean isEconomyEnabled() {
        return vaultEnabled && (ownStorage || (economy != null && economy.isEnabled()));
    }

    /** Баланс игрока (0, если экономика выключена). */
    public double getBalance(Player player) {
        if (!isEconomyEnabled()) return 0;
        return bal(player);
    }

    /** Снять деньги. true — успех. */
    public boolean withdraw(Player player, double amount, String reason) {
        if (!isEconomyEnabled()) return false;
        return withdrawOp(player, amount);
    }

    /** Начислить деньги. true — успех. */
    public boolean deposit(Player player, double amount, String reason) {
        if (!isEconomyEnabled()) return false;
        return depositOp(player, amount);
    }

    /** Форматированная сумма ("$1,000"). */
    public String format(double amount) {
        if (!isEconomyEnabled()) return String.valueOf(amount);
        return formatMoney(amount);
    }

    // ===== UTILS =====

    private String colorize(String msg) {
        return msg == null ? "" : msg.replace("&", "§");
    }
}