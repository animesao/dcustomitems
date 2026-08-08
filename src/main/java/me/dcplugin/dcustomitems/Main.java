
package me.dcplugin.dcustomitems;

import me.dcplugin.dcustomitems.commands.CustomItemsCommand;
import me.dcplugin.dcustomitems.handlers.CustomItemHandler;
import me.dcplugin.dcustomitems.listeners.PlayerListener;
import me.dcplugin.dcustomitems.listeners.TriggerListener;
import me.dcplugin.dcustomitems.managers.ConfigManager;
import me.dcplugin.dcustomitems.managers.MessageManager;
import me.dcplugin.dcustomitems.managers.EffectManager;
import me.dcplugin.dcustomitems.managers.AttributeManager;
import me.dcplugin.dcustomitems.managers.ArmorSetManager;
import me.dcplugin.dcustomitems.utils.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Main extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private EffectManager effectManager;
    private AttributeManager attributeManager;
    private CustomItemHandler itemHandler;
    private PlayerListener playerListener;
    private TriggerListener triggerListener;
    private ArmorSetManager armorSetManager;
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        try {
            getLogger().info("CustomItems включается...");

            // Сохраняем дефолтные конфиги
            saveDefaultConfig();
            saveResourceItems();

            // Инициализируем менеджеры
            configManager = new ConfigManager(this);
            messageManager = new MessageManager(this);
            effectManager = new EffectManager(this);
            attributeManager = new AttributeManager(this);
            itemHandler = new CustomItemHandler(this);
            armorSetManager = new ArmorSetManager(this);

            // Загружаем кастомные предметы
            itemHandler.loadCustomItems();

            // Регистрируем команды (отложенно для предотвращения ConcurrentModificationException)
            Bukkit.getScheduler().runTaskLater(this, () -> {
                CustomItemsCommand command = new CustomItemsCommand(this);
                getCommand("customitems").setExecutor(command);
                getCommand("customitems").setTabCompleter(command);
            }, 1L);

            // Регистрируем события
            playerListener = new PlayerListener(this);
            triggerListener = new TriggerListener(this);
            getServer().getPluginManager().registerEvents(playerListener, this);
            getServer().getPluginManager().registerEvents(new me.dcplugin.dcustomitems.listeners.BlockPlaceListener(this), this);
            getServer().getPluginManager().registerEvents(triggerListener, this);

            // Проверяем обновления
            updateChecker = new UpdateChecker(this);
            updateChecker.checkForUpdates(message -> {
                String cleanMessage = message.replaceAll("§[0-9a-fk-or]", "");
                if (cleanMessage.contains("Вы используете последнюю версию")) {
                    getLogger().info(cleanMessage);
                } else if (cleanMessage.contains("Доступна новая версия")) {
                    getLogger().warning(cleanMessage);
                }
            });

            getLogger().info("CustomItems успешно включен!");

        } catch (Exception e) {
            getLogger().severe("Ошибка при включении плагина: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("CustomItems отключается...");

        // Останавливаем все активные эффекты
        if (effectManager != null) {
            getServer().getOnlinePlayers().forEach(effectManager::stopPeriodicEffects);
        }

        // Очищаем атрибуты
        if (attributeManager != null) {
            attributeManager.cleanup();
        }

        getLogger().info("CustomItems отключен!");
    }

    private void saveResourceItems() {
        File itemsFolder = new File(getDataFolder(), "items");
        if (!itemsFolder.exists()) {
            itemsFolder.mkdirs();
        }
        
        // Сохраняем все .yml файлы из resources/items/
        try {
            java.util.jar.JarFile jar = new java.util.jar.JarFile(getFile());
            java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                java.util.jar.JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("items/") && name.endsWith(".yml")) {
                    File destFile = new File(getDataFolder(), name);
                    if (!destFile.exists()) {
                        saveResource(name, false);
                    }
                }
            }
            jar.close();
        } catch (Exception e) {
            getLogger().warning("Не удалось сохранить файлы предметов: " + e.getMessage());
        }
    }

    // Геттеры для менеджеров
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }

    public CustomItemHandler getItemHandler() {
        return itemHandler;
    }

    public PlayerListener getPlayerListener() {
        return playerListener;
    }

    public ArmorSetManager getArmorSetManager() {
        return armorSetManager;
    }

    public AttributeManager getAttributeManager() {
        return attributeManager;
    }

    public TriggerListener getTriggerListener() {
        return triggerListener;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
}
