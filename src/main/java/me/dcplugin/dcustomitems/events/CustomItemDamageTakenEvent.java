package me.dcplugin.dcustomitems.events;

import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Событие получения урона при экипированном/вооружённом кастомном предмете
 * (on_damage_taken / onDamageTaken). Стреляет ДО выполнения триггеров/хука.
 *
 * Если событие отменено — стандартные триггеры/хук не выполняются.
 * Для изменения урона используйте getBukkitEvent()/setDamage().
 */
public class CustomItemDamageTakenEvent extends CustomItemEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final EntityDamageEvent bukkitEvent;
    private boolean cancelled;

    /** Для YAML-предметов. */
    public CustomItemDamageTakenEvent(Player player, CustomItem customItem, EntityDamageEvent bukkitEvent) {
        super(player, customItem);
        this.bukkitEvent = bukkitEvent;
    }

    /** Для Java API-предметов (AbstractCustomItem). */
    public CustomItemDamageTakenEvent(Player player, AbstractCustomItem javaItem, EntityDamageEvent bukkitEvent) {
        super(player, javaItem);
        this.bukkitEvent = bukkitEvent;
    }

    /** Исходное Bukkit-событие (можно менять урон и т.д.). */
    public EntityDamageEvent getBukkitEvent() {
        return bukkitEvent;
    }

    /** Итоговый урон (шорткат). */
    public double getDamage() {
        return bukkitEvent.getDamage();
    }

    /** Установить урон (шорткат). */
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