package me.dcplugin.dcustomitems.utils;

import me.dcplugin.dcustomitems.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {

    private final Main plugin;
    private final String githubApiUrl;
    private final String githubReleasesUrl;
    private String latestVersion;
    private boolean updateAvailable;

    public UpdateChecker(Main plugin) {
        this.plugin = plugin;
        this.githubApiUrl = "https://api.github.com/repos/animesao/dcustomitems/releases/latest";
        this.githubReleasesUrl = "https://github.com/animesao/dcustomitems/releases";
        this.updateAvailable = false;
    }

    public void checkForUpdates(Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(githubApiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("User-Agent", "DC-CustomItems-Plugin");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

                int responseCode = connection.getResponseCode();
                
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String jsonResponse = response.toString();
                    
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

                connection.disconnect();
            } catch (Exception e) {
                consumer.accept("§cОшибка при проверке обновлений: " + e.getMessage());
                consumer.accept("§7Проверьте обновления вручную: §b" + githubReleasesUrl);
                plugin.getLogger().severe("Update check error: " + e.getMessage());
            }
        });
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
     */
    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        
        int maxLength = Math.max(parts1.length, parts2.length);
        
        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        
        return 0;
    }

    public void notifyPlayer(Player player) {
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
