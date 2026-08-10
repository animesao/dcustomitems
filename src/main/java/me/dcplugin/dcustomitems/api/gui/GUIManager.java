package me.dcplugin.dcustomitems.api.gui;

import me.dcplugin.dcustomitems.Main;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Менеджер для управления GUI
 * 
 * Загружает GUI из:
 * 1. Java файлов (items/gui/*.java)
 * 2. YAML конфигов (gui.yml)
 */
public class GUIManager {

    private final Main plugin;
    private final Map<String, CustomGUI> guis = new LinkedHashMap<>();
    private final Map<String, YamlConfiguration> guiConfigs = new LinkedHashMap<>();

    public GUIManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Загрузить все GUI
     */
    public void loadAll() {
        guis.clear();
        guiConfigs.clear();
        
        loadYAMLGUIs();
        
        plugin.getLogger().info("[GUI] Loaded " + guis.size() + " GUIs");
    }

    /**
     * Загрузить GUI из gui.yml
     */
    private void loadYAMLGUIs() {
        File guiFile = new File(plugin.getDataFolder(), "gui.yml");
        if (!guiFile.exists()) {
            plugin.saveResource("gui.yml", false);
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
            
            for (String key : config.getConfigurationSection("guis").getKeys(false)) {
                try {
                    String path = "guis." + key;
                    String title = config.getString(path + ".title", "GUI");
                    int size = config.getInt(path + ".size", 27);
                    
                    YAMLGUI gui = new YAMLGUI(key, title, size, config, path);
                    gui.setPlugin(plugin);
                    guis.put(key, gui);
                    guiConfigs.put(key, config);
                    
                    plugin.getLogger().info("[GUI] Loaded YAML GUI: " + key);
                } catch (Exception e) {
                    plugin.getLogger().warning("[GUI] Error loading GUI: " + key + " - " + e.getMessage());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[GUI] Error loading gui.yml: " + e.getMessage());
        }
    }

    /**
     * Зарегистрировать Java GUI
     */
    public void registerGUI(CustomGUI gui) {
        gui.setPlugin(plugin);
        guis.put(gui.getId(), gui);
        plugin.getLogger().info("[GUI] Registered Java GUI: " + gui.getId());
    }

    /**
     * Открыть GUI по ID
     */
    public boolean openGUI(org.bukkit.entity.Player player, String guiId) {
        CustomGUI gui = guis.get(guiId);
        if (gui == null) {
            player.sendMessage("§cGUI не найдено: " + guiId);
            return false;
        }
        gui.open(player);
        return true;
    }

    /**
     * Получить GUI по ID
     */
    public CustomGUI getGUI(String id) {
        return guis.get(id);
    }

    /**
     * Получить все GUI
     */
    public Map<String, CustomGUI> getAllGUIs() {
        return Collections.unmodifiableMap(guis);
    }

    /**
     * Получить количество GUI
     */
    public int getGUICount() {
        return guis.size();
    }
}
