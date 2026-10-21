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
 * Extracts the EXAMPLE-* samples from the plugin .jar into the items/ folder.
 *
 * All functionality (items, commands, modules) lives in items/ as files that
 * the administrator can delete, modify or create themselves. The plugin jar
 * contains only samples with the EXAMPLE-* prefix — by default on a clean
 * server they are NOT copied (extract-default-modules: false). With the option
 * enabled only missing files are copied, nothing is overwritten.
 *
 * To enable an item/command/module from a sample: copy the file (or folder)
 * into items/ and remove the EXAMPLE- prefix from the name — after /ci reload
 * the compiler and the loaders pick it up as regular content.
 *
 * Rules:
 *  - only missing files are copied (the administrator's settings are left alone);
 *  - only files/folders whose name starts with EXAMPLE- are copied;
 *  - the EXAMPLES/ and _template/ folders are not copied (reference material for
 *    manual copying, they always stay in the jar);
 *  - the rest of the repository content is not copied — it is examples for study.
 */
public final class DefaultContentExtractor {

    private static final String RESOURCE_ROOT = "items/";

    private DefaultContentExtractor() {}

    public static void extract(Main plugin) {
        if (!plugin.getConfig().getBoolean("extract-default-modules", false)) {
            plugin.getLogger().info("[Extract] extract-default-modules = false, EXAMPLE-* samples are not copied (the items/ folder is left as is)");
            return;
        }

        File itemsDir = new File(plugin.getDataFolder(), "items");
        if (!itemsDir.exists()) itemsDir.mkdirs();

        URL resourceUrl = plugin.getClass().getClassLoader().getResource(RESOURCE_ROOT);
        if (resourceUrl == null) {
            plugin.getLogger().warning("[Extract] items/ resource not found inside the jar");
            return;
        }

        int copied = 0;
        int skipped = 0;
        try {
            if ("file".equals(resourceUrl.getProtocol())) {
                // Development mode: the resources live in the source folder
                File sourceDir = new File(URLDecoder.decode(resourceUrl.getPath(), StandardCharsets.UTF_8));
                copied = copyDir(plugin, sourceDir, itemsDir);
            } else if ("jar".equals(resourceUrl.getProtocol())) {
                copied = copyFromJar(plugin, resourceUrl, itemsDir);
            } else {
                plugin.getLogger().warning("[Extract] Unknown resource protocol: " + resourceUrl.getProtocol());
            }
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "[Extract] Error while extracting bundled modules: " + e.getMessage(), e);
            return;
        }

        if (copied > 0) {
            plugin.getLogger().info("[Extract] EXAMPLE-* samples copied: " + copied
                    + " files. To enable a sample - remove the EXAMPLE- prefix from the file/folder name");
        }
    }

    // ===== From the jar =====

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
                    plugin.getLogger().fine("[Extract] Added module: " + relative);
                }
            }
        }
        return copied;
    }

    // ===== From the source folder (development mode) =====

    private static int copyDir(Main plugin, File sourceDir, File targetRoot) {
        int copied = 0;
        File[] children = sourceDir.listFiles();
        if (children == null) return 0;
        for (File child : children) {
            String relative = child.getName();
            if (child.isDirectory()) {
                // Recursive: the path in items/ is built from the subfolder names
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

    // ===== Rules =====

    private static boolean shouldExtract(Main plugin, String relative) {
        if (relative.isEmpty()) return false;
        if (relative.startsWith(".")) return false;
        // By default only the EXAMPLE-* samples are copied: the compiler and
        // the loaders ignore them, so items/ gets filled with examples
        // instead of ready-to-use content. EXAMPLES/ and _template/ do not
        // match this rule and stay as reference material inside the jar.
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
