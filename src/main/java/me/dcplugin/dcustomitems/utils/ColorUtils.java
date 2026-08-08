package me.dcplugin.dcustomitems.utils;

import org.bukkit.ChatColor;

public class ColorUtils {

    public static String colorize(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String stripColor(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.stripColor(text);
    }

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
}