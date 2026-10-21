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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ItemRegistry {

    private final Main plugin;
    private final Map<String, AbstractCustomItem> registeredItems;
    private final Map<String, CustomCommand> registeredCommands;
    private final Map<String, CustomPlaceholder> registeredPlaceholders;
    private final Map<String, Module> registeredModules;

    /**
     * Зеркало ID предметов в потокобезопасном множестве.
     *
     * Нужно для обращений из потока PlaceholderAPI: обход keySet() обычной
     * LinkedHashMap во время reload дал бы ConcurrentModificationException,
     * а копирование карты на каждый запрос плейсхолдера — лишняя аллокация.
     */
    private final Set<String> registeredItemIds = ConcurrentHashMap.newKeySet();
    private final JavaItemCompiler compiler;
    private final AtomicBoolean loading = new AtomicBoolean(false);
    private final ExecutorService reloadExecutor;

    public ItemRegistry(Main plugin) {
        this.plugin = plugin;
        this.registeredItems = new LinkedHashMap<>();
        this.registeredCommands = new LinkedHashMap<>();
        this.registeredPlaceholders = new LinkedHashMap<>();
        this.registeredModules = new LinkedHashMap<>();
        this.compiler = new JavaItemCompiler(plugin);
        this.reloadExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "DC-CustomItems-Reload");
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Initial load. Must run on the Bukkit main thread. */
    public void loadAll() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("ItemRegistry.loadAll() must run on the Bukkit main thread");
        }

        registeredItems.clear();
        registeredItemIds.clear();
        registeredCommands.clear();
        registeredPlaceholders.clear();
        registeredModules.clear();

        File itemsDir = new File(plugin.getDataFolder(), "items");
        if (!itemsDir.exists()) itemsDir.mkdirs();
        loadJarFiles(itemsDir);
        loadJavaFiles(itemsDir);
        registerJavaRecipes();
        applyRuntimeMessages();

        // Стартовый путь обязан регистрировать команды и плейсхолдеры так же,
        // как и reload: иначе onRegister() не вызывался, а Java-плейсхолдеры
        // не появлялись до первого /ci reload.
        registerCustomCommandsAndPlaceholders();

        logStats();
    }

    /**
     * Вызывает onRegister() у команд и плейсхолдеров и регистрирует
     * плейсхолдеры в PlaceholderManager.
     */
    private void registerCustomCommandsAndPlaceholders() {
        for (CustomCommand cmd : registeredCommands.values()) {
            try {
                cmd.onRegister();
            } catch (Exception exception) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "[API] Error registering command " + cmd.getName(), exception);
            }
        }
        for (CustomPlaceholder ph : registeredPlaceholders.values()) {
            try {
                if (plugin.getPlaceholderManager() != null) {
                    plugin.getPlaceholderManager().registerCustom(ph.getIdentifier(), ph::getValue);
                }
                ph.onRegister();
            } catch (Exception exception) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "[API] Error registering placeholder " + ph.getIdentifier(), exception);
            }
        }
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

        // Отдельный поток вместо общего пула ForkJoin: там же блокирующее
        // ожидание .get() занимало поток пула и могло привести к
        // взаимной блокировке с главным потоком сервера.
        if (reloadExecutor.isShutdown()) {
            loading.set(false);
            throw new IllegalStateException("Плагин выключается — перезагрузка недоступна");
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
        }, reloadExecutor);
    }

    /**
     * Остановить поток перезагрузки (при выключении плагина).
     */
    public void shutdown() {
        reloadExecutor.shutdownNow();
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
            try {
                cmd.onUnregister();
            } catch (Exception exception) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "[API] Error unregistering command " + cmd.getName(), exception);
            }
        }
        for (CustomPlaceholder ph : registeredPlaceholders.values()) {
            try {
                ph.onUnregister();
            } catch (Exception exception) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "[API] Error unregistering placeholder " + ph.getIdentifier(), exception);
            }
            // Снимаем плейсхолдер и с менеджера: иначе старые реализации
            // (а вместе с ними старые классы и classloader) оставались
            // в карте PlaceholderManager навсегда.
            if (plugin.getPlaceholderManager() != null) {
                plugin.getPlaceholderManager().unregisterCustom(ph.getIdentifier());
            }
        }

        registeredItems.clear();
        registeredItemIds.clear();
        registeredCommands.clear();
        registeredPlaceholders.clear();
        registeredModules.clear();
        compiler.clear();

        File itemsDir = new File(plugin.getDataFolder(), "items");
        if (!itemsDir.exists()) itemsDir.mkdirs();
        loadJarFiles(itemsDir);
        loadJavaFiles(itemsDir);
        registerJavaRecipes();
        applyRuntimeMessages();

        registerCustomCommandsAndPlaceholders();
        plugin.registerCustomCommands();
        if (plugin.getModuleManager() != null) {
            plugin.getModuleManager().loadAll();
        }
        logStats();
    }

    /**
     * Рецепты Java API-предметов (getRecipes() -> RecipeDef) регистрируются
     * в ванильный крафт. Метод сам чистит старые java-ключи перед добавлением,
     * поэтому безопасен при повторных reload без перезагрузки YAML.
     *
     * Публичный метод: при старте сервера рецепты перерегистрируются после
     * загрузки YAML-предметов — иначе Java-рецепт, ссылающийся на YAML-предмет,
     * не мог бы разрешить ингредиент.
     */
    public void registerJavaRecipes() {
        try {
            if (plugin.getItemHandler() != null) {
                plugin.getItemHandler().getRecipeManager()
                        .registerJavaItems(registeredItems.values());
            }
        } catch (Exception exception) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "[API] Error registering recipes for Java items", exception);
        }
    }

    /**
     * items/messages.java теперь РАБОЧИЙ конфиг сообщений: после компиляции
     * (компилятор переименовывает класс в items.messagesItem) вызываем его
     * статический load(), который перезаписывает MessagesConfig.*.
     * Вызывается на старте и при /ci reload.
     */
    private void applyRuntimeMessages() {
        try {
            Class<?> clazz = compiler.loadClass("items.messagesItem");
            if (clazz == null) return; // файла нет — встроенные MessagesConfig
            clazz.getMethod("load").invoke(null);
            plugin.getLogger().info("[Messages] items/messages.java применён (runtime load())");
        } catch (java.lang.reflect.InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            plugin.getLogger().warning("[Messages] items/messages.java не применился: " + e.getMessage());
        }
    }

    private void loadJavaFiles(File itemsDir) {
        // Безопасность: runtime-компиляция .java — это исполнение произвольного
        // кода. Держим выключенной, если сервер администрируют несколько человек
        // или есть риск записи в папку плагина извне.
        if (!plugin.getConfig().getBoolean("java-compilation", true)) {
            plugin.getLogger().info(
                    "[API] Runtime Java compilation disabled (java-compilation: false)"
                    + " — .java files in items/ are skipped.");
            return;
        }
        List<File> files = new ArrayList<>();
        collectJavaFiles(itemsDir, files);
        if (files.isEmpty()) return;

        plugin.getLogger().info("[API] Compiling " + files.size() + " .java files...");
        JavaItemCompiler.CompileResult result = compiler.compileAll();

        for (String cn : result.items) {
            AbstractCustomItem item = compiler.createItemInstance(cn);
            if (item != null) {
                registeredItems.put(item.getId(), item);
                registeredItemIds.add(item.getId());
            }
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
                // Образцы и справочные папки (EXAMPLE-*, EXAMPLES/, _template/)
                // не компилируются — это справочный материал.
                String name = child.getName();
                if (name.startsWith("EXAMPLE-")
                        || name.equalsIgnoreCase("EXAMPLES")
                        || name.equalsIgnoreCase("_template")) continue;
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
        // URLClassLoader держит открытый файл jar — без закрытия на Windows
        // файл оставался залоченным, а дескриптор не закрывался до GC.
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
            try (URLClassLoader cl = new URLClassLoader(new URL[]{jarUrl}, plugin.getClass().getClassLoader())) {
                for (String cn : classNames) {
                    try {
                        Class<?> clazz = Class.forName(cn, false, cl);
                        if (AbstractCustomItem.class.isAssignableFrom(clazz)) {
                            AbstractCustomItem item = (AbstractCustomItem) clazz.getDeclaredConstructor().newInstance();
                            registeredItems.put(item.getId(), item);
                        registeredItemIds.add(item.getId());
                        }
                    } catch (Exception exception) {
                        plugin.getLogger().log(java.util.logging.Level.WARNING,
                            "[API] Failed to load class " + cn + " from " + jarFile.getName(), exception);
                    }
                }
            }
        } catch (IOException exception) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                "[API] Failed to read Java item jar " + jarFile.getName(), exception);
        }
    }

    private void logStats() {
        plugin.getLogger().info("[API] Loaded: " + registeredItems.size() + " items, "
            + registeredCommands.size() + " commands, " + registeredPlaceholders.size() + " placeholders"
            + ", " + registeredModules.size() + " modules");
    }

    public AbstractCustomItem getItem(String id) { return registeredItems.get(id); }

    /**
     * Снимок всех зарегистрированных Java-предметов (только для чтения).
     *
     * Раньше возвращался живой unmodifiable-представление карты: любой
     * внешний обход мог упасть с ConcurrentModificationException во время
     * reload и мог менять содержимое карты.
     */
    public Map<String, AbstractCustomItem> getAllItems() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(registeredItems));
    }

    public Set<String> getAllIds() { return new LinkedHashSet<>(registeredItems.keySet()); }

    /**
     * Представление ID предметов без копирования (потокобезопасно для обхода).
     */
    public Set<String> getItemIdsView() { return Collections.unmodifiableSet(registeredItemIds); }
    public int getItemCount() { return registeredItems.size(); }
    public CustomCommand getCommand(String name) { return registeredCommands.get(name.toLowerCase()); }

    /**
     * Снимок всех зарегистрированных команд (только для чтения).
     */
    public Map<String, CustomCommand> getAllCommands() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(registeredCommands));
    }

    public int getCommandCount() { return registeredCommands.size(); }
    public CustomPlaceholder getPlaceholder(String id) { return registeredPlaceholders.get(id.toLowerCase()); }

    /**
     * Снимок всех зарегистрированных плейсхолдеров (только для чтения).
     */
    public Map<String, CustomPlaceholder> getAllPlaceholders() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(registeredPlaceholders));
    }

    public int getPlaceholderCount() { return registeredPlaceholders.size(); }

    /**
     * Снимок всех зарегистрированных модулей (только для чтения).
     */
    public Map<String, Module> getAllModules() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(registeredModules));
    }

    public Module getModule(String id) { return registeredModules.get(id.toLowerCase()); }
    public int getModuleCount() { return registeredModules.size(); }
    public JavaItemCompiler getCompiler() { return compiler; }
    public int getCount() { return getItemCount(); }
    public boolean isLoading() { return loading.get(); }
}
