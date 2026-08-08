package me.dcplugin.dcustomitems.utils;

import me.dcplugin.dcustomitems.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;

public class UpdateChecker {

    private final Main plugin;
    private final String updateUrl;
    private String latestVersion;
    private boolean updateAvailable;

    public UpdateChecker(Main plugin) {
        this.plugin = plugin;
        this.updateUrl = "https://animesao.spcfy.eu/api/plugins/dc-customitems";
        this.updateAvailable = false;
    }

    public void checkForUpdates(Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(updateUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                // Добавляем заголовки чтобы выглядеть как браузер
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                connection.setRequestProperty("Accept", "application/json, text/plain, */*");
                connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9,ru;q=0.8");
                connection.setRequestProperty("Cache-Control", "no-cache");
                connection.setRequestProperty("Pragma", "no-cache");

                int responseCode = connection.getResponseCode();
                plugin.getLogger().info("Update check response code: " + responseCode);
                
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String jsonResponse = response.toString();
                    plugin.getLogger().info("API Response length: " + jsonResponse.length());
                    
                    // Проверяем если это HTML (защита от DDoS)
                    if (jsonResponse.trim().startsWith("<html") || jsonResponse.contains("javascript")) {
                        consumer.accept("§cСайт защищен от ботов. Проверьте обновления вручную:");
                        consumer.accept("§bhttps://animesao.spcfy.eu/plugins/dc-customitems");
                        plugin.getLogger().warning("Website returned HTML instead of JSON (DDoS protection active)");
                        return;
                    }

                    // Парсим JSON ответ от вашего API
                    if (jsonResponse.contains("\"versions\"") || jsonResponse.contains("\"changelogs\"")) {
                        // Ищем последнюю версию в массиве versions или changelogs
                        String latestVersionFromAPI = parseLatestVersion(jsonResponse);
                        
                        if (latestVersionFromAPI != null) {
                            latestVersion = latestVersionFromAPI;
                            String currentVersion = plugin.getDescription().getVersion();
                            
                            plugin.getLogger().info("Current version: " + currentVersion + ", Latest version: " + latestVersion);
                            
                            if (!currentVersion.equals(latestVersion)) {
                                updateAvailable = true;
                                consumer.accept("§aНайдено обновление! §7Текущая: §e" + currentVersion + " §7→ Новая: §a" + latestVersion);
                                consumer.accept("§7Скачать: §bhttps://animesao.spcfy.eu/plugins/dc-customitems");
                            } else {
                                consumer.accept("§aВы используете последнюю версию! §7(" + currentVersion + ")");
                            }
                        } else {
                            consumer.accept("§cНе удалось найти информацию о версиях в ответе API");
                        }
                    } else {
                        consumer.accept("§cНеверный формат ответа API - ожидался JSON с версиями");
                        plugin.getLogger().warning("Invalid API response format - expected JSON with versions");
                    }
                } else {
                    consumer.accept("§cНе удалось проверить обновления (код: " + responseCode + ")");
                }

                connection.disconnect();
            } catch (Exception e) {
                consumer.accept("§cОшибка при проверке обновлений: " + e.getMessage());
                consumer.accept("§7Проверьте обновления вручную: §bhttps://animesao.spcfy.eu/plugins/dc-customitems");
                plugin.getLogger().severe("Update check error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private String parseLatestVersion(String jsonResponse) {
        try {
            // Сначала ищем массив versions
            int versionsStart = jsonResponse.indexOf("\"versions\":[");
            if (versionsStart != -1) {
                int firstVersionStart = jsonResponse.indexOf("{", versionsStart);
                if (firstVersionStart != -1) {
                    int versionFieldStart = jsonResponse.indexOf("\"version\":\"", firstVersionStart);
                    if (versionFieldStart != -1) {
                        versionFieldStart += 11; // длина "version":"
                        int versionFieldEnd = jsonResponse.indexOf("\"", versionFieldStart);
                        if (versionFieldEnd != -1) {
                            return jsonResponse.substring(versionFieldStart, versionFieldEnd);
                        }
                    }
                }
            }
            
            // Если versions пустой, ищем в changelogs
            int changelogsStart = jsonResponse.indexOf("\"changelogs\":[");
            if (changelogsStart != -1) {
                int firstChangelogStart = jsonResponse.indexOf("{", changelogsStart);
                if (firstChangelogStart != -1) {
                    int versionFieldStart = jsonResponse.indexOf("\"version\":\"", firstChangelogStart);
                    if (versionFieldStart != -1) {
                        versionFieldStart += 11; // длина "version":"
                        int versionFieldEnd = jsonResponse.indexOf("\"", versionFieldStart);
                        if (versionFieldEnd != -1) {
                            return jsonResponse.substring(versionFieldStart, versionFieldEnd);
                        }
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            plugin.getLogger().severe("Error parsing version from API response: " + e.getMessage());
            return null;
        }
    }

    public void notifyPlayer(Player player) {
        if (updateAvailable && player.hasPermission("customitems.update")) {
            player.sendMessage("§8§m                                                    ");
            player.sendMessage("§6§lDC-CustomItems §7- §aДоступно обновление!");
            player.sendMessage("§7Текущая версия: §e" + plugin.getDescription().getVersion());
            player.sendMessage("§7Новая версия: §a" + latestVersion);
            player.sendMessage("§7Скачать: §bhttps://animesao.spcfy.eu/plugins/dc-customitems");
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
