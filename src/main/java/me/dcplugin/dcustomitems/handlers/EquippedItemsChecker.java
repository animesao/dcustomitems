package me.dcplugin.dcustomitems.handlers;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.events.CustomItemEquipEvent;
import me.dcplugin.dcustomitems.events.CustomItemPeriodicEvent;
import me.dcplugin.dcustomitems.models.CustomItem;
import me.dcplugin.dcustomitems.utils.ColorUtils;
import me.dcplugin.dcustomitems.utils.EnumCache;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Глобальный таск проверки экипировки.
 * Заменяет per-player runTaskLater на один глобальный BukkitRunnable.
 * Каждый игрок проверяется не чаще чем раз в RECALC_COOLDOWN_MS.
 */
public class EquippedItemsChecker extends BukkitRunnable {

    private final Main plugin;
    private static final long RECALC_COOLDOWN_MS = 150; // 150ms между пересчётами для одного игрока

    // Per-player state
    private final Map<UUID, Long> lastRecalcTime = new HashMap<>();
    private final Map<UUID, String> lastEquipmentHash = new HashMap<>();
    private final Map<UUID, Set<String>> previousActiveItems = new HashMap<>();
    private final Set<UUID> processingPlayers = new HashSet<>();

    // Trail particles: tick counter per player
    private final Map<UUID, Integer> trailTickCounter = new HashMap<>();
    // World tracking for per-item world restrictions
    private final Map<UUID, String> lastWorldName = new HashMap<>();

    // Глобальный счётчик тиков: дорогие проверки выполняются не каждый тик
    private int globalTick = 0;
    private static final int DURATION_CHECK_INTERVAL = 20;  // раз в секунду
    private static final int WORLD_CHECK_INTERVAL = 20;     // раз в секунду

    // Java API-предметы с getPeriodicInterval() > 0: id -> предмет
    // (обновляется раз в секунду; пока пусто — периодический цикл не выполняется)
    private final Map<String, AbstractCustomItem> periodicJavaItems = new HashMap<>();

    public EquippedItemsChecker(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Запустить глобальный таск (вызывать один раз при старте)
     */
    public void start() {
        this.runTaskTimer(plugin, 1L, 1L); // Каждый тик
    }

    /**
     * Пометить игрока для пересчёта (вызывается из событий)
     */
    public void markForRecalculation(Player player) {
        // Просто обнуляем lastRecalcTime чтобы следующий тик его обработал
        lastRecalcTime.put(player.getUniqueId(), 0L);
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        globalTick++;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();

            // Проверяем кулдаун
            Long lastRecalc = lastRecalcTime.get(playerId);
            if (lastRecalc != null && (now - lastRecalc) < RECALC_COOLDOWN_MS) {
                continue;
            }

            // Проверяем флаг обработки
            if (processingPlayers.contains(playerId)) {
                continue;
            }

            // Создаём хеш экипировки
            String currentHash = createEquipmentHash(player);
            String lastHash = lastEquipmentHash.get(playerId);

            // Если ничего не изменилось — пропускаем
            if (Objects.equals(currentHash, lastHash)) {
                continue;
            }

            // Пересчитываем
            processingPlayers.add(playerId);
            try {
                lastRecalcTime.put(playerId, now);
                lastEquipmentHash.put(playerId, currentHash);
                recalculateEquipment(player, playerId);
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "Ошибка при обновлении экипировки для " + player.getName() + ": " + e.getMessage(), e);
            } finally {
                processingPlayers.remove(playerId);
            }
        }

