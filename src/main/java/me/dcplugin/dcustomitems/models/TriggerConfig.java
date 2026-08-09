package me.dcplugin.dcustomitems.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Конфигурация триггеров для кастомного предмета.
 * Поддерживает старый формат (trigger-actions) и новый (triggers).
 */
public class TriggerConfig {
    
    // Карта триггеров: имя_триггера -> список_действий
    private final Map<String, List<String>> triggers;
    
    // Кулдаун между использованиями (мс)
    private long cooldown;
    
    public TriggerConfig() {
        this.triggers = new HashMap<>();
        this.cooldown = 0;
    }
    
    public TriggerConfig(Map<String, List<String>> triggers, long cooldown) {
        this.triggers = triggers != null ? triggers : new HashMap<>();
        this.cooldown = cooldown;
    }
    
    /**
     * Получить действия для триггера
     */
    public List<String> getActions(String trigger) {
        return triggers.getOrDefault(trigger, List.of());
    }
    
    /**
     * Есть ли действия для триггера
     */
    public boolean hasActions(String trigger) {
        return triggers.containsKey(trigger) && !triggers.get(trigger).isEmpty();
    }
    
    /**
     * Есть ли какие-либо триггеры
     */
    public boolean hasAnyTriggers() {
        return !triggers.isEmpty();
    }
    
    /**
     * Получить все триггеры
     */
    public Map<String, List<String>> getAllTriggers() {
        return triggers;
    }
    
    /**
     * Получить кулдаун
     */
    public long getCooldown() {
        return cooldown;
    }
    
    /**
     * Установить кулдаун
     */
    public void setCooldown(long cooldown) {
        this.cooldown = cooldown;
    }
    
    /**
     * Добавить триггер
     */
    public void addTrigger(String trigger, List<String> actions) {
        triggers.put(trigger, actions);
    }
    
    // === Статические константы для имен триггеров ===
    
    public static final String ON_EQUIP = "on_equip";
    public static final String ON_UNEQUIP = "on_unequip";
    public static final String ON_CLICK_RIGHT = "on_click_right";
    public static final String ON_CLICK_LEFT = "on_click_left";
    public static final String ON_KILL = "on_kill";
    public static final String ON_DEATH = "on_death";
    public static final String ON_DAMAGE_TAKEN = "on_damage_taken";
    public static final String ON_DAMAGE_DEALT = "on_damage_dealt";
    public static final String ON_JUMP = "on_jump";
    public static final String ON_PICKUP = "on_pickup";
    public static final String ON_DROP = "on_drop";
    public static final String ON_SNEAK = "on_sneak";
    public static final String ON_SPRINT = "on_sprint";
    public static final String ON_SWIM = "on_swim";
    public static final String ON_BLOCK_BREAK = "on_block_break";
    public static final String ON_BLOCK_PLACE = "on_block_place";
    
    /**
     * Получить все поддерживаемые триггеры
     */
    public static String[] getSupportedTriggers() {
        return new String[] {
            ON_EQUIP, ON_UNEQUIP, ON_CLICK_RIGHT, ON_CLICK_LEFT,
            ON_KILL, ON_DEATH, ON_DAMAGE_TAKEN, ON_DAMAGE_DEALT,
            ON_JUMP, ON_PICKUP, ON_DROP, ON_SNEAK, ON_SPRINT,
            ON_SWIM, ON_BLOCK_BREAK, ON_BLOCK_PLACE
        };
    }
}
