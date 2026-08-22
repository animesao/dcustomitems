import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import me.dcplugin.dcustomitems.models.CustomItem;
import me.dcplugin.dcustomitems.utils.EnumCache;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Команда /give — выдача предметов (кастомных + ванильных).
 *
 * Использование:
 *   /give <id>                  — выдать себе
 *   /give <id> <player>         — выдать игроку
 *   /give <id> <player> <amount>— выдать N штук
 *   /give <id> all              — выдать всем онлайн
 *   /give list                  — список кастомных предметов
 *   /give materials             — поиск ванильных материалов
 *
 * Поддерживает:
 *   - Кастомные предметы: /give vampire-blade
 *   - Ванильные материалы: /give diamond, /give netherite_sword, /give cooked_beef 64
 */
public class GiveCommand extends CustomCommand {

    public GiveCommand() {
        super("give", "Выдать предмет (кастомный или ванильный)",
              "/give <id|material> [player] [amount]", "dcustomitems.give");
        setAliases("g");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Main plugin = Main.getInstance();
        if (plugin == null) return true;

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        // /give list
        if (args[0].equalsIgnoreCase("list")) {
            return handleList(sender);
        }

        // /give materials <filter>
        if (args[0].equalsIgnoreCase("materials")) {
            return handleMaterials(sender, args.length >= 2 ? args[1] : "");
        }

        String input = args[0];

        // 1. Ищем среди кастомных предметов
        CustomItem customItem = plugin.getItemHandler().getCustomItem(input.toLowerCase());

        // 2. Если не найден — ищем как ванильный материал
        Material vanillaMaterial = null;
        if (customItem == null) {
            vanillaMaterial = EnumCache.getMaterial(input);
        }

        if (customItem == null && vanillaMaterial == null) {
            msg(sender, "&cПредмет '&e" + input + "&c' не найден!");
            msg(sender, "&7Попробуй &e/give list &7или &e/give materials");
            return true;
        }

        // Определяем получателя
        Player target;
        int amount = 1;

        if (args.length >= 2) {
            if (args[1].equalsIgnoreCase("all")) {
                int amt = args.length >= 3 ? parseAmount(args[2], 1) : 1;
                return handleGiveAll(sender, customItem, vanillaMaterial, input, amt);
            }
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                msg(sender, "&cИгрок '&e" + args[1] + "&c' не найден!");
                return true;
            }
            if (args.length >= 3) {
                amount = parseAmount(args[2], 1);
            }
        } else {
            if (!(sender instanceof Player)) {
                msg(sender, "&cУкажите игрока: /give <id> <player>");
                return true;
            }
            target = (Player) sender;
        }

        // Ограничиваем количество
        amount = Math.max(1, Math.min(amount, 64));

        // Создаём ItemStack
        ItemStack itemStack;
        String displayName;

        if (customItem != null) {
            itemStack = customItem.getItemStack().clone();
            displayName = customItem.getId();
        } else {
            itemStack = new ItemStack(vanillaMaterial, amount);
            displayName = vanillaMaterial.name().toLowerCase().replace("_", " ");
        }
        itemStack.setAmount(amount);

        // Выдаём
        target.getInventory().addItem(itemStack);

        // Сообщение
        String targetName = target.equals(sender) ? "себе" : target.getName();
        msg(sender, "&aВыдано &e" + displayName + " &ax" + amount + " &7→ &f" + targetName);

        if (!target.equals(sender)) {
            msg(target, "&aВы получили &e" + displayName + " &ax" + amount);
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, int position) {
        Main plugin = Main.getInstance();
        if (plugin == null) return new ArrayList<>();

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();

            // Команды
            if ("list".startsWith(prefix)) completions.add("list");
            if ("materials".startsWith(prefix)) completions.add("materials");

            // Кастомные предметы
            for (String id : plugin.getItemHandler().getAllCustomItems().keySet()) {
                if (id.toLowerCase().startsWith(prefix)) {
                    completions.add(id);
                }
            }

            // Ванильные материалы (популярные)
            for (Material mat : POPULAR_MATERIALS) {
                String name = mat.name().toLowerCase();
                if (name.startsWith(prefix) || name.replace("_", "").startsWith(prefix.replace("_", ""))) {
                    completions.add(name);
                }
            }

            return completions;
        }

        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            List<String> completions = new ArrayList<>();

