package me.dcplugin.dcustomitems.events;

import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Событие периодического эффекта Java API-предмета (onPeriodic).
 *
 * Стреляет перед каждым вызовом {@code onPeriodic} у экипированного
 * Java-предмета (глобальный чекер экипировки). YAML-предметы периодических
 * эффектов не имеют.
 *
 * Если событие отменено — конкретный вызов {@code onPeriodic} пропускается.
 */
public class CustomItemPeriodicEvent extends CustomItemEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final long interval;
    private boolean cancelled;

    public CustomItemPeriodicEvent(Player player, AbstractCustomItem javaItem, long interval) {
        super(player, javaItem);
        this.interval = interval;
    }

    /** Интервал предмета (тики), см. getPeriodicInterval(). */
    public long getInterval() {
        return interval;
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