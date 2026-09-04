package me.dcplugin.dcustomitems.bootstrap;

import me.dcplugin.dcustomitems.Main;

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
 * Извлекает образцы EXAMPLE-* из .jar плагина в папку items/.
 *
 * Вся функциональность (предметы, команды, модули) живёт в items/ как файлы,
 * которые администратор может удалить, изменить или создать сам. В jar плагина
 * лежат только образцы с префиксом EXAMPLE-* — по умолчанию на чистом сервере
 * копируются именно они (только отсутствующие файлы, ничего не перезаписывая).
 *
 * Чтобы включить предмет/команду/модуль из образца: скопируйте файл (или
 * папку) в items/ и уберите префикс EXAMPLE- из имени — после /ci reload
 * компилятор и загрузчики подхватят его как обычный контент.
 *
 * Правила:
 *  - копируются только отсутствующие файлы (настройки администратора не трогаются);
 *  - копируются только файлы/папки, имя которых начинается с EXAMPLE-;
 *  - папки EXAMPLES/ и _template/ не копируются (справочные материалы для
 *    ручного копирования, в jar они остаются всегда);
 *  - остальной контент репозитория не копируется — это примеры для изучения.
 */
public final class DefaultContentExtractor {

    private static final String RESOURCE_ROOT = "items/";

    private DefaultContentExtractor() {}

    public static void extract(Main plugin) {
        if (!plugin.getConfig().getBoolean("extract-default-modules", true)) {
            plugin.getLogger().info("[Extract] extract-default-modules = false, образцы EXAMPLE-* не копируются");
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
            plugin.getLogger().info("[Extract] Образцы EXAMPLE-* скопированы: " + copied
                    + " файлов. Чтобы включить образец — уберите префикс EXAMPLE- из имени файла/папки");
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
        // По умолчанию копируются только образцы EXAMPLE-*: компилятор и
        // загрузчики их игнорируют, так что items/ заполняется примерами,
        // а не готовым контентом. EXAMPLES/ и _template/ под это правило не
        // попадают и остаются справочными материалами внутри jar.
        return relative.startsWith("EXAMPLE-");
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