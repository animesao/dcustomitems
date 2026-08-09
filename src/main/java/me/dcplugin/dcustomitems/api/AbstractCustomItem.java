package me.dcplugin.dcustomitems.api;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Абстрактный базовый класс для создания кастомных предметов на Java.
 *
 * Разработчики должны наследовать этот класс и переопределять нужные методы.
 * Каждый предмет должен иметь аннотацию @ItemInfo или переопределять getId().
 *
 * Пример:
 * <pre>
 * public class FireSword extends AbstractCustomItem {
 *
 *     @Override
 *     public String getId() { return "fire_sword"; }
 *
 *     @Override
 *     public String getDisplayName() { return "&cОгненный Меч"; }
 *
 *     @Override
 *     public Material getMaterial() { return Material.NETHERITE_SWORD; }
 *
 *     @Override
 *     public void onLeftClick(PlayerInteractEvent event, Player player) {
 *         player.setFireTicks(100);
 *         ItemAPI.particles(player, Particle.FLAME, 50);
 *         ItemAPI.sound(player, Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
 *     }
 * }
 * </pre>
 */
public abstract class AbstractCustomItem {

    // ===== ОСНОВНЫЕ СВОЙСТВА =====

    /**
     * Уникальный ID предмета (используется в командах и конфигах).
     */
    public abstract String getId();

    /**
     * Отображаемое название предмета (поддерживает цветовые коды &).
     */
    public abstract String getDisplayName();

    /**
     * Материал предмета (например, Material.DIAMOND_SWORD).
     */
    public abstract Material getMaterial();

    /**
     * Описание предмета (lore). Пустой список = без описания.
     */
    public List<String> getLore() {
        return List.of();
    }

    /**
     * Количество предмета.
     */
    public int getAmount() {
        return 1;
    }

    /**
     * Кастомная модель (item-model для 1.21.11).
     */
    public String getItemModel() {
        return null;
    }

    /**
     * CustomModelData (legacy, для старых версий).
     */
    public int getCustomModelData() {
        return -1;
    }

    /**
     * Неломаемость.
     */
    public boolean isUnbreakable() {
        return false;
    }

    /**
     * Свечение чар.
     */
    public boolean isGlowing() {
        return false;
    }

    /**
     * Кулдаун кликов в миллисекундах.
     */
    public long getClickCooldown() {
        return 0;
    }

    /**
     * Разрешение для использования (null = без ограничений).
     */
    public String getPermission() {
        return null;
    }

    /**
     * Можно ли положить предмет.
     */
    public boolean isPlaceable() {
        return false;
    }

    /**
     * Тип предмета (RUNE, TOOL, ARMOR, CONSUMABLE).
     */
    public String getType() {
        return "TOOL";
    }

    /**
     * Слот активации (HAND, OFFHAND, HEAD, CHEST, LEGS, FEET).
     */
    public String getActivationSlot() {
        return "HAND";
    }

    // ===== ДЕЙСТВИЯ =====

    /**
     * Вызывается при ЛКМ (правая кнопка) с предметом.
     */
    public void onRightClick(PlayerInteractEvent event, Player player) {}

    /**
     * Вызывается при ПКМ (левая кнопка) с предметом.
     */
    public void onLeftClick(PlayerInteractEvent event, Player player) {}

    /**
     * Вызывается при экипировке предмета.
     */
    public void onEquip(Player player) {}

    /**
     * Вызывается при снятии предмета.
     */
    public void onUnequip(Player player) {}

    /**
     * Вызывается при убийстве моба/игрока с предметом.
     */
    public void onKill(Player killer, Player victim) {}

    /**
     * Вызывается при смерти игрока с этим предметом.
     */
    public void onDeath(Player player, PlayerDeathEvent event) {}

    /**
     * Вызывается при получении урона (с предметом в руке/броне).
     */
    public void onDamageTaken(EntityDamageEvent event, Player player) {}

    /**
     * Вызывается при нанесении урона (с предметом в руке).
     */
    public void onDamageDealt(EntityDamageByEntityEvent event, Player player) {}

    /**
     * Вызывается при прыжке (с предметом в руке).
     */
    public void onJump(Player player) {}

    /**
     * Вызывается при движении (с предметом в руке).
     */
    public void onMove(PlayerMoveEvent event, Player player) {}

    /**
     * Вызывается при выпадении предмета.
     */
    public void onDrop(Player player, PlayerDropItemEvent event) {}

    /**
     * Вызывается при подборе предмета.
     */
    public void onPickup(Player player) {}

    /**
     * Вызывается при ломании блока с предметом.
     */
    public void onBlockBreak(Player player, BlockBreakEvent event) {}

    /**
     * Вызывается при смене слота (с предметом в руке).
     */
    public void onSlotChange(PlayerItemHeldEvent event, Player player) {}

    /**
     * Вызывается при свопе рук (F).
     */
    public void onSwapHand(PlayerSwapHandItemsEvent event, Player player) {}

    // ===== ПЕРИОДИЧЕСКИЕ ЭФФЕКТЫ =====

    /**
     * Интервал периодических эффектов в тиках (20 тиков = 1 сек).
     * Возвращает 0 если не нужны периодические эффекты.
     */
    public long getPeriodicInterval() {
        return 0;
    }

    /**
     * Вызывается периодически (каждые getPeriodicInterval() тиков).
     * Используйте для эффектов экипировки (броня и т.д).
     */
    public void onPeriodic(Player player) {}

    // ===== УТИЛИТЫ =====

    /**
     * Создаёт ItemStack из параметров этого предмета.
     */
    public ItemStack createItemStack() {
        return ItemAPI.createItem(this);
    }

    /**
     * Проверяет, держит ли игрок этот предмет в руке.
     */
    public boolean isHeldBy(Player player) {
        return ItemAPI.isHolding(player, getId());
    }

    /**
     * Проверяет, экипирован ли этот предмет игроком.
     */
    public boolean isEquippedBy(Player player) {
        return ItemAPI.isEquipped(player, getId());
    }
}
