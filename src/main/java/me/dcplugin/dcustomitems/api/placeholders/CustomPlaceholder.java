package me.dcplugin.dcustomitems.api.placeholders;

import me.dcplugin.dcustomitems.Main;
import org.bukkit.entity.Player;

/**
 * Базовый класс для создания кастомных плейсхолдеров
 * 
 * Пример:
 * <pre>
 * public class KillsPlaceholder extends CustomPlaceholder {
 *     
 *     public KillsPlaceholder() {
 *         super("kills"); // %kills%
 *     }
 *     
 *     @Override
 *     public String getValue(Player player) {
 *         // Получить данные из базы данных
 *         DatabaseManager db = getPlugin().getDatabaseManager();
 *         return String.valueOf(db.queryInt(
 *             "SELECT kills FROM stats WHERE uuid=?", 
 *             player.getUniqueId().toString()
 *         ));
 *     }
 * }
 * </pre>
 * 
 * Использование в любом тексте:
 * "Убийств: %kills%"
 */
public abstract class CustomPlaceholder {

    private String identifier;
    private Main plugin;

    protected CustomPlaceholder() {}

    protected CustomPlaceholder(String identifier) {
        this.identifier = identifier.toLowerCase();
    }

    /**
     * Установить плагин (вызывается автоматически)
     */
    public void setPlugin(Main plugin) {
        this.plugin = plugin;
    }

    public Main getPlugin() {
        return plugin;
    }

    // ===== ОБЯЗАТЕЛЬНЫЙ МЕТОД =====

    /**
     * Получить значение плейсхолдера
     * 
     * @param player Игрок (может быть null для глобальных плейсхолдеров)
     * @return Значение для замены
     */
    public abstract String getValue(Player player);

    // ===== ОПЦИОНАЛЬНЫЕ МЕТОДЫ =====

    /**
     * Вызывается при регистрации
     */
    public void onRegister() {}

    /**
     * Вызывается при отключении
     */
    public void onUnregister() {}

    // ===== ГЕТТЕРЫ/СЕТТЕРЫ =====

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier.toLowerCase(); }
}