        // === Trail particles + периодические хуки Java API (каждый тик) ===
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            int tick = trailTickCounter.getOrDefault(playerId, 0) + 1;
            trailTickCounter.put(playerId, tick);
            spawnTrailParticles(player, tick);
            runJavaPeriodic(player, tick);
        }

        // Кэш Java-предметов с периодическими эффектами — раз в секунду
        if (globalTick % DURATION_CHECK_INTERVAL == 0) {
            refreshPeriodicJavaItems();
        }

        // === Duration check (раз в секунду, а не каждый тик) ===
        if (globalTick % DURATION_CHECK_INTERVAL == 0) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                checkItemDurations(player);
            }
        }

        // === World restriction check (раз в секунду и только при смене мира) ===
        if (globalTick % WORLD_CHECK_INTERVAL == 0) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                UUID playerId = player.getUniqueId();
                String currentWorld = player.getWorld().getName();
                String prevWorld = lastWorldName.get(playerId);
                if (prevWorld != null && !prevWorld.equals(currentWorld)) {
                    checkWorldRestrictions(player);
                }
                lastWorldName.put(playerId, currentWorld);
            }
        }
    }

    /**
     * Полный пересчёт экипировки для игрока
     */
    private void recalculateEquipment(Player player, UUID playerId) {
        // Удаляем старые эффекты и атрибуты
        plugin.getEffectManager().removeAllEffectsFromPlayer(player);
        plugin.getAttributeManager().removeAllAttributes(player);

        Map<PotionEffectType, Integer> totalEffects = new HashMap<>();
        Map<org.bukkit.attribute.Attribute, Double> totalAttributes = new HashMap<>();
        Set<String> activeSets = new HashSet<>();
        Set<String> currentActiveItemIds = new HashSet<>();

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        ItemStack[] armor = player.getInventory().getArmorContents();

        // Проверяем руки
        checkSlot(mainHand, "HAND", totalEffects, totalAttributes, currentActiveItemIds, activeSets);
        checkSlot(offHand, "OFFHAND", totalEffects, totalAttributes, currentActiveItemIds, activeSets);

        // Проверяем броню
        String[] armorSlots = {"FEET", "LEGS", "CHEST", "HEAD"};
        for (int i = 0; i < armor.length && i < armorSlots.length; i++) {
            checkSlot(armor[i], armorSlots[i], totalEffects, totalAttributes, currentActiveItemIds, activeSets);
        }

        // Сет-бонусы
        for (String setId : activeSets) {
            if (plugin.getArmorSetManager().hasFullSet(player, setId)) {
                plugin.getEffectManager().addSetBonusEffectsToMap(totalEffects, setId);
            }
        }

        // Применяем эффекты и атрибуты
        plugin.getEffectManager().applyEffectsFromMap(player, totalEffects);
        plugin.getAttributeManager().applyAttributesFromMap(player, totalAttributes);

        // Определяем экипированные/снятые предметы
        Set<String> previousIds = previousActiveItems.getOrDefault(playerId, new HashSet<>());
        Set<String> newlyEquipped = new HashSet<>(currentActiveItemIds);
        newlyEquipped.removeAll(previousIds);
        Set<String> newlyUnequipped = new HashSet<>(previousIds);
        newlyUnequipped.removeAll(currentActiveItemIds);

        // Эффекты для экипированных (YAML + Java API)
        for (String itemId : newlyEquipped) {
            CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
            if (customItem != null) {
                // Событие для сторонних плагинов (можно отменить стандартные эффекты)
                CustomItemEquipEvent equipEvent = new CustomItemEquipEvent(player, customItem, true);
                plugin.getServer().getPluginManager().callEvent(equipEvent);
                if (equipEvent.isCancelled()) continue;

                me.dcplugin.dcustomitems.utils.ItemFX.spawnEquipEffects(plugin, player, customItem, true);
                if (customItem.hasEquipMessage()) {
                    String msg = ColorUtils.processMessage(player, customItem.getEquipMessage());
                    if (!msg.trim().isEmpty()) player.sendMessage(msg);
                }
                plugin.getTriggerListener().executeEquipTriggers(player, customItem);
                continue;
            }

            // Java API-предмет: событие (можно отменить) + хук onEquip
            AbstractCustomItem javaItem = plugin.getApiItemRegistry().getItem(itemId);
            if (javaItem != null) {
                CustomItemEquipEvent equipEvent = new CustomItemEquipEvent(player, javaItem, true);
                plugin.getServer().getPluginManager().callEvent(equipEvent);
                if (equipEvent.isCancelled()) continue;
                try {
                    javaItem.onEquip(player);
                } catch (Exception e) {
                    plugin.getLogger().log(java.util.logging.Level.WARNING,
                            "Ошибка onEquip у '" + itemId + "' для " + player.getName() + ": " + e.getMessage(), e);
                }
            }
        }

        // Эффекты для снятых (YAML + Java API)
        for (String itemId : newlyUnequipped) {
            CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
            if (customItem != null) {
                // Событие для сторонних плагинов
                CustomItemEquipEvent unequipEvent = new CustomItemEquipEvent(player, customItem, false);
                plugin.getServer().getPluginManager().callEvent(unequipEvent);
                if (unequipEvent.isCancelled()) continue;

                me.dcplugin.dcustomitems.utils.ItemFX.spawnEquipEffects(plugin, player, customItem, false);
                if (customItem.hasUnequipMessage()) {
                    String msg = ColorUtils.processMessage(player, customItem.getUnequipMessage());
                    if (!msg.trim().isEmpty()) player.sendMessage(msg);
                }
                plugin.getTriggerListener().executeUnequipTriggers(player, customItem);
                continue;
            }

            // Java API-предмет: событие (можно отменить) + хук onUnequip
            AbstractCustomItem javaItem = plugin.getApiItemRegistry().getItem(itemId);
            if (javaItem != null) {
                CustomItemEquipEvent unequipEvent = new CustomItemEquipEvent(player, javaItem, false);
                plugin.getServer().getPluginManager().callEvent(unequipEvent);
                if (unequipEvent.isCancelled()) continue;
                try {
                    javaItem.onUnequip(player);
                } catch (Exception e) {
                    plugin.getLogger().log(java.util.logging.Level.WARNING,
                            "Ошибка onUnequip у '" + itemId + "' для " + player.getName() + ": " + e.getMessage(), e);
                }
            }
        }

        previousActiveItems.put(playerId, currentActiveItemIds);
    }

    private void checkSlot(ItemStack item, String slotType,
                           Map<PotionEffectType, Integer> totalEffects,
                           Map<org.bukkit.attribute.Attribute, Double> totalAttributes,
                           Set<String> currentActiveItemIds,
                           Set<String> activeSets) {
        if (item == null || item.getType() == Material.AIR) return;

        String itemId = plugin.getItemHandler().getCustomItemId(item);
        if (itemId == null) return;

        CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
        if (customItem == null) {
            // Java API-предмет (YAML-модели нет): учитываем только в своём слоте
            AbstractCustomItem javaItem = plugin.getApiItemRegistry().getItem(itemId);
            if (javaItem != null && slotType.equalsIgnoreCase(javaItem.getActivationSlot())) {
                currentActiveItemIds.add(itemId);
            }
            return;
        }

        if (slotType.equals(customItem.getActivationSlot())) {
            plugin.getEffectManager().addEffectsToMap(totalEffects, customItem);
            plugin.getAttributeManager().addAttributesToMap(totalAttributes, customItem);
            currentActiveItemIds.add(itemId);
            if (customItem.getArmorSetId() != null) {
                activeSets.add(customItem.getArmorSetId());
            }
        }
    }

    private String createEquipmentHash(Player player) {
        StringBuilder hash = new StringBuilder(128);
        hash.append("MAIN:").append(getItemId(player.getInventory().getItemInMainHand())).append(";");
        hash.append("OFF:").append(getItemId(player.getInventory().getItemInOffHand())).append(";");

        String[] armorSlots = {"FEET", "LEGS", "CHEST", "HEAD"};
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length && i < armorSlots.length; i++) {
            hash.append(armorSlots[i]).append(":").append(getItemId(armor[i])).append(";");
        }
        return hash.toString();
    }

    private String getItemId(ItemStack item) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) return "null";
        String id = plugin.getItemHandler().getCustomItemId(item);
        return id != null ? id : "vanilla";
    }

    // === Trail particles ===

    private void spawnTrailParticles(Player player, int tick) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        spawnTrailForItem(player, mainHand, tick);
        spawnTrailForItem(player, offHand, tick);
    }

    private void spawnTrailForItem(Player player, ItemStack item, int tick) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) return;

        String itemId = plugin.getItemHandler().getCustomItemId(item);
        if (itemId == null) return;

        CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
        if (customItem == null || !customItem.hasTrailParticles()) return;

        int interval = customItem.getTrailParticleInterval();
        if (interval <= 0 || tick % interval != 0) return;

        for (String particleStr : customItem.getTrailParticles()) {
            try {
                String[] parts = particleStr.split(":");
                org.bukkit.Particle particle = EnumCache.getParticle(parts[0]);
                if (particle == null) continue;
                int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 5;
                player.getWorld().spawnParticle(particle, player.getLocation().add(0, 0.5, 0), count, 0.3, 0.3, 0.3);
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при спавне trail частиц: " + particleStr);
            }
        }
    }

    // === Периодические хуки Java API (onPeriodic) ===

    /**
     * Кэш Java-предметов с getPeriodicInterval() > 0 (раз в секунду).
     */
    private void refreshPeriodicJavaItems() {
        periodicJavaItems.clear();
        for (AbstractCustomItem item : plugin.getApiItemRegistry().getAllItems().values()) {
            if (item != null && item.getPeriodicInterval() > 0) {
                periodicJavaItems.put(item.getId(), item);
            }
        }
    }

    /**
     * Вызов onPeriodic у экипированных Java-предметов по их интервалу.
     * Пустой кэш = возврат без единого PDC-чтения.
     */
    private void runJavaPeriodic(Player player, int tick) {
        if (periodicJavaItems.isEmpty()) return;

        // Читаем PDC-id каждого слота один раз за тик
        Map<String, String> slotIds = new HashMap<>(6);
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        slotIds.put("HAND", plugin.getItemHandler().getCustomItemId(mainHand));
        slotIds.put("OFFHAND", plugin.getItemHandler().getCustomItemId(offHand));

        String[] armorSlots = {"FEET", "LEGS", "CHEST", "HEAD"};
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length && i < armorSlots.length; i++) {
            slotIds.put(armorSlots[i], plugin.getItemHandler().getCustomItemId(armor[i]));
        }

        for (Map.Entry<String, AbstractCustomItem> entry : periodicJavaItems.entrySet()) {
            AbstractCustomItem item = entry.getValue();
            long interval = item.getPeriodicInterval();
            if (interval <= 0 || tick % interval != 0) continue;

            // Предмет должен лежать в своём активирующем слоте
            String slotId = slotIds.get(item.getActivationSlot().toUpperCase());
            if (slotId == null || !slotId.equals(entry.getKey())) continue;

            // Bukkit-событие: отмена пропускает этот вызов onPeriodic
            CustomItemPeriodicEvent periodicEvent = new CustomItemPeriodicEvent(player, item, interval);
            plugin.getServer().getPluginManager().callEvent(periodicEvent);
            if (periodicEvent.isCancelled()) continue;

            try {
                item.onPeriodic(player);
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "Ошибка onPeriodic у '" + entry.getKey() + "' для " + player.getName() + ": " + e.getMessage(), e);
            }
        }
    }

    // === Duration check ===

    private void checkItemDurations(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        boolean changed = false;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) continue;

            String itemId = plugin.getItemHandler().getCustomItemId(item);
            if (itemId == null) continue;

            CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
            if (customItem != null) {
                if (!customItem.hasDuration()) continue;

                // Ensure acquired time is set
                plugin.getItemHandler().ensureAcquiredTime(item);

                if (plugin.getItemHandler().isExpired(item)) {
                    contents[i] = null;
                    changed = true;

                    // Send message
                    if (customItem.hasMaxDurationMessage()) {
                        String msg = ColorUtils.processMessage(player, customItem.getMaxDurationMessage());
                        if (!msg.trim().isEmpty()) player.sendMessage(msg);
                    } else {
                        player.sendMessage(ColorUtils.processMessage(player,
                            "&cПредмет " + customItem.getId() + " исчерпал время жизни!"));
                    }

                    // Play sound and particles
                    player.getWorld().spawnParticle(org.bukkit.Particle.SMOKE,
                        player.getLocation().add(0, 1, 0), 20);
                    player.playSound(player.getLocation(),
                        org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                }
                continue;
            }

            // Java API-предмет: getDuration() в секундах (0 = вечный)
            AbstractCustomItem javaItem = plugin.getApiItemRegistry().getItem(itemId);
            if (javaItem == null || javaItem.getDuration() <= 0) continue;

            plugin.getItemHandler().ensureAcquiredTime(item);
            long elapsed = javaItem.getDuration(); // sentinel, если время не читается
            Long acquired = getAcquiredTime(item);
            if (acquired != null) {
                elapsed = (System.currentTimeMillis() - acquired) / 1000L;
            }
            if (elapsed >= javaItem.getDuration()) {
                contents[i] = null;
                changed = true;
                player.sendMessage(ColorUtils.processMessage(player,
                    javaItem.getDurationExpiredMessage() != null
                            ? javaItem.getDurationExpiredMessage()
                            : "&cПредмет " + itemId + " исчерпал время жизни!"));
                player.getWorld().spawnParticle(org.bukkit.Particle.SMOKE, player.getLocation().add(0, 1, 0), 20);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            }
        }

        if (changed) {
            player.getInventory().setContents(contents);
            markForRecalculation(player);
        }
    }

    // === World restriction check ===

    /**
     * Проверить, разрешены ли предметы игрока в текущем мире
     */
    public void checkWorldRestrictions(Player player) {
        String worldName = player.getWorld().getName();
        ItemStack[] contents = player.getInventory().getContents();
        boolean changed = false;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) continue;

            String itemId = plugin.getItemHandler().getCustomItemId(item);
            if (itemId == null) continue;

            CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
            if (customItem != null) {
                if (!customItem.isAllowedInWorld(worldName)) {
                    contents[i] = null;
                    changed = true;
                    player.sendMessage(ColorUtils.processMessage(player,
                        "&cПредмет " + customItem.getId() + " запрещён в этом мире!"));
                }
                continue;
            }

            // Java API-предмет: isAllowedInWorld(String)
            AbstractCustomItem javaItem = plugin.getApiItemRegistry().getItem(itemId);
            if (javaItem != null && !javaItem.isAllowedInWorld(worldName)) {
                contents[i] = null;
                changed = true;
                player.sendMessage(ColorUtils.processMessage(player,
                    javaItem.getWorldBlockedMessage() != null
                            ? javaItem.getWorldBlockedMessage()
                            : "&cПредмет " + itemId + " запрещён в этом мире!"));
            }
        }

        if (changed) {
            player.getInventory().setContents(contents);
            markForRecalculation(player);
        }
    }

    /**
     * Время приобретения предмета из PDC (null, если не установлено).
     */
    private Long getAcquiredTime(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "acquired_time"),
                org.bukkit.persistence.PersistentDataType.LONG);
    }

    /**
     * Очистка при выходе игрока
     */
    public void onPlayerQuit(UUID playerId) {
        lastRecalcTime.remove(playerId);
        lastEquipmentHash.remove(playerId);
        previousActiveItems.remove(playerId);
        processingPlayers.remove(playerId);
        trailTickCounter.remove(playerId);
        lastWorldName.remove(playerId);
    }

    /**
     * Полная остановка таска
     */
    @Override
    public void cancel() {
        super.cancel();
        lastRecalcTime.clear();
        lastEquipmentHash.clear();
        previousActiveItems.clear();
        processingPlayers.clear();
        trailTickCounter.clear();
        lastWorldName.clear();
    }
}
