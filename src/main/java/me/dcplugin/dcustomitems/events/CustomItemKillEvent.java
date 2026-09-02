package me.dcplugin.dcustomitems.events;

import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Событие убийства игрока (on_kill / onKill) предметом в руке убийцы.
 *
 * Работает для YAML-предметов (getCustomItem()) и Java API-предметов
 * (getJavaItem()). Стреляет ДО выполнения триггеров/хука.
 *
 * Если событие отменено — стандартные триггеры/хук не выполняются.
 * Внимание: как и в текущем обработчике, улавливаются убийства ИГРОКОВ
 * (наниматель — PlayerDeathEvent), убийства мобов сюда не попадают.
 */
public class CustomItemKillEvent extends CustomItemEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player victim;
    private boolean cancelled;

    /** Для YAML-предметов. */
    public CustomItemKillEvent(Player killer, CustomItem customItem, Player victim) {
        super(killer, customItem);
        this.victim = victim;
    }

    /** Для Java API-предметов (AbstractCustomItem). */
    public CustomItemKillEvent(Player killer, AbstractCustomItem javaItem, Player victim) {
        super(killer, javaItem);
        this.victim = victim;
    }

    /** Убитый игрок. */
    public Player getVictim() {
        return victim;
    }

    /** Убийца (совпадает с getPlayer()). */
    public Player getKiller() {
        return getPlayer();
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