package me.dcplugin.dcustomitems.events;

import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Событие крафта кастомного предмета в GUI-верстаке (/craft, модуль customcraft).
 *
 * Вызывается в момент клика по слоту результата — ДО списания ингредиентов.
 * Работает для YAML-предметов (getCustomItem()) и Java API-предметов
 * (getJavaItem()).
 *
 * Если событие отменено — крафт не выполняется: ингредиенты остаются в сетке,
 * предпросмотр результата не исчезает. Иначе можно заменить выдаваемый предмет
 * через {@link #setResult(ItemStack)} (например, добавить зачарование) — игрок
 * получит изменённый стек.
 *
 * <pre>
 * {@literal @}EventHandler
 * public void onCraft(CustomItemCraftEvent event) {
 *     if (event.getItemId() != null &amp;&amp; event.getItemId().startsWith("admin-")) {
 *         event.setCancelled(true); // запретить крафт админ-предметов
 *     }
 * }
 * </pre>
 */
public class CustomItemCraftEvent extends CustomItemEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private ItemStack result;
    private boolean cancelled;

    /** Для YAML-предметов. */
    public CustomItemCraftEvent(Player player, CustomItem customItem, ItemStack result) {
        super(player, customItem);
        this.result = result;
    }

    /** Для Java API-предметов (AbstractCustomItem). */
    public CustomItemCraftEvent(Player player, AbstractCustomItem javaItem, ItemStack result) {
        super(player, javaItem);
        this.result = result;
    }

    /**
     * Предмет, который будет выдан игроку. Можете изменить стек —
     * игрок получит именно его (например, добавить зачарование).
     */
    public ItemStack getResult() {
        return result;
    }

    /**
     * Заменить выдаваемый предмет. null игнорируется — останется прежний стек.
     */
    public void setResult(ItemStack result) {
        if (result != null) {
            this.result = result;
        }
    }

    /**
     * Количество выдаваемых предметов (шорткат к result.getAmount()).
     */
    public int getAmount() {
        return result == null ? 0 : result.getAmount();
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
