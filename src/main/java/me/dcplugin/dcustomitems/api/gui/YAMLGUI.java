package me.dcplugin.dcustomitems.api.gui;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * GUI загружаемое из YAML конфига
 * 
 * Пример gui.yml:
 * guis:
 *   shop:
 *     title: "&6Магазин"
 *     size: 27
 *     fill:
 *       material: GRAY_STAINED_GLASS_PANE
 *       name: " "
 *     items:
 *       13:
 *         material: DIAMOND
 *         name: "&bАлмаз"
 *         lore:
 *           - "&7Цена: 100 алмазов"
 *         command: "give %player% diamond 1"
 */
public class YAMLGUI extends CustomGUI {

    private final YamlConfiguration config;
    private final String configPath;

    public YAMLGUI(String id, String title, int size, YamlConfiguration config, String configPath) {
        super(id, title, size);
        this.config = config;
        this.configPath = configPath;
    }

    @Override
    public void onOpen(Player player) {
        // Заполняем фон
        ConfigurationSection fillSection = config.getConfigurationSection(configPath + ".fill");
        if (fillSection != null) {
            String materialName = fillSection.getString("material", "GRAY_STAINED_GLASS_PANE");
            String name = fillSection.getString("name", " ");
            Material material = Material.matchMaterial(materialName.toUpperCase());
            if (material != null) {
                fill(material, name);
            }
        }

        // Устанавливаем предметы
        ConfigurationSection itemsSection = config.getConfigurationSection(configPath + ".items");
        if (itemsSection != null) {
            for (String slotKey : itemsSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(slotKey);
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(slotKey);
                    if (itemSection != null) {
                        loadItem(slot, itemSection);
                    }
                } catch (NumberFormatException e) {
                    // Invalid slot
                }
            }
        }
    }

    private void loadItem(int slot, ConfigurationSection section) {
        String materialName = section.getString("material", "STONE");
        String name = section.getString("name", "&fПредмет");
        List<String> lore = section.getStringList("lore");
        
        Material material = Material.matchMaterial(materialName.toUpperCase());
        if (material == null) material = Material.STONE;
        
        String[] loreArray = lore.toArray(new String[0]);
        setItem(slot, material, name, loreArray);
    }

    @Override
    public void onClick(Player player, int slot, ItemStack item) {
        ConfigurationSection itemsSection = config.getConfigurationSection(configPath + ".items");
        if (itemsSection == null) return;
        
        String slotKey = String.valueOf(slot);
        ConfigurationSection itemSection = itemsSection.getConfigurationSection(slotKey);
        if (itemSection == null) return;
        
        // Выполняем команду
        String command = itemSection.getString("command");
        if (command != null && !command.isEmpty()) {
            command = command.replace("%player%", player.getName());
            player.getServer().dispatchCommand(player.getServer().getConsoleSender(), command);
        }
        
        // Отправляем сообщение
        String message = itemSection.getString("message");
        if (message != null && !message.isEmpty()) {
            message = message.replace("%player%", player.getName());
            player.sendMessage(colorize(message));
        }
        
        // Закрываем GUI
        if (itemSection.getBoolean("close", false)) {
            close(player);
        }
    }
}
