package me.dcplugin.dcustomitems.events;

import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Событие использования кастомного предмета (ЛКМ/ПКМ).
 *
 * Вызывается перед выполнением действий клика (right/left-click-actions,
 * триггеров и эффектов — для YAML; хуков {@code onRightClick/onLeftClick} —
 * для Java API). Если событие отменено — клик полностью игнорируется:
 * действия, триггеры, хуки и расходование предмета не выполняются.
 */
public class CustomItemUseEvent extends CustomItemEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    public enum UseType {
        RIGHT_CLICK, LEFT_CLICK
    }

    private final UseType useType;
    private boolean cancelled;

    /** Для YAML-предметов. */
    public CustomItemUseEvent(Player player, CustomItem customItem, UseType useType) {
        super(player, customItem);
        this.useType = useType;
    }

    /** Для Java API-предметов (AbstractCustomItem). */
    public CustomItemUseEvent(Player player, AbstractCustomItem javaItem, UseType useType) {
        super(player, javaItem);
        this.useType = useType;
    }

    /**
     * Тип клика: RIGHT_CLICK или LEFT_CLICK.
     */
    public UseType getUseType() {
        return useType;
    }

    /**
     * true если это ПКМ.
     */
    public boolean isRightClick() {
        return useType == UseType.RIGHT_CLICK;
    }

    /**
     * true если это ЛКМ.
     */
    public boolean isLeftClick() {
        return useType == UseType.LEFT_CLICK;
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