            if ("all".startsWith(prefix)) completions.add("all");
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(prefix)) {
                    completions.add(online.getName());
                }
            }

            return completions;
        }

        if (args.length == 3) {
            return Arrays.asList("1", "5", "16", "32", "64");
        }

        return new ArrayList<>();
    }

    // ===== Internal =====

    private boolean handleList(CommandSender sender) {
        Main plugin = Main.getInstance();
        var allItems = plugin.getItemHandler().getAllCustomItems();

        if (allItems.isEmpty()) {
            msg(sender, "&7Нет загруженных кастомных предметов.");
            msg(sender, "&7Используй &e/give <material> &7для ванильных предметов");
            return true;
        }

        msg(sender, "&6=== Кастомные предметы (" + allItems.size() + ") ===");

        for (var entry : allItems.entrySet()) {
            CustomItem item = entry.getValue();
            String type = item.getType() != null ? item.getType() : "UNKNOWN";
            String price = "";
            if (item.isBuyable()) {
                price = " &7[&e$" + String.format("%.0f", item.getBuyPrice()) + "&7]";
            }
            msg(sender, " &e" + entry.getKey() + " &7(" + type + ")" + price);
        }

        return true;
    }

    private boolean handleMaterials(CommandSender sender, String filter) {
        String filterLower = filter.toLowerCase();
        List<Material> found = new ArrayList<>();

        for (Material mat : Material.values()) {
            if (mat.isAir() || !mat.isItem()) continue;
            if (!filterLower.isEmpty() && !mat.name().toLowerCase().contains(filterLower)) continue;
            found.add(mat);
        }

        if (found.isEmpty()) {
            msg(sender, "&cМатериалы с '&e" + filter + "&c' не найдены.");
            return true;
        }

        // Ограничиваем вывод
        int limit = Math.min(found.size(), 50);
        msg(sender, "&6=== Материалы" + (filter.isEmpty() ? "" : " ('" + filter + "')") +
            " [" + found.size() + "]" + (found.size() > limit ? " (первые " + limit + ")" : "") + " ===");

        StringBuilder line = new StringBuilder(" &e");
        for (int i = 0; i < limit; i++) {
            if (i > 0) line.append("&7, &e");
            line.append(found.get(i).name().toLowerCase());
            // Перенос строки каждые 5 предметов
            if ((i + 1) % 5 == 0 && i + 1 < limit) {
                msg(sender, line.toString());
                line = new StringBuilder(" &e");
            }
        }
        if (line.length() > 3) {
            msg(sender, line.toString());
        }

        return true;
    }

    private boolean handleGiveAll(CommandSender sender, CustomItem customItem, Material vanillaMat, String name, int amount) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        int count = 0;

        for (Player target : online) {
            ItemStack itemStack;
            if (customItem != null) {
                itemStack = customItem.getItemStack().clone();
            } else {
                itemStack = new ItemStack(vanillaMat, amount);
            }
            itemStack.setAmount(amount);
            target.getInventory().addItem(itemStack);
            String displayName = customItem != null ? customItem.getId() : vanillaMat.name().toLowerCase().replace("_", " ");
            msg(target, "&aВы получили &e" + displayName + " &ax" + amount);
            count++;
        }

        String displayName = customItem != null ? customItem.getId() : vanillaMat.name().toLowerCase().replace("_", " ");
        msg(sender, "&aВыдано &e" + displayName + " &ax" + amount + " &7→ &fвсем (" + count + " игроков)");

        return true;
    }

    private int parseAmount(String input, int defaultValue) {
        try {
            return Math.max(1, Math.min(64, Integer.parseInt(input)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void sendHelp(CommandSender sender) {
        msg(sender, "&6=== /give — Выдача предметов ===");
        msg(sender, "&e/give <id|material> &7— выдать себе");
        msg(sender, "&e/give <id|material> <player> &7— выдать игроку");
        msg(sender, "&e/give <id|material> <player> <amount> &7— выдать N штук");
        msg(sender, "&e/give <id|material> all &7— выдать всем онлайн");
        msg(sender, "&7");
        msg(sender, "&e/give list &7— список кастомных предметов");
        msg(sender, "&e/give materials &7— все ванильные материалы");
        msg(sender, "&e/give materials diamond &7— поиск по фильтру");
        msg(sender, "&7");
        msg(sender, "&7Примеры:");
        msg(sender, "&e/give vampire-blade &7— кастомный предмет");
        msg(sender, "&e/give diamond &7— алмаз");
        msg(sender, "&e/give netherite_sword Steve 1 &7— незеритовый меч");
        msg(sender, "&e/give cooked_beef all 16 &7— стейк всем");
    }

    // Популярные ванильные материалы для tab-completion
    private static final Material[] POPULAR_MATERIALS = {
        // Алмазы
        Material.DIAMOND, Material.DIAMOND_SWORD, Material.DIAMOND_PICKAXE,
        Material.DIAMOND_AXE, Material.DIAMOND_SHOVEL, Material.DIAMOND_HOE,
        Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
        // Незерит
        Material.NETHERITE_INGOT, Material.NETHERITE_SWORD, Material.NETHERITE_PICKAXE,
        Material.NETHERITE_AXE, Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
        Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
        // Железо
        Material.IRON_INGOT, Material.IRON_SWORD, Material.IRON_PICKAXE,
        Material.IRON_AXE, Material.IRON_HELMET, Material.IRON_CHESTPLATE,
        Material.IRON_LEGGINGS, Material.IRON_BOOTS,
        // Золото
        Material.GOLD_INGOT, Material.GOLDEN_APPLE, Material.GOLDEN_CARROT,
        // Еда
        Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.COOKED_MUTTON,
        Material.COOKED_CHICKEN, Material.COOKED_RABBIT, Material.COOKED_COD,
        Material.BREAD, Material.PUMPKIN_PIE, Material.COOKIE,
        // Зелья
        Material.POTION, Material.SPLASH_POTION, Material.LINGERING_POTION,
        // Ресурсы
        Material.ENDER_PEARL, Material.ENDER_EYE, Material.ELYTRA,
        Material.TOTEM_OF_UNDYING, Material.SHULKER_SHELL, Material.NETHER_STAR,
        Material.DRAGON_EGG, Material.BEACON, Material.CONDUIT,
        // Стройматериалы
        Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.BEDROCK,
        Material.END_STONE, Material.BRICKS, Material.DEEPSLATE_BRICKS,
        // Разное
        Material.BOW, Material.CROSSBOW, Material.SHIELD, Material.TRIDENT,
        Material.FLINT_AND_STEEL, Material.COMPASS, Material.CLOCK,
        Material.EXPERIENCE_BOTTLE, Material.EMERALD, Material.EMERALD_BLOCK,
    };
}
