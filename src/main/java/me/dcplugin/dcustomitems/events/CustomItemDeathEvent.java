package me.dcplugin.dcustomitems.events;

import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Событие смерти игрока с кастомным предметом (on_death / onDeath).
 *
 * Работает для YAML-предметов (getCustomItem()) и Java API-предметов
 * (getJavaItem()). Стреляет ДО выполнения триггеров/хука.
 *
 * Если событие отменено — стандартные триггеры/хук не выполняются
 * (сама смерть Bukkit не отменяется).
 */
public class CustomItemDeathEvent extends CustomItemEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final PlayerDeathEvent deathEvent;
    private boolean cancelled;

    /** Для YAML-предметов. */
    public CustomItemDeathEvent(Player player, CustomItem customItem, PlayerDeathEvent deathEvent) {
        super(player, customItem);
        this.deathEvent = deathEvent;
    }

    /** Для Java API-предметов (AbstractCustomItem). */
    public CustomItemDeathEvent(Player player, AbstractCustomItem javaItem, PlayerDeathEvent deathEvent) {
        super(player, javaItem);
        this.deathEvent = deathEvent;
    }

    /** Умерший игрок (совпадает с getPlayer()). */
    public Player getVictim() {
        return getPlayer();
    }

    /** Исходное Bukkit-событие смерти. */
    public PlayerDeathEvent getDeathEvent() {
        return deathEvent;
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