package me.dcplugin.dcustomitems.utils;

import me.dcplugin.dcustomitems.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {

    private static final int MAX_RESPONSE_CHARS = 64 * 1024;

    private final Main plugin;
    private final String githubApiUrl;
    private final String githubReleasesUrl;

    // Пишется из async-потока, читается из основного — volatile обязателен,
    // иначе значение могло остаться «старым» из-за кэша процессора.
    private volatile String latestVersion;
    private volatile boolean updateAvailable;

    public UpdateChecker(Main plugin) {
        this.plugin = plugin;
        this.githubApiUrl = "https://api.github.com/repos/animesao/dcustomitems/releases/latest";
        this.githubReleasesUrl = "https://github.com/animesao/dcustomitems/releases";
        this.updateAvailable = false;
    }

    public void checkForUpdates(Consumer<String> consumer) {
        if (consumer == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(githubApiUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("User-Agent", "DC-CustomItems-Plugin");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

                int responseCode = connection.getResponseCode();

                if (responseCode == 200) {
                    String jsonResponse = readResponse(connection);

                    // Parse tag_name from GitHub API response
                    String tagName = extractTag(jsonResponse);

                    if (tagName != null) {
                        // Remove 'v' prefix if present
                        latestVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;
                        String currentVersion = plugin.getDescription().getVersion();

                        plugin.getLogger().info("Current version: " + currentVersion + ", Latest version: " + latestVersion);

                        if (compareVersions(latestVersion, currentVersion) > 0) {
                            updateAvailable = true;
                            consumer.accept("§aНайдено обновление! §7Текущая: §e" + currentVersion + " §7→ Новая: §a" + latestVersion);
                            consumer.accept("§7Скачать: §b" + githubReleasesUrl);
                        } else {
                            consumer.accept("§aВы используете последнюю версию! §7(" + currentVersion + ")");
                        }
                    } else {
                        consumer.accept("§cНе удалось найти информацию о версии");
                    }
                } else if (responseCode == 404) {
                    consumer.accept("§cРелизы не найдены. Проверьте: §b" + githubReleasesUrl);
                } else {
                    consumer.accept("§cНе удалось проверить обновления (код: " + responseCode + ")");
                }
            } catch (Exception e) {
                consumer.accept("§cОшибка при проверке обновлений: " + e.getMessage());
                consumer.accept("§7Проверьте обновления вручную: §b" + githubReleasesUrl);
                plugin.getLogger().log(java.util.logging.Level.WARNING, "Update check error: " + e.getMessage(), e);
            } finally {
                // Соединение закрывалось только в happy-path — при ошибке
                // оставался занятый сокет.
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    /**
     * Читает ответ, ограничивая размер (защита от «раздутого» ответа).
     */
    private static String readResponse(HttpURLConnection connection) throws java.io.IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (response.length() + line.length() > MAX_RESPONSE_CHARS) {
                    break;
                }
                response.append(line);
            }
        }
        return response.toString();
    }

    private String extractTag(String jsonResponse) {
        // Extract "tag_name":"xxx" from JSON
        Pattern pattern = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(jsonResponse);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Compare two version strings (e.g., "1.320.203" vs "1.320.202")
     * Returns: > 0 if v1 > v2, < 0 if v1 < v2, 0 if equal
     *
     * Нечисловые части (например "1.2.3-beta") не роняли проверку:
     * NumberFormatException прерывал всю проверку обновлений.
     */
    private int compareVersions(String v1, String v2) {
        String[] parts1 = splitVersion(v1);
        String[] parts2 = splitVersion(v2);

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? parseNumeric(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseNumeric(parts2[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }

        return 0;
    }

    private String[] splitVersion(String version) {
        if (version == null) return new String[0];
        // Отбрасываем суффиксы сборки: "1.2.3-beta" -> "1.2.3"
        String cleaned = version.trim();
        int dash = cleaned.indexOf('-');
        if (dash > 0) cleaned = cleaned.substring(0, dash);
        return cleaned.split("\\.");
    }

    private int parseNumeric(String part) {
        if (part == null) return 0;
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < part.length(); i++) {
            char c = part.charAt(i);
            if (c < '0' || c > '9') break;
            digits.append(c);
        }
        if (digits.length() == 0) return 0;
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void notifyPlayer(Player player) {
        if (player == null) return;
        if (updateAvailable && player.hasPermission("customitems.update")) {
            player.sendMessage("§8§m                                                    ");
            player.sendMessage("§6§lDC-CustomItems §7- §aДоступно обновление!");
            player.sendMessage("§7Текущая версия: §e" + plugin.getDescription().getVersion());
            player.sendMessage("§7Новая версия: §a" + latestVersion);
            player.sendMessage("§7Скачать: §b" + githubReleasesUrl);
            player.sendMessage("§8§m                                                    ");
        }
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
