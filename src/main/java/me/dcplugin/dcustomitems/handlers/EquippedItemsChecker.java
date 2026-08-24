package me.dcplugin.dcustomitems.handlers;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.models.CustomItem;
import me.dcplugin.dcustomitems.utils.ColorUtils;
import me.dcplugin.dcustomitems.utils.EnumCache;
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

        // === Trail particles (каждый тик, без кулдауна) ===
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            int tick = trailTickCounter.getOrDefault(playerId, 0) + 1;
            trailTickCounter.put(playerId, tick);
            spawnTrailParticles(player, tick);
        }

        // === Duration check (каждые 20 тиков = 1 секунда) ===
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            checkItemDurations(player);
        }

        // === World restriction check (при смене мира) ===
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

        // Эффекты для экипированных
        for (String itemId : newlyEquipped) {
            CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
            if (customItem != null) {
                spawnEquipEffects(player, customItem, true);
                if (customItem.hasEquipMessage()) {
                    String msg = ColorUtils.processMessage(player, customItem.getEquipMessage());
                    if (!msg.trim().isEmpty()) player.sendMessage(msg);
                }
                plugin.getTriggerListener().executeEquipTriggers(player, customItem);
            }
        }

        // Эффекты для снятых
        for (String itemId : newlyUnequipped) {
            CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
            if (customItem != null) {
                spawnEquipEffects(player, customItem, false);
                if (customItem.hasUnequipMessage()) {
                    String msg = ColorUtils.processMessage(player, customItem.getUnequipMessage());
                    if (!msg.trim().isEmpty()) player.sendMessage(msg);
                }
                plugin.getTriggerListener().executeUnequipTriggers(player, customItem);
            }
        }

        previousActiveItems.put(playerId, currentActiveItemIds);
    }

    private void checkSlot(ItemStack item, String slotType,
                           Map<PotionEffectType, Integer> totalEffects,
                           Map<org.bukkit.attribute.Attribute, Double> totalAttributes,
                           Set<String> currentActiveItemIds,
                           Set<String> activeSets) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) return;

        String itemId = plugin.getItemHandler().getCustomItemId(item);
        if (itemId == null) return;

        CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
        if (customItem == null) return;

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

    private void spawnEquipEffects(Player player, CustomItem customItem, boolean isEquip) {
        List<String> particles = isEquip ? customItem.getEquipParticles() : customItem.getUnequipParticles();
        List<String> sounds = isEquip ? customItem.getEquipSounds() : customItem.getUnequipSounds();

        for (String particleStr : particles) {
            try {
                String[] parts = particleStr.split(":");
                org.bukkit.Particle particle = EnumCache.getParticle(parts[0]);
                if (particle == null) continue;
                int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 10;
                player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count);
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при спавне частиц: " + particleStr);
            }
        }

        for (String soundStr : sounds) {
            try {
                String[] parts = soundStr.split(":");
                org.bukkit.Sound sound = EnumCache.getSound(parts[0]);
                if (sound == null) continue;
                float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
                float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при воспроизведении звука: " + soundStr);
            }
        }
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

    // === Duration check ===

    private void checkItemDurations(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        boolean changed = false;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == org.bukkit.Material.AIR) continue;

            String itemId = plugin.getItemHandler().getCustomItemId(item);
            if (itemId == null) continue;

            CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
            if (customItem == null || !customItem.hasDuration()) continue;

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
            if (item == null || item.getType() == org.bukkit.Material.AIR) continue;

            String itemId = plugin.getItemHandler().getCustomItemId(item);
            if (itemId == null) continue;

            CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
            if (customItem == null) continue;

            if (!customItem.isAllowedInWorld(worldName)) {
                contents[i] = null;
                changed = true;
                player.sendMessage(ColorUtils.processMessage(player,
                    "&cПредмет " + customItem.getId() + " запрещён в этом мире!"));
            }
        }

        if (changed) {
            player.getInventory().setContents(contents);
            markForRecalculation(player);
        }
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
