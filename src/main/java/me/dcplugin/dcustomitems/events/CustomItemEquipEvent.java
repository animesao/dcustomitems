package me.dcplugin.dcustomitems.events;

import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Событие экипировки/снятия кастомного предмета.
 *
 * Вызывается, когда игрок берёт предмет в активирующий слот (или убирает его),
 * перед применением эффектов/сообщений/триггеров экипировки (YAML-предметы)
 * или перед вызовом хука {@code onEquip()/onUnequip()} (Java API-предметы).
 *
 * Если событие отменено, стандартные эффекты экипировки и сообщения
 * не применяются (для equip) и частицы снятия не спавнятся (для unequip),
 * а хук Java-предмета не вызывается.
 */
public class CustomItemEquipEvent extends CustomItemEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final boolean equip;
    private boolean cancelled;

    /** Для YAML-предметов. */
    public CustomItemEquipEvent(Player player, CustomItem customItem, boolean equip) {
        super(player, customItem);
        this.equip = equip;
    }

    /** Для Java API-предметов (AbstractCustomItem). */
    public CustomItemEquipEvent(Player player, AbstractCustomItem javaItem, boolean equip) {
        super(player, javaItem);
        this.equip = equip;
    }

    /**
     * true = предмет экипирован, false = снят.
     */
    public boolean isEquip() {
        return equip;
    }

    /**
     * true = предмет снят.
     */
    public boolean isUnequip() {
        return !equip;
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
