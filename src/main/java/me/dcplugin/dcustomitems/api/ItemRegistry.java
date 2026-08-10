package me.dcplugin.dcustomitems.api;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import me.dcplugin.dcustomitems.api.modules.Module;
import me.dcplugin.dcustomitems.api.placeholders.CustomPlaceholder;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ItemRegistry {

    private final Main plugin;
    private final Map<String, AbstractCustomItem> registeredItems;
    private final Map<String, CustomCommand> registeredCommands;
    private final Map<String, CustomPlaceholder> registeredPlaceholders;
    private final Map<String, Module> registeredModules;
    private final JavaItemCompiler compiler;
    private final AtomicBoolean loading = new AtomicBoolean(false);

    public ItemRegistry(Main plugin) {
        this.plugin = plugin;
        this.registeredItems = new LinkedHashMap<>();
        this.registeredCommands = new LinkedHashMap<>();
        this.registeredPlaceholders = new LinkedHashMap<>();
        this.registeredModules = new LinkedHashMap<>();
        this.compiler = new JavaItemCompiler(plugin);
    }

    /** Initial load. Must run on the Bukkit main thread. */
    public void loadAll() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("ItemRegistry.loadAll() must run on the Bukkit main thread");
        }

        registeredItems.clear();
        registeredCommands.clear();
        registeredPlaceholders.clear();
        registeredModules.clear();

        File itemsDir = new File(plugin.getDataFolder(), "items");
        if (!itemsDir.exists()) itemsDir.mkdirs();
        loadJarFiles(itemsDir);
        loadJavaFiles(itemsDir);
        logStats();
    }

    /**
     * Asynchronous API kept for callers outside the server thread.
     * Bukkit/plugin operations are marshalled back to the main thread.
     */
    public CompletableFuture<Void> reloadAsync() {
        if (!loading.compareAndSet(false, true)) {
            plugin.getLogger().info("[API] Already loading, skipping...");
            return CompletableFuture.completedFuture(null);
        }
        if (Bukkit.isPrimaryThread()) {
            try {
                reloadSynchronously();
                return CompletableFuture.completedFuture(null);
            } finally {
                loading.set(false);
            }
        }

        return CompletableFuture.runAsync(() -> {
            try {
                Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                    reloadSynchronously();
                    return null;
                }).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Reload interrupted", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("Reload failed", e.getCause());
            } finally {
                loading.set(false);
            }
        });
    }

    /** Synchronous reload used by /ci reload on the main Bukkit thread. */
    public void reload() {
        if (!loading.compareAndSet(false, true)) {
            plugin.getLogger().info("[API] Already loading, skipping...");
            return;
        }

        try {
            if (!Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                    reloadSynchronously();
                    return null;
                }).get();
            } else {
                reloadSynchronously();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Reload interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Reload failed", e.getCause());
        } finally {
            loading.set(false);
        }
    }

    private void reloadSynchronously() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Reload must run on the Bukkit main thread");
        }

        if (plugin.getModuleManager() != null) {
            plugin.getModuleManager().unloadAll();
        }

        // Remove dynamic Bukkit command nodes before dropping the old classloader.
        // Otherwise a reload can retain command objects backed by stale Java classes.
        plugin.unregisterCustomCommands();
        for (CustomCommand cmd : registeredCommands.values()) {
            try { cmd.onUnregister(); } catch (Exception ignored) {}
        }
        for (CustomPlaceholder ph : registeredPlaceholders.values()) {
            try { ph.onUnregister(); } catch (Exception ignored) {}
        }

        registeredItems.clear();
        registeredCommands.clear();
        registeredPlaceholders.clear();
        registeredModules.clear();
        compiler.clear();

        File itemsDir = new File(plugin.getDataFolder(), "items");
        if (!itemsDir.exists()) itemsDir.mkdirs();
        loadJarFiles(itemsDir);
        loadJavaFiles(itemsDir);

        for (CustomCommand cmd : registeredCommands.values()) {
            try { cmd.onRegister(); } catch (Exception ignored) {}
        }
        for (CustomPlaceholder ph : registeredPlaceholders.values()) {
            try {
                if (plugin.getPlaceholderManager() != null) {
                    plugin.getPlaceholderManager().register(ph.getIdentifier(), ph::getValue);
                }
                ph.onRegister();
            } catch (Exception ignored) {}
        }

        plugin.registerCustomCommands();
        if (plugin.getModuleManager() != null) {
            plugin.getModuleManager().loadAll();
        }
        logStats();
    }

    private void loadJavaFiles(File itemsDir) {
        List<File> files = new ArrayList<>();
        collectJavaFiles(itemsDir, files);
        if (files.isEmpty()) return;

        plugin.getLogger().info("[API] Compiling " + files.size() + " .java files...");
        JavaItemCompiler.CompileResult result = compiler.compileAll();

        for (String cn : result.items) {
            AbstractCustomItem item = compiler.createItemInstance(cn);
            if (item != null) registeredItems.put(item.getId(), item);
        }
        for (String cn : result.commands) {
            CustomCommand cmd = compiler.createCommandInstance(cn);
            if (cmd != null) registeredCommands.put(cmd.getName(), cmd);
        }
        for (String cn : result.modules) {
            Module module = compiler.createModuleInstance(cn);
            if (module != null) registeredModules.put(module.getId().toLowerCase(), module);
        }
        for (String cn : result.placeholders) {
            CustomPlaceholder ph = compiler.createPlaceholderInstance(cn);
            if (ph != null) registeredPlaceholders.put(ph.getIdentifier(), ph);
        }

        for (CustomCommand cmd : registeredCommands.values()) {
            plugin.getLogger().info("[API] Command: /" + cmd.getName());
        }
    }

    private void collectJavaFiles(File directory, List<File> files) {
        File[] children = directory.listFiles();
        if (children == null) return;
        Arrays.sort(children, Comparator.comparing(File::getPath));
        for (File child : children) {
            if (child.isDirectory()) {
                collectJavaFiles(child, files);
            } else if (child.getName().endsWith(".java") && !child.getName().startsWith("EXAMPLE-")) {
                files.add(child);
            }
        }
    }

    private void loadJarFiles(File itemsDir) {
        File[] jars = itemsDir.listFiles((d, n) -> n.endsWith(".jar"));
        if (jars != null) for (File jar : jars) loadFromJar(jar);
    }

    private void loadFromJar(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            List<String> classNames = new ArrayList<>();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    classNames.add(entry.getName().replace("/", ".").replace(".class", ""));
                }
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
        } catch (IOException ignored) {}
    }

    private void logStats() {
        plugin.getLogger().info("[API] Loaded: " + registeredItems.size() + " items, "
            + registeredCommands.size() + " commands, " + registeredPlaceholders.size() + " placeholders"
            + ", " + registeredModules.size() + " modules");
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
    public Map<String, Module> getAllModules() { return Collections.unmodifiableMap(registeredModules); }
    public Module getModule(String id) { return registeredModules.get(id.toLowerCase()); }
    public int getModuleCount() { return registeredModules.size(); }
    public JavaItemCompiler getCompiler() { return compiler; }
    public int getCount() { return getItemCount(); }
    public boolean isLoading() { return loading.get(); }
}
