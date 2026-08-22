package me.dcplugin.dcustomitems.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Утилиты для обработки сообщений.
 * Поддерживает цветовые коды и PlaceholderAPI замены.
 */
public class ColorUtils {

    /**
     * Заменяет цветовые коды (& -> §).
     */
    public static String colorize(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Обрабатывает сообщение: цветовые коды + PlaceholderAPI плейсхолдеры.
     * Центральный метод для обработки всех сообщений плагина.
     *
     * @param player Игрок (для PAPI плейсхолдеров, может быть null)
     * @param message Сообщение с цветовыми кодами и плейсхолдерами
     * @return Обработанное сообщение
     */
    public static String processMessage(Player player, String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        // 1. PlaceholderAPI замены (если доступен и есть игрок)
        if (player != null && isPlaceholderApiAvailable()) {
            try {
                message = PlaceholderAPI.setPlaceholders(player, message);
            } catch (Exception ignored) {
                // Если PAPI упал — продолжаем с цветовыми кодами
            }
        }

        // 2. Цветовые коды
        return colorize(message);
    }

    /**
     * Обрабатывает заголовок и подзаголовок для sendTitle.
     */
    public static String[] processTitle(Player player, String title, String subtitle) {
        return new String[]{
            processMessage(player, title),
            processMessage(player, subtitle)
        };
    }

    /**
     * Убирает цветовые коды из текста.
     */
    public static String stripColor(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.stripColor(text);
    }

    /**
     * Форматирует время в читаемый вид.
     */
    public static String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "с";
        } else if (seconds < 3600) {
            return (seconds / 60) + "м " + (seconds % 60) + "с";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            long secs = seconds % 60;
            return hours + "ч " + minutes + "м " + secs + "с";
        }
    }

    // ===== PlaceholderAPI detection =====

    private static Boolean placeholderApiAvailable = null;

    /**
     * Проверяет наличие PlaceholderAPI (результат кэшируется).
     */
    private static boolean isPlaceholderApiAvailable() {
        if (placeholderApiAvailable == null) {
            try {
                Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                placeholderApiAvailable = true;
            } catch (ClassNotFoundException e) {
                placeholderApiAvailable = false;
            }
        }
        return placeholderApiAvailable;
    }

    /**
     * Сбросить кэш PAPI (вызывать при /reload если нужно).
     */
    public static void resetCache() {
        placeholderApiAvailable = null;
    }
}
