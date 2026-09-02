package me.dcplugin.dcustomitems.events;

import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Базовое событие кастомных предметов.
 *
 * События работают и для YAML-предметов (модель {@link CustomItem}),
 * и для Java-предметов из API ({@link AbstractCustomItem}).
 *
 * Сторонние плагины могут слушать конкретные события:
 * <pre>
 * public class MyListener implements Listener {
 *     {@literal @}EventHandler
 *     public void onEquip(CustomItemEquipEvent event) {
 *         event.setCancelled(true); // запретить экипировку/эффекты
 *     }
 *
 *     {@literal @}EventHandler
 *     public void onUse(CustomItemUseEvent event) {
 *         Player player = event.getPlayer();
 *         String itemId = event.getItemId();          // общий ID
 *         if (event.getJavaItem() != null) {           // Java API предмет
 *             AbstractCustomItem javaItem = event.getJavaItem();
 *         } else {                                     // YAML предмет
 *             CustomItem item = event.getCustomItem();
 *         }
 *     }
 * }
 * </pre>
 */
public abstract class CustomItemEvent extends Event {

    private final Player player;
    private final CustomItem customItem;
    private final AbstractCustomItem javaItem;

    /** Для YAML-предметов. */
    protected CustomItemEvent(Player player, CustomItem customItem) {
        this(player, customItem, null);
    }

    /** Для Java API-предметов (AbstractCustomItem). */
    protected CustomItemEvent(Player player, AbstractCustomItem javaItem) {
        this(player, null, javaItem);
    }

    private CustomItemEvent(Player player, CustomItem customItem, AbstractCustomItem javaItem) {
        this.player = player;
        this.customItem = customItem;
        this.javaItem = javaItem;
    }

    /**
     * Игрок, который экипировал / использовал предмет.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Модель кастомного предмета (из YAML). {@code null}, если предмет
     * создан через Java API — тогда смотрите {@link #getJavaItem()}.
     */
    public CustomItem getCustomItem() {
        return customItem;
    }

    /**
     * Java API-предмет (AbstractCustomItem). {@code null}, если предмет
     * определён в YAML — тогда смотрите {@link #getCustomItem()}.
     */
    public AbstractCustomItem getJavaItem() {
        return javaItem;
    }

    /**
     * true, если событие относится к Java API-предмету.
     */
    public boolean isJavaItem() {
        return javaItem != null;
    }

    /**
     * ID кастомного предмета (например "vampire-blade").
     */
    public String getItemId() {
        if (customItem != null) return customItem.getId();
        return javaItem != null ? javaItem.getId() : null;
    }
}
