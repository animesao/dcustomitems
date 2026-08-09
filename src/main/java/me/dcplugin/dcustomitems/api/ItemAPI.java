package me.dcplugin.dcustomitems.api;

import me.dcplugin.dcustomitems.utils.ColorUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Утилитный класс для работы с кастомными предметами.
 *
 * Предоставляет статические методы для:
 * - Создания предметов
 * - Эффектов зелий
 * - Частиц и звуков
 * - Сообщений и заголовков
 * - Телепортации и урона
 * - Проверки предметов
 */
public class ItemAPI {

    private static NamespacedKey customItemKey;
    private static NamespacedKey itemModelKey;

    /**
     * Инициализация ключей (вызывается при загрузке плагина).
     */
    public static void init(org.bukkit.plugin.java.JavaPlugin plugin) {
        customItemKey = new NamespacedKey(plugin, "custom_item_id");
        itemModelKey = new NamespacedKey(plugin, "item_model");
    }

    // ===== СОЗДАНИЕ ПРЕДМЕТОВ =====

    /**
     * Создаёт ItemStack из AbstractCustomItem.
     */
    public static ItemStack createItem(AbstractCustomItem item) {
        ItemStack stack = new ItemStack(item.getMaterial(), item.getAmount());
        ItemMeta meta = stack.getItemMeta();

        if (meta == null) return stack;

        // Название
        meta.setDisplayName(ColorUtils.colorize(item.getDisplayName()));

        // Описание
        List<String> lore = item.getLore();
        if (!lore.isEmpty()) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(ColorUtils.colorize(line));
            }
            meta.setLore(coloredLore);
        }

        // Неломаемость
        meta.setUnbreakable(item.isUnbreakable());

        // Свечение
        if (item.isGlowing()) {
            meta.addEnchant(Enchantment.SHARPNESS, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        // Item Model (1.21.11)
        String itemModel = item.getItemModel();
        if (itemModel != null && !itemModel.isEmpty()) {
            org.bukkit.inventory.meta.components.CustomModelDataComponent cmd = meta.getCustomModelDataComponent();
            cmd.setStrings(List.of(itemModel));
            meta.setCustomModelDataComponent(cmd);
            meta.getPersistentDataContainer().set(itemModelKey, PersistentDataType.STRING, itemModel);
        } else if (item.getCustomModelData() > 0) {
            meta.setCustomModelData(item.getCustomModelData());
        }

        // Кастомный ID
        meta.getPersistentDataContainer().set(customItemKey, PersistentDataType.STRING, item.getId());

        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Создаёт предмет с атрибутами.
     */
    public static ItemStack createItem(AbstractCustomItem item, Map<Attribute, Double> attributes) {
        ItemStack stack = createItem(item);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            for (Map.Entry<Attribute, Double> entry : attributes.entrySet()) {
                meta.addAttributeModifier(entry.getKey(),
                    new AttributeModifier(UUID.randomUUID(), item.getId(), entry.getValue(),
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    // ===== ПРОВЕРКИ =====

    /**
     * Проверяет, является ли ItemStack кастомным предметом.
     */
    public static boolean isCustomItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta()) return false;
        if (customItemKey == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(customItemKey, PersistentDataType.STRING);
    }

    /**
     * Получает ID кастомного предмета.
     */
    public static String getCustomItemId(ItemStack item) {
        if (!isCustomItem(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(customItemKey, PersistentDataType.STRING);
    }

    /**
     * Проверяет, держит ли игрок предмет с указанным ID.
     */
    public static boolean isHolding(Player player, String itemId) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        return (isCustomItem(main) && itemId.equals(getCustomItemId(main))) ||
               (isCustomItem(off) && itemId.equals(getCustomItemId(off)));
    }

    /**
     * Проверяет, экипирован ли предмет (в руке или броне).
     */
    public static boolean isEquipped(Player player, String itemId) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isCustomItem(item) && itemId.equals(getCustomItemId(item))) return true;
        }
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (isCustomItem(item) && itemId.equals(getCustomItemId(item))) return true;
        }
        return false;
    }

    // ===== ЭФФЕКТЫ =====

    /**
     * Добавляет эффект зелья игроку.
     *
     * @param player Игрок
     * @param type Тип эффекта
     * @param duration Длительность в секундах
     * @param amplifier Уровень (1 = обычный, 2 = усиленный и т.д)
     */
    public static void effect(Player player, PotionEffectType type, int duration, int amplifier) {
        player.addPotionEffect(new PotionEffect(type, duration * 20, amplifier - 1, false, false));
    }

    /**
     * Добавляет эффект скорости.
     */
    public static void speed(Player player, int duration, int level) {
        effect(player, PotionEffectType.SPEED, duration, level);
    }

    /**
     * Добавляет эффект силы.
     */
    public static void strength(Player player, int duration, int level) {
        effect(player, PotionEffectType.STRENGTH, duration, level);
    }

    /**
     * Делает игрока невидимым.
     */
    public static void vanish(Player player, int seconds) {
        effect(player, PotionEffectType.INVISIBILITY, seconds, 1);
    }

    /**
     * Делает игрока светящимся.
     */
    public static void glow(Player player, int seconds) {
        effect(player, PotionEffectType.GLOWING, seconds, 1);
    }

    /**
     * Исцеляет игрока.
     *
     * @param amount Количество сердец (1 сердце = 2 HP)
     */
    public static void heal(Player player, double amount) {
        if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
            double max = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            player.setHealth(Math.min(player.getHealth() + amount * 2, max));
        }
    }

    /**
     * Наносит урон игроку.
     */
    public static void damage(Player player, double amount) {
        player.damage(amount);
    }

    /**
     * Наносит AoE урон по области.
     */
    public static void damageNearby(Player player, double amount, double radius) {
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player && entity != player) {
                ((Player) entity).damage(amount, player);
            }
        }
    }

    /**
     * Исцеляет ближайших игроков.
     */
    public static void healNearby(Player player, double amount, double radius) {
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player && entity != player) {
                Player target = (Player) entity;
                if (target.getAttribute(Attribute.MAX_HEALTH) != null) {
                    double max = target.getAttribute(Attribute.MAX_HEALTH).getValue();
                    target.setHealth(Math.min(target.getHealth() + amount * 2, max));
                }
            }
        }
    }

    // ===== ЧАСТИЦЫ И ЗВУКИ =====

    /**
     * Спавнит частицы у игрока.
     */
    public static void particles(Player player, Particle particle, int count) {
        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count);
    }

    /**
     * Спавнит частицы в указанной локации.
     */
    public static void particlesAt(Location loc, Particle particle, int count) {
        loc.getWorld().spawnParticle(particle, loc, count);
    }

    /**
     * Спавнит частицы с кастомным offset.
     */
    public static void particlesCustom(Player player, Particle particle, int count,
                                        double offsetX, double offsetY, double offsetZ) {
        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count,
            offsetX, offsetY, offsetZ);
    }

    /**
     * Воспроизводит звук для игрока.
     */
    public static void sound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    /**
     * Воспроизводит звук в локации для всех рядом.
     */
    public static void soundAt(Location loc, Sound sound, float volume, float pitch) {
        loc.getWorld().playSound(loc, sound, volume, pitch);
    }

    // ===== СООБЩЕНИЯ =====

    /**
     * Отправляет сообщение игроку (с цветовыми кодами).
     */
    public static void message(Player player, String msg) {
        if (msg != null && !msg.trim().isEmpty()) {
            player.sendMessage(ColorUtils.colorize(msg));
        }
    }

    /**
     * Отправляет заголовок (большой текст по центру).
     */
    public static void title(Player player, String title, String subtitle) {
        player.sendTitle(
            ColorUtils.colorize(title),
            ColorUtils.colorize(subtitle),
            10, 40, 10
        );
    }

    /**
     * Отправляет action bar (над хотбаром).
     */
    public static void actionbar(Player player, String text) {
        player.spigot().sendMessage(
            net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(ColorUtils.colorize(text))
        );
    }

    /**
     * Объявление всем игрокам.
     */
    public static void broadcast(String msg) {
        Bukkit.broadcastMessage(ColorUtils.colorize(msg));
    }

    // ===== ТЕЛЕПОРТАЦИЯ =====

    /**
     * Телепортирует игрока по координатам.
     */
    public static void teleport(Player player, double x, double y, double z) {
        player.teleport(new Location(player.getWorld(), x, y, z));
    }

    /**
     * Телепортирует игрока относительно текущей позиции.
     */
    public static void teleportRelative(Player player, double dx, double dy, double dz) {
        Location loc = player.getLocation();
        player.teleport(new Location(loc.getWorld(), loc.getX() + dx, loc.getY() + dy, loc.getZ() + dz));
    }

    /**
     * Телепортирует к ближайшему игроку.
     */
    public static void teleportToNearest(Player player, double maxRange) {
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other == player) continue;
            double dist = player.getLocation().distance(other.getLocation());
            if (dist < nearestDist && dist <= maxRange) {
                nearestDist = dist;
                nearest = other;
            }
        }

        if (nearest != null) {
            player.teleport(nearest.getLocation());
        }
    }

    // ===== БОЕВЫЕ =====

    /**
     * Удар молнией в месте, куда смотрит игрок.
     */
    public static void lightningForward(Player player, int distance) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();
        Location target = eyeLoc.clone().add(direction.multiply(distance));
        player.getWorld().strikeLightning(target);
    }

    /**
     * Удар молнией в указанной локации.
     */
    public static void lightning(Location loc) {
        loc.getWorld().strikeLightning(loc);
    }

    /**
     * Отбросить ближайших игроков.
     */
    public static void knockbackNearby(Player player, double radius) {
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player && entity != player) {
                Vector dir = entity.getLocation().toVector()
                    .subtract(player.getLocation().toVector()).normalize();
                entity.setVelocity(dir.multiply(1.5).setY(0.5));
            }
        }
    }

    /**
     * Подбросить ближайших игроков.
     */
    public static void launchNearby(Player player, double power, double radius) {
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player && entity != player) {
                entity.setVelocity(entity.getVelocity().add(new Vector(0, power, 0)));
            }
        }
    }

    // ===== ПРЕДМЕТЫ =====

    /**
     * Выдаёт предмет игроку.
     */
    public static void giveItem(Player player, Material material, int amount) {
        player.getInventory().addItem(new ItemStack(material, amount));
    }

    /**
     * Забирает предмет у игрока.
     */
    public static boolean removeItem(Player player, Material material, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material && remaining > 0) {
                int toRemove = Math.min(item.getAmount(), remaining);
                item.setAmount(item.getAmount() - toRemove);
                remaining -= toRemove;
            }
        }
        return remaining == 0;
    }

    /**
     * Выдаёт опыт.
     */
    public static void giveExp(Player player, int amount) {
        player.giveExp(amount);
    }

    /**
     * Выдаёт уровни опыта.
     */
    public static void giveLevels(Player player, int amount) {
        player.giveExpLevels(amount);
    }

    // ===== ОГОНЬ =====

    /**
     * Поджигает игрока.
     */
    public static void setOnFire(Player player, int ticks) {
        player.setFireTicks(ticks);
    }

    /**
     * Поджигает ближайших игроков.
     */
    public static void setNearbyOnFire(Player player, int ticks, double radius) {
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player && entity != player) {
                entity.setFireTicks(ticks);
            }
        }
    }
}
