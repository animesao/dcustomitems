package me.dcplugin.dcustomitems.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Level;

/**
 * Доступ к Bukkit CommandMap и его карте известных команд.
 *
 * Bukkit не даёт публичного API для этого, поэтому используется рефлексия.
 * Раньше каждая реализация искала поле commandMap в классе {@code Bukkit},
 * где его нет (поле лежит в CraftServer) — команды модулей и Java-API
 * просто не регистрировались.
 *
 * Результат кэшируется: рефлексия понадобилась один раз.
 */
public final class CommandMapAccessor {

    private CommandMapAccessor() {}

    private static CommandMap commandMap;
    private static Map<String, Command> knownCommands;
    private static boolean resolved;

    /**
     * CommandMap сервера (null, если достучаться не удалось).
     */
    public static synchronized CommandMap getCommandMap() {
        if (resolved) return commandMap;
        resolved = true;
        commandMap = resolveCommandMap();
        return commandMap;
    }

    private static CommandMap resolveCommandMap() {
        Object server = Bukkit.getServer();

        // 1) Paper 1.20.6+: CraftServer#getCommandMap()
        if (server != null) {
            try {
                Method method = server.getClass().getMethod("getCommandMap");
                Object result = method.invoke(server);
                if (result instanceof CommandMap map) {
                    return map;
                }
            } catch (Exception ignored) {
                // Переходим к следующему способу
            }
        }

        // 2) Поле commandMap в классе сервера (Spigot/CraftServer).
        //    Поле нестатическое — читать его нужно у экземпляра сервера.
        if (server != null) {
            CommandMap map = readCommandMapField(server.getClass(), server);
            if (map != null) return map;
        }

        // 3) Исторический вариант: статическое поле в самом классе Bukkit
        try {
            CommandMap map = readCommandMapField(Bukkit.class, null);
            if (map != null) return map;
        } catch (Exception ignored) {
            // Переходим к следующему способу
        }

        Bukkit.getLogger().log(Level.WARNING,
                "[DC-CustomItems] Не удалось получить CommandMap — динамические команды не будут зарегистрированы");
        return null;
    }

    private static CommandMap readCommandMapField(Class<?> owner, Object instance) {
        try {
            Field field = owner.getDeclaredField("commandMap");
            field.setAccessible(true);
            Object value = field.get(instance);
            return value instanceof CommandMap map ? map : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Карта «известных» команд CommandMap.
     *
     * Возвращает null, если достучаться не удалось — раньше в этом случае
     * возвращалась пустая копия, и команды модулей формально «удалялись»,
     * но оставались в CommandMap навсегда.
     */
    public static synchronized Map<String, Command> getKnownCommands(CommandMap map) {
        if (map == null) return null;
        if (knownCommands != null) return knownCommands;

        // 1) Стандартное поле SimpleCommandMap#knownCommands
        try {
            Field field = map.getClass().getDeclaredField("knownCommands");
            field.setAccessible(true);
            Object value = field.get(map);
            if (value instanceof Map<?, ?> raw) {
                knownCommands = castKnownCommands(raw);
                return knownCommands;
            }
        } catch (Exception ignored) {
            // Переходим к перебору полей
        }

        // 2) Перебор всех полей: Paper/Purpur могут переименовать поле
        for (Field field : map.getClass().getDeclaredFields()) {
            if (!Map.class.isAssignableFrom(field.getType())) continue;
            try {
                field.setAccessible(true);
                Object value = field.get(map);
                if (value instanceof Map<?, ?> raw) {
                    Map<String, Command> commands = castKnownCommands(raw);
                    if (commands != null) {
                        knownCommands = commands;
                        return knownCommands;
                    }
                }
            } catch (Exception ignored) {
                // Следующее поле
            }
        }

        Bukkit.getLogger().log(Level.WARNING,
                "[DC-CustomItems] Не удалось получить knownCommands — команды не будут сняты при reload");
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Command> castKnownCommands(Map<?, ?> raw) {
        if (raw.isEmpty()) return null;
        Map.Entry<?, ?> first = raw.entrySet().iterator().next();
        if (!(first.getKey() instanceof String) || !(first.getValue() instanceof Command)) {
            return null;
        }
        return (Map<String, Command>) raw;
    }

    /**
     * Сбросить кэш (на случай смены сервера в тестах).
     */
    public static synchronized void reset() {
        commandMap = null;
        knownCommands = null;
        resolved = false;
    }
}
