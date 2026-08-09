package me.dcplugin.dcustomitems.managers;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.config.MessagesConfig;
import me.dcplugin.dcustomitems.utils.ColorUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class MessageManager {

    private final Main plugin;
    private FileConfiguration messages;
    private File messagesFile;

    public MessageManager(Main plugin) {
        this.plugin = plugin;
        // No longer loading messages.yml - use MessagesConfig.java instead
    }

    public void reloadMessages() {
        // Messages are now managed via MessagesConfig.java in items/
    }

    public String getMessage(String path, String defaultMessage) {
        // Try to get from MessagesConfig first, then fallback
        return ColorUtils.colorize(defaultMessage);
    }

    public String getMessage(String path) {
        return getMessage(path, "");
    }

    public void saveMessages() {
        // No longer needed
    }
}
