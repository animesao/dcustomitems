package me.dcplugin.dcustomitems.bootstrap;

import me.dcplugin.dcustomitems.Main;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Извлекает штатные модули и предметы из .jar плагина в папку items/.
 *
 * Ядро плагина — только компилятор (Java + YAML) и базовая инфраструктура.
 * Вся функциональность (предметы, команды, модули) живёт в items/ как файлы,
 * которые администратор может удалить или изменить. Данный экстрактор делает
 * модульность реальной: на чистом сервере items/ заполняется один раз, а
 * повторные запуски ничего не перезаписывают (только добавляют отсутствующее).
 *
 * Правила:
 *  - копируются только отсутствующие файлы (настройки администратора не трогаются);
 *  - файлы EXAMPLE-* не копируются (это образцы, компилятор их игнорирует);
 *  - папка EXAMPLES/ не копируется (справочные примеры для ручного копирования);
 *  - папка _template/ не копируется (шаблон для ручного копирования);
 *  - модуль vault/ копируется только если на сервере установлен Vault
 *    (иначе его .java не скомпилируется из-за отсутствия VaultAPI).
 */
public final class DefaultContentExtractor {

    private static final String RESOURCE_ROOT = "items/";

    private DefaultContentExtractor() {}

    public static void extract(Main plugin) {
        if (!plugin.getConfig().getBoolean("extract-default-modules", true)) {
            plugin.getLogger().info("[Extract] extract-default-modules = false, штатные модули не копируются");
            return;
        }

        File itemsDir = new File(plugin.getDataFolder(), "items");
        if (!itemsDir.exists()) itemsDir.mkdirs();

        URL resourceUrl = plugin.getClass().getClassLoader().getResource(RESOURCE_ROOT);
        if (resourceUrl == null) {
            plugin.getLogger().warning("[Extract] Не найден ресурс items/ внутри jar");
            return;
        }

        int copied = 0;
        int skipped = 0;
        try {
            if ("file".equals(resourceUrl.getProtocol())) {
                // Режим разработки: ресурсы лежат в исходной папке
                File sourceDir = new File(URLDecoder.decode(resourceUrl.getPath(), StandardCharsets.UTF_8));
                copied = copyDir(plugin, sourceDir, itemsDir);
            } else if ("jar".equals(resourceUrl.getProtocol())) {
                copied = copyFromJar(plugin, resourceUrl, itemsDir);
            } else {
                plugin.getLogger().warning("[Extract] Неизвестный протокол ресурса: " + resourceUrl.getProtocol());
            }
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "[Extract] Ошибка извлечения штатных модулей: " + e.getMessage(), e);
            return;
        }

        if (copied > 0) {
            plugin.getLogger().info("[Extract] Штатные модули скопированы: " + copied
                    + " файлов. Всё функциональное — файлы в items/, удалите их, чтобы отключить.");
        }
    }

    // ===== Из jar =====

    private static int copyFromJar(Main plugin, URL resourceUrl, File itemsDir) throws Exception {
        String spec = resourceUrl.toString();
        int separator = spec.indexOf("!/");
        String jarPath = spec.substring("jar:".length(), separator);
        if (jarPath.startsWith("file:")) jarPath = jarPath.substring("file:".length());
        jarPath = URLDecoder.decode(jarPath, StandardCharsets.UTF_8);

        int copied = 0;
        int skipped = 0;
        try (JarFile jar = new JarFile(new File(jarPath))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(RESOURCE_ROOT) || entry.isDirectory()) continue;

                String relative = name.substring(RESOURCE_ROOT.length());
                if (!shouldExtract(plugin, relative)) continue;

                File target = new File(itemsDir, relative);
                if (target.exists()) {
                    skipped++;
                    continue;
                }

                try (InputStream in = plugin.getResource(name)) {
                    if (in == null) continue;
                    copyStream(in, target);
                    copied++;
                    plugin.getLogger().fine("[Extract] Добавлен модуль: " + relative);
                }
            }
        }
        return copied;
    }

    // ===== Из исходной папки (режим разработки) =====

    private static int copyDir(Main plugin, File sourceDir, File targetRoot) {
        int copied = 0;
        File[] children = sourceDir.listFiles();
        if (children == null) return 0;
        for (File child : children) {
            String relative = child.getName();
            if (child.isDirectory()) {
                // Рекурсивно: путь в items/ собираем из имён подпапок
                File[] nested = child.listFiles();
                if (nested == null) continue;
                for (File nestedFile : nested) {
                    String rel = child.getName() + "/" + nestedFile.getName();
                    if (nestedFile.isDirectory()) {
                        copied += copyDirRecursive(plugin, nestedFile, targetRoot, child.getName() + "/");
                    } else {
                        if (!shouldExtract(plugin, rel)) continue;
                        File target = new File(targetRoot, rel);
                        if (target.exists()) continue;
                        try (InputStream in = new java.io.FileInputStream(nestedFile)) {
                            target.getParentFile().mkdirs();
                            copyStream(in, target);
                            copied++;
                        } catch (Exception ignored) {}
                    }
                }
            } else {
                if (!shouldExtract(plugin, relative)) continue;
                File target = new File(targetRoot, relative);
                if (target.exists()) continue;
                try (InputStream in = new java.io.FileInputStream(child)) {
                    target.getParentFile().mkdirs();
                    copyStream(in, target);
                    copied++;
                } catch (Exception ignored) {}
            }
        }
        return copied;
    }

    private static int copyDirRecursive(Main plugin, File sourceDir, File targetRoot, String prefix) {
        int copied = 0;
        File[] children = sourceDir.listFiles();
        if (children == null) return 0;
        for (File child : children) {
            String relative = prefix + child.getName();
            if (child.isDirectory()) {
                copied += copyDirRecursive(plugin, child, targetRoot, relative + "/");
            } else {
                if (!shouldExtract(plugin, relative)) continue;
                File target = new File(targetRoot, relative);
                if (target.exists()) continue;
                try (InputStream in = new java.io.FileInputStream(child)) {
                    target.getParentFile().mkdirs();
                    copyStream(in, target);
                    copied++;
                } catch (Exception ignored) {}
            }
        }
        return copied;
    }

    // ===== Правила =====

    private static boolean shouldExtract(Main plugin, String relative) {
        if (relative.isEmpty()) return false;
        if (relative.startsWith(".")) return false;
        if (relative.contains("EXAMPLE-")) return false; // образцы, компилятор их игнорирует
        if (relative.toLowerCase().startsWith("examples/")) return false; // справочные примеры (папка EXAMPLES/)
        if (relative.startsWith("_template/")) return false; // шаблон для ручного копирования

        // Модуль vault/ компилируется только при установленном Vault (VaultAPI в classpath)
        if (relative.startsWith("vault/")) {
            if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
                return false;
            }
        }
        return true;
    }

    private static void copyStream(InputStream in, File target) throws Exception {
        File parent = target.getParentFile();
        if (parent != null) parent.mkdirs();
        try (OutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }
}