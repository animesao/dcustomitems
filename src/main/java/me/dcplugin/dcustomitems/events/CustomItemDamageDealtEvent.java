package me.dcplugin.dcustomitems.events;

import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Событие нанесения урона кастомным предметом (on_damage_dealt / onDamageDealt).
 *
 * Работает для YAML-предметов (getCustomItem()) и Java API-предметов
 * (getJavaItem()). Стреляет ДО выполнения триггеров/хука при нанесении урона.
 *
 * Если событие отменено — стандартные триггеры/хук не выполняются
 * (сам урон Bukkit не отменяется; для этого меняйте event.getBukkitEvent()).
 */
public class CustomItemDamageDealtEvent extends CustomItemEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final EntityDamageByEntityEvent bukkitEvent;
    private boolean cancelled;

    /** Для YAML-предметов. */
    public CustomItemDamageDealtEvent(Player player, CustomItem customItem, EntityDamageByEntityEvent bukkitEvent) {
        super(player, customItem);
        this.bukkitEvent = bukkitEvent;
    }

    /** Для Java API-предметов (AbstractCustomItem). */
    public CustomItemDamageDealtEvent(Player player, AbstractCustomItem javaItem, EntityDamageByEntityEvent bukkitEvent) {
        super(player, javaItem);
        this.bukkitEvent = bukkitEvent;
    }

    /** Исходное Bukkit-событие (можно менять урон и т.д.). */
    public EntityDamageByEntityEvent getBukkitEvent() {
        return bukkitEvent;
    }

    /** Итоговый урон (шорткат к getBukkitEvent().getDamage()). */
    public double getDamage() {
        return bukkitEvent.getDamage();
    }

    /** Установить урон (шорткат к bukkitEvent.setDamage()). */
    public void setDamage(double damage) {
        bukkitEvent.setDamage(damage);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}