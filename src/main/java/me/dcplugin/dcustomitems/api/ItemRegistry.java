package me.dcplugin.dcustomitems.api;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import me.dcplugin.dcustomitems.api.placeholders.CustomPlaceholder;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ItemRegistry {

    private final Main plugin;
    private final Map<String, AbstractCustomItem> registeredItems;
    private final Map<String, CustomCommand> registeredCommands;
    private final Map<String, CustomPlaceholder> registeredPlaceholders;
    private final JavaItemCompiler compiler;

    public ItemRegistry(Main plugin) {
        this.plugin = plugin;
        this.registeredItems = new LinkedHashMap<>();
        this.registeredCommands = new LinkedHashMap<>();
        this.registeredPlaceholders = new LinkedHashMap<>();
        this.compiler = new JavaItemCompiler(plugin);
    }

    public void loadAll() {
        registeredItems.clear();
        registeredCommands.clear();
        registeredPlaceholders.clear();
        File itemsDir = new File(plugin.getDataFolder(), "items");
        if (!itemsDir.exists()) { itemsDir.mkdirs(); return; }
        loadJarFiles(itemsDir);
        loadJavaFiles(itemsDir);
        plugin.getLogger().info("[API] Loaded: " + registeredItems.size() + " items, " + registeredCommands.size() + " commands, " + registeredPlaceholders.size() + " placeholders");
    }

    public void reload() {
        for (CustomCommand cmd : registeredCommands.values()) cmd.onUnregister();
        for (CustomPlaceholder ph : registeredPlaceholders.values()) ph.onUnregister();
        registeredItems.clear();
        registeredCommands.clear();
        registeredPlaceholders.clear();
        compiler.clear();
        loadAll();
        for (CustomCommand cmd : registeredCommands.values()) {
            try {
                org.bukkit.command.PluginCommand pluginCmd = plugin.getCommand(cmd.getName());
                if (pluginCmd != null) {
                    pluginCmd.setExecutor(plugin.getCommandManager());
                    pluginCmd.setTabCompleter(plugin.getCommandManager());
                }
                cmd.onRegister();
            } catch (Exception e) {}
        }
        for (CustomPlaceholder ph : registeredPlaceholders.values()) {
            plugin.getPlaceholderManager().register(ph.getIdentifier(), (player) -> ph.getValue(player));
            ph.onRegister();
        }
    }

    private void loadJarFiles(File itemsDir) {
        File[] jars = itemsDir.listFiles((d, n) -> n.endsWith(".jar"));
        if (jars != null) for (File jar : jars) loadFromJar(jar);
    }

    private void loadJavaFiles(File itemsDir) {
        File[] files = itemsDir.listFiles((d, n) -> n.endsWith(".java") && !n.startsWith("EXAMPLE-"));
        if (files == null || files.length == 0) return;
        plugin.getLogger().info("[API] Compiling " + files.length + " .java files...");
        JavaItemCompiler.CompileResult result = compiler.compileAll();
        for (String cn : result.items) { AbstractCustomItem item = compiler.createItemInstance(cn); if (item != null) registeredItems.put(item.getId(), item); }
        for (String cn : result.commands) { CustomCommand cmd = compiler.createCommandInstance(cn); if (cmd != null) registeredCommands.put(cmd.getName(), cmd); }
        for (String cn : result.placeholders) { CustomPlaceholder ph = compiler.createPlaceholderInstance(cn); if (ph != null) registeredPlaceholders.put(ph.getIdentifier(), ph); }
    }

    private void loadFromJar(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            List<String> classNames = new ArrayList<>();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) classNames.add(entry.getName().replace("/", ".").replace(".class", ""));
            }
            URL jarUrl = jarFile.toURI().toURL();
            ClassLoader cl = new URLClassLoader(new URL[]{jarUrl}, plugin.getClass().getClassLoader());
            for (String cn : classNames) {
                try {
                    Class<?> clazz = Class.forName(cn, false, cl);
                    if (AbstractCustomItem.class.isAssignableFrom(clazz)) {
                        AbstractCustomItem item = (AbstractCustomItem) clazz.getDeclaredConstructor().newInstance();
                        registeredItems.put(item.getId(), item);
                    }
                } catch (Exception ignored) {}
            }
        } catch (IOException e) {}
    }

    public AbstractCustomItem getItem(String id) { return registeredItems.get(id); }
    public Map<String, AbstractCustomItem> getAllItems() { return Collections.unmodifiableMap(registeredItems); }
    public Set<String> getAllIds() { return registeredItems.keySet(); }
    public int getItemCount() { return registeredItems.size(); }
    public CustomCommand getCommand(String name) { return registeredCommands.get(name.toLowerCase()); }
    public Map<String, CustomCommand> getAllCommands() { return Collections.unmodifiableMap(registeredCommands); }
    public int getCommandCount() { return registeredCommands.size(); }
    public CustomPlaceholder getPlaceholder(String id) { return registeredPlaceholders.get(id.toLowerCase()); }
    public Map<String, CustomPlaceholder> getAllPlaceholders() { return Collections.unmodifiableMap(registeredPlaceholders); }
    public int getPlaceholderCount() { return registeredPlaceholders.size(); }
    public JavaItemCompiler getCompiler() { return compiler; }
    public int getCount() { return getItemCount(); }
}
