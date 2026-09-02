package me.dcplugin.dcustomitems.utils;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.models.CustomItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

/**
 * Единственный исполнитель действий кастомных предметов.
 *
 * И PlayerListener, и TriggerListener вызывают только {@link #execute(Player, String)}.
 * Действия можно писать с дефисами или подчёркиваниями — они нормализуются
 * (например {@code damage-mobs} и {@code damage_mobs} это одно и то же).
 *
 * Зарегистрированные действия:
 *   message, title, actionbar, announce/broadcast,
 *   effect, heal, damage, damage_nearby, damage_mobs, damage_players, heal_nearby,
 *   effect_nearby (+ _mobs / _players),
 *   teleport, teleport_relative, teleport_sequence,
 *   give (материал или кастомный предмет), remove, exp,
 *   lightning, lightning_forward, particle(s), sound, fireworks,
 *   sethealth, setfood, set_xp, vanish, glow, speed, flight,
 *   knockback, launch, stun (+ _mobs / _players),
 *   command (от консоли), console_command, break,
 *   particles_custom, sound_sequence, title_sequence, command_sequence,
 *   effect_sequence, damage_custom, heal_custom.
 */
public class ActionParser {

    // Маппинг кастомных названий эффектов на Minecraft
    // Лениво инициализируется: статические ссылки на PotionEffectType
    // требуют инициализированного Bukkit и ломали бы чистые юнит-тесты.
    private static final Map<String, PotionEffectType> EFFECT_MAP = new HashMap<>();

    private static Map<String, PotionEffectType> effectMap() {
        if (EFFECT_MAP.isEmpty()) {
            // Стандартные названия
            putEffect("SPEED", PotionEffectType.SPEED);
            putEffect("SLOW", PotionEffectType.SLOWNESS);
            putEffect("SLOWNESS", PotionEffectType.SLOWNESS);
            putEffect("HASTE", PotionEffectType.HASTE);
            putEffect("MINING_FATIGUE", PotionEffectType.MINING_FATIGUE);
            putEffect("STRENGTH", PotionEffectType.STRENGTH);
            putEffect("INCREASE_DAMAGE", PotionEffectType.STRENGTH);
            putEffect("JUMP", PotionEffectType.JUMP_BOOST);
            putEffect("JUMP_BOOST", PotionEffectType.JUMP_BOOST);
            putEffect("NAUSEA", PotionEffectType.NAUSEA);
            putEffect("CONFUSION", PotionEffectType.NAUSEA);
            putEffect("REGENERATION", PotionEffectType.REGENERATION);
            putEffect("RESISTANCE", PotionEffectType.RESISTANCE);
            putEffect("DAMAGE_RESISTANCE", PotionEffectType.RESISTANCE);
            putEffect("FIRE_RESISTANCE", PotionEffectType.FIRE_RESISTANCE);
            putEffect("WATER_BREATHING", PotionEffectType.WATER_BREATHING);
            putEffect("INVISIBILITY", PotionEffectType.INVISIBILITY);
            putEffect("BLINDNESS", PotionEffectType.BLINDNESS);
            putEffect("NIGHT_VISION", PotionEffectType.NIGHT_VISION);
            putEffect("HEALTH_BOOST", PotionEffectType.HEALTH_BOOST);
            putEffect("ABSORPTION", PotionEffectType.ABSORPTION);
            putEffect("SATURATION", PotionEffectType.SATURATION);
            putEffect("GLOWING", PotionEffectType.GLOWING);
            putEffect("LEVITATION", PotionEffectType.LEVITATION);
            putEffect("LUCK", PotionEffectType.LUCK);
            putEffect("UNLUCK", PotionEffectType.UNLUCK);
            putEffect("SLOW_FALLING", PotionEffectType.SLOW_FALLING);
            putEffect("CONDUIT_POWER", PotionEffectType.CONDUIT_POWER);
            putEffect("DOLPHINS_GRACE", PotionEffectType.DOLPHINS_GRACE);
            putEffect("BAD_OMEN", PotionEffectType.BAD_OMEN);
            putEffect("HERO_OF_THE_VILLAGE", PotionEffectType.HERO_OF_THE_VILLAGE);
            putEffect("DARKNESS", PotionEffectType.DARKNESS);
            putEffect("OOZING", PotionEffectType.OOZING);
            putEffect("WEAVING", PotionEffectType.WEAVING);
            putEffect("INFESTED", PotionEffectType.INFESTED);
        }
        return EFFECT_MAP;
    }

    private static void putEffect(String name, PotionEffectType type) {
        EFFECT_MAP.put(name, type);
    }

    /**
     * Парсит и выполняет действие.
     *
     * @param player Игрок
     * @param action Строка действия из конфига, например {@code "effect:SPEED:10:2"}
     * @return true если действие выполнено
     */
    /**
     * Разбирает строку действия на [тип, значение].
     * Тип нормализуется: нижний регистр, дефисы = подчёркивания
     * (damage-mobs == damage_mobs). null/пустая строка → null.
     */
    public static String[] splitAction(String action) {
        if (action == null || action.trim().isEmpty()) return null;
        String[] parts = action.split(":", 2);
        String actionType = parts[0].toLowerCase().replace('-', '_');
        String actionValue = parts.length > 1 ? parts[1] : "";
        return new String[]{actionType, actionValue};
    }

    public static boolean execute(Player player, String action) {
        if (action == null || action.isEmpty()) return false;

        String[] parsed = splitAction(action);
        String actionType = parsed[0];
        String actionValue = parsed[1];

        try {
            switch (actionType) {
                // === КОММУНИКАЦИЯ ===
                case "message":
                    executeMessage(player, actionValue);
                    return true;
                case "title":
                    executeTitle(player, actionValue);
                    return true;
                case "actionbar":
                    executeActionbar(player, actionValue);
                    return true;
                case "broadcast":
                case "announce":
                    executeBroadcast(actionValue);
                    return true;

                // === ЭФФЕКТЫ ===
                case "effect":
                    executeEffect(player, actionValue);
                    return true;
                case "heal":
                    executeHeal(player, actionValue);
                    return true;
                case "damage":
                    executeDamage(player, actionValue);
                    return true;
                case "damage_nearby":
                    executeDamageNearby(player, actionValue);
                    return true;
                case "damage_mobs":
                    executeDamageFiltered(player, actionValue, false, true);
                    return true;
                case "damage_players":
                    executeDamageFiltered(player, actionValue, true, false);
                    return true;
                case "heal_nearby":
                    executeHealNearby(player, actionValue);
                    return true;
                case "effect_nearby":
                    executeEffectNearby(player, actionValue, true, true);
                    return true;
                case "effect_nearby_mobs":
                    executeEffectNearby(player, actionValue, false, true);
                    return true;
                case "effect_nearby_players":
                    executeEffectNearby(player, actionValue, true, false);
                    return true;

                // === ТЕЛЕПОРТАЦИЯ ===
                case "teleport":
                    executeTeleport(player, actionValue);
                    return true;
                case "teleport_relative":
                    executeTeleportRelative(player, actionValue);
                    return true;

                // === ПРЕДМЕТЫ ===
                case "give":
                    executeGive(player, actionValue);
                    return true;
                case "remove":
                    executeRemove(player, actionValue);
                    return true;
                case "exp":
                    executeExp(player, actionValue);
                    return true;

                // === МИРОВЫЕ ===
                case "lightning":
                    executeLightning(player, actionValue);
                    return true;
                case "lightning_forward":
                    executeLightningForward(player, actionValue);
                    return true;
                case "particles":
                case "particle":
                    executeParticle(player, actionValue);
                    return true;
                case "sound":
                    executeSound(player, actionValue);
                    return true;
                case "fireworks":
                    executeFireworks(player, actionValue);
                    return true;
                case "break":
                    executeBreak(player, actionValue);
                    return true;

                // === СТАТУС ===
                case "sethealth":
                    executeSetHealth(player, actionValue);
                    return true;
                case "setfood":
                    executeSetFood(player, actionValue);
                    return true;
                case "set_xp":
                    executeSetXp(player, actionValue);
                    return true;
                case "vanish":
                    executeVanish(player, actionValue);
                    return true;
                case "glow":
                    executeGlow(player, actionValue);
                    return true;
                case "speed":
                    executeSpeed(player, actionValue);
                    return true;
                case "flight":
                    executeFlight(player, actionValue);
                    return true;
                case "knockback":
                    executeKnockback(player, actionValue, true, true);
                    return true;
                case "knockback_mobs":
                    executeKnockback(player, actionValue, false, true);
                    return true;
                case "knockback_players":
                    executeKnockback(player, actionValue, true, false);
                    return true;
                case "launch":
                    executeLaunch(player, actionValue, true, true);
                    return true;
                case "launch_mobs":
                    executeLaunch(player, actionValue, false, true);
                    return true;
                case "launch_players":
                    executeLaunch(player, actionValue, true, false);
                    return true;
                case "stun":
                    executeStun(player, actionValue, true, true);
                    return true;
                case "stun_mobs":
                    executeStun(player, actionValue, false, true);
                    return true;
                case "stun_players":
                    executeStun(player, actionValue, true, false);
                    return true;

                // === КОМАНДЫ ===
                case "command":
                    executeCommand(player, actionValue);
                    return true;
                case "console_command":
                    executeConsoleCommand(actionValue);
                    return true;

                // === ПРОДВИНУТЫЕ ===
                case "particles_custom":
                    executeParticleCustom(player, actionValue);
                    return true;
                case "sound_sequence":
                    executeSoundSequence(player, actionValue);
                    return true;
                case "title_sequence":
                    executeTitleSequence(player, actionValue);
                    return true;
                case "command_sequence":
                    executeCommandSequence(player, actionValue);
                    return true;
                case "teleport_sequence":
                    executeTeleportSequence(player, actionValue);
                    return true;
                case "effect_sequence":
                    executeEffectSequence(player, actionValue);
                    return true;
                case "damage_custom":
                    executeDamageCustom(player, actionValue);
                    return true;
                case "heal_custom":
                    executeHealCustom(player, actionValue);
                    return true;

                default:
                    return false;
            }
        } catch (Exception e) {
            Main plugin = Main.getInstance();
            if (plugin != null) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "Error executing action: " + action + " - " + e.getMessage(), e);
            }
            return false;
        }
    }

    // === КОММУНИКАЦИЯ ===

    private static void executeMessage(Player player, String value) {
        if (value.isEmpty()) return;
        value = value.replace("%player%", player.getName());
        player.sendMessage(ColorUtils.processMessage(player, value));
    }

    /**
     * Поддерживаются все форматы:
     *   title:Заголовок|Подзаголовок|fadeIn|stay|fadeOut   (рекомендуется)
     *   title:Заголовок|Подзаголовок:fadeIn:stay:fadeOut    (legacy: fade приклеен к подзаголовку)
     *   title:Заголовок:Подзаголовок:fadeIn:stay:fadeOut    (legacy: двоеточия)
     */
    private static void executeTitle(Player player, String value) {
        String[] parts = value.split("\\|");
        if (parts.length == 1) {
            parts = value.split(":");
        }

        String title = parts.length > 0 ? parts[0].replace("%player%", player.getName()) : "";
        String subtitle = parts.length > 1 ? parts[1].replace("%player%", player.getName()) : "";
        int fadeIn = 10;
        int stay = 40;
        int fadeOut = 10;

        if (parts.length >= 3) {
            // title:Заголовок|Подзаголовок|fadeIn|stay|fadeOut
            fadeIn = parseInt(parts[2], 10);
            stay = parts.length > 3 ? parseInt(parts[3], 40) : 40;
            fadeOut = parts.length > 4 ? parseInt(parts[4], 10) : 10;
        } else if (parts.length == 2 && subtitle.contains(":")) {
            // title:Заголовок|Подзаголовок:fadeIn:stay:fadeOut
            String[] subParts = subtitle.split(":");
            subtitle = subParts[0];
            if (subParts.length >= 2) fadeIn = parseInt(subParts[1], 10);
            if (subParts.length >= 3) stay = parseInt(subParts[2], 40);
            if (subParts.length >= 4) fadeOut = parseInt(subParts[3], 10);
        }

        player.sendTitle(
            ColorUtils.processMessage(player, title),
            ColorUtils.processMessage(player, subtitle),
            fadeIn, stay, fadeOut);
    }

    private static void executeActionbar(Player player, String value) {
        if (value.isEmpty()) return;
        value = value.replace("%player%", player.getName());
        try {
            player.spigot().sendMessage(
                net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(ColorUtils.processMessage(player, value))
            );
        } catch (Exception e) {
            player.sendMessage(ColorUtils.processMessage(player, value));
        }
    }

    private static void executeBroadcast(String value) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(ColorUtils.processMessage(online, value));
        }
    }

    // === ЭФФЕКТЫ ===

    /**
     * Формат: effect:ТИП:СЕКУНДЫ:УРОВЕНЬ[:ambient[:showParticles]]
     */
    private static void executeEffect(Player player, String value) {
        String[] parts = value.split(":");
        if (parts.length < 2) return;

        String effectName = parts[0].toUpperCase();
        int duration = parseInt(parts[1], 0);
        int amplifier = parts.length > 2 ? parseInt(parts[2], 1) - 1 : 0;
        boolean ambient = parts.length > 3 ? Boolean.parseBoolean(parts[3]) : true;
        boolean showParticles = parts.length > 4 ? Boolean.parseBoolean(parts[4]) : true;

        PotionEffectType effectType = resolveEffect(effectName);
        if (effectType != null) {
            player.addPotionEffect(new PotionEffect(effectType, duration * 20, amplifier, ambient, showParticles));
        }
    }

    /**
     * Исцеление. Формат: heal:КОЛИЧЕСТВО_HP (например heal:10 = +10 HP).
     */
    private static void executeHeal(Player player, String value) {
        double amount = value.isEmpty() ? 10.0 : parseDouble(value, 10.0);
        if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
            double max = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            player.setHealth(Math.min(player.getHealth() + amount, max));
        }
    }

    /**
     * Урон себе. Формат: damage:СУММА
     */
    private static void executeDamage(Player player, String value) {
        double amount = value.isEmpty() ? 5.0 : parseDouble(value, 5.0);
        player.damage(amount);
    }

    /**
     * AoE урон по всем живым рядом (включая игроков). Формат: damage_nearby:УРОН:РАДИУС
     */
    private static void executeDamageNearby(Player player, String value) {
        executeDamageFiltered(player, value, true, true);
    }

    /**
     * AoE урон с фильтром целей. Формат: УРОН:РАДИУС
     */
    private static void executeDamageFiltered(Player player, String value, boolean includePlayers, boolean includeMobs) {
        String[] parts = value.split(":");
        double amount = parts.length > 0 ? parseDouble(parts[0], 5.0) : 5.0;
        double range = parts.length > 1 ? parseDouble(parts[1], 3.0) : 3.0;

        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof LivingEntity) || entity == player) continue;
            if (entity instanceof Player && !includePlayers) continue;
            if (!(entity instanceof Player) && !includeMobs) continue;
            ((LivingEntity) entity).damage(amount, player);
        }
    }

    /**
     * Исцеление ближайших игроков. Формат: heal_nearby:КОЛ-ВО:РАДИУС
     */
    private static void executeHealNearby(Player player, String value) {
        String[] parts = value.split(":");
        double amount = parts.length > 0 ? parseDouble(parts[0], 10.0) : 10.0;
        double range = parts.length > 1 ? parseDouble(parts[1], 5.0) : 5.0;

        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof Player && entity != player) {
                Player target = (Player) entity;
                if (target.getAttribute(Attribute.MAX_HEALTH) != null) {
                    double max = target.getAttribute(Attribute.MAX_HEALTH).getValue();
                    target.setHealth(Math.min(target.getHealth() + amount, max));
                }
            }
        }
    }

    /**
     * Эффект по области. Формат: effect_nearby:ТИП:СЕКУНДЫ:УРОВЕНЬ[:РАДИУС]
     */
    private static void executeEffectNearby(Player player, String value, boolean includePlayers, boolean includeMobs) {
        String[] parts = value.split(":");
        if (parts.length < 3) return;

        PotionEffectType effectType = resolveEffect(parts[0].toUpperCase());
        if (effectType == null) return;

        int duration = parseInt(parts[1], 0) * 20;
        int amplifier = parseInt(parts[2], 1) - 1;
        double range = parts.length > 3 ? parseDouble(parts[3], 4.0) : 4.0;

        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof LivingEntity) || entity == player) continue;
            if (entity instanceof Player && !includePlayers) continue;
            if (!(entity instanceof Player) && !includeMobs) continue;
            ((LivingEntity) entity).addPotionEffect(new PotionEffect(effectType, duration, amplifier, false, false));
        }
    }

    // === ТЕЛЕПОРТАЦИЯ ===

    /**
     * Формат: teleport:X:Y:Z[:YAW]. Любая координата может начинаться с "~"
     * для перемещения относительно текущей позиции (например teleport:~:5:~).
     */
    private static void executeTeleport(Player player, String value) {
        String[] parts = value.split(":");
        if (parts.length < 3) return;

        Location loc = player.getLocation();
        double x = parseRelative(parts[0], loc.getX());
        double y = parseRelative(parts[1], loc.getY());
        double z = parseRelative(parts[2], loc.getZ());
        float yaw = parts.length > 3 ? (float) parseDouble(parts[3], loc.getYaw()) : loc.getYaw();
        player.teleport(new Location(loc.getWorld(), x, y, z, yaw, loc.getPitch()));
    }

    /**
     * Телепорт относительно текущей позиции. Формат: teleport_relative:X:Y:Z
     * (без "~" координаты складываются с текущими).
     */
    private static void executeTeleportRelative(Player player, String value) {
        String[] parts = value.split(":");
        Location loc = player.getLocation();
        double x = parts.length > 0 ? loc.getX() + parseRelative(parts[0], 0) : loc.getX();
        double y = parts.length > 1 ? loc.getY() + parseRelative(parts[1], 0) : loc.getY();
        double z = parts.length > 2 ? loc.getZ() + parseRelative(parts[2], 0) : loc.getZ();
        player.teleport(new Location(loc.getWorld(), x, y, z, loc.getYaw(), loc.getPitch()));
    }

    // === ПРЕДМЕТЫ ===

    /**
     * Выдать предмет. Формат: give:МАТЕРИАЛ:КОЛ  или  give:ID_КАСТОМНОГО_ПРЕДМЕТА:КОЛ
     */
    private static void executeGive(Player player, String value) {
        String[] parts = value.split(":");
        if (parts.length < 1) return;

        int amount = parts.length > 1 ? parseInt(parts[1], 1) : 1;
        amount = Math.max(1, Math.min(amount, 64));

        Material material = EnumCache.getMaterial(parts[0]);
        if (material != null) {
            player.getInventory().addItem(new ItemStack(material, amount));
            return;
        }

        // Кастомный предмет из реестра плагина
        Main plugin = Main.getInstance();
        if (plugin != null) {
            CustomItem customItem = plugin.getItemHandler().getCustomItem(parts[0].toLowerCase());
            if (customItem != null) {
                ItemStack stack = customItem.getItemStack().clone();
                stack.setAmount(amount);
                plugin.getItemHandler().updateItemWithUses(stack);
                player.getInventory().addItem(stack);
            }
        }
    }

    /**
     * Забрать предмет. Формат: remove:МАТЕРИАЛ:КОЛ
     */
    private static void executeRemove(Player player, String value) {
        String[] parts = value.split(":");
        if (parts.length < 1) return;

        Material material = EnumCache.getMaterial(parts[0]);
        if (material == null) return;

        int amount = parts.length > 1 ? parseInt(parts[1], 1) : 1;
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material && remaining > 0) {
                int toRemove = Math.min(item.getAmount(), remaining);
                item.setAmount(item.getAmount() - toRemove);
                remaining -= toRemove;
            }
        }
    }

    /**
     * Опыт. Формат: exp:ОПЫТ[:УРОВНИ]
     */
    private static void executeExp(Player player, String value) {
        String[] parts = value.split(":");
        int exp = parts.length > 0 ? parseInt(parts[0], 100) : 100;
        int levels = parts.length > 1 ? parseInt(parts[1], 0) : 0;
        player.giveExp(exp);
        if (levels > 0) {
            player.giveExpLevels(levels);
        }
    }

    // === МИРОВЫЕ ===

    private static void executeLightning(Player player, String value) {
        int strikes = value.isEmpty() ? 1 : parseInt(value, 1);
        for (int i = 0; i < strikes; i++) {
            player.getWorld().strikeLightning(player.getLocation());
        }
    }

    private static void executeLightningForward(Player player, String value) {
        int distance = value.isEmpty() ? 100 : parseInt(value, 100);
        Location eyeLoc = player.getEyeLocation();
        org.bukkit.util.Vector direction = eyeLoc.getDirection();
        Location targetLocation = eyeLoc.clone().add(direction.multiply(distance));
        player.getWorld().strikeLightning(targetLocation);
    }

    private static void executeParticle(Player player, String value) {
        String[] parts = value.split(":");
        if (parts.length < 1) return;

        Particle particle = EnumCache.getParticle(parts[0]);
        if (particle == null) return;
        int count = parts.length > 1 ? parseInt(parts[1], 10) : 10;
        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count);
    }

    private static void executeSound(Player player, String value) {
        String[] parts = value.split(":");
        if (parts.length < 1) return;

        Sound sound = EnumCache.getSound(parts[0]);
        if (sound == null) return;
        float volume = parts.length > 1 ? (float) parseDouble(parts[1], 1.0) : 1.0f;
        float pitch = parts.length > 2 ? (float) parseDouble(parts[2], 1.0) : 1.0f;
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private static void executeFireworks(Player player, String value) {
        try {
            org.bukkit.entity.Firework fw = player.getWorld().spawn(player.getLocation().add(0, 2, 0), org.bukkit.entity.Firework.class);
            org.bukkit.inventory.meta.FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(org.bukkit.FireworkEffect.builder()
                .with(org.bukkit.FireworkEffect.Type.BALL_LARGE)
                .withColor(org.bukkit.Color.RED, org.bukkit.Color.ORANGE)
                .withFade(org.bukkit.Color.YELLOW)
                .withFlicker()
                .withTrail()
                .build());
            meta.setPower(1);
            fw.setFireworkMeta(meta);
        } catch (Exception e) {
            Main plugin = Main.getInstance();
            if (plugin != null) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "Error spawning firework: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Ломает блок, на который смотрит игрок. Формат: break[:МАТЕРИАЛ]
     */
    private static void executeBreak(Player player, String value) {
        Block block = player.getTargetBlockExact(5);
        if (block == null || block.getType() == Material.AIR) return;

        String[] parts = value.split(":");
        if (parts.length >= 1 && !parts[0].isEmpty()) {
            Material wanted = EnumCache.getMaterial(parts[0]);
            if (wanted != null && block.getType() != wanted) return;
        }
        block.breakNaturally();
    }

    // === СТАТУС ===

    private static void executeSetHealth(Player player, String value) {
        double health = parseDouble(value, 20.0);
        if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
            double max = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            player.setHealth(Math.min(health, max));
        }
    }

    private static void executeSetFood(Player player, String value) {
        int food = parseInt(value, 20);
        player.setFoodLevel(Math.min(food, 20));
    }

    /**
     * Установить уровень. Формат: set_xp:УРОВЕНЬ
     */
    private static void executeSetXp(Player player, String value) {
        int level = Math.max(0, parseInt(value, 0));
        player.setLevel(level);
    }

    private static void executeVanish(Player player, String value) {
        int duration = value.isEmpty() ? 30 : parseInt(value, 30);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration * 20, 0, false, false));
    }

    private static void executeGlow(Player player, String value) {
        int duration = value.isEmpty() ? 10 : parseInt(value, 10);
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration * 20, 0, false, false));
    }

    private static void executeSpeed(Player player, String value) {
        String[] parts = value.split(":");
        int duration = parts.length > 0 ? parseInt(parts[0], 30) : 30;
        int level = parts.length > 1 ? parseInt(parts[1], 1) : 1;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration * 20, level - 1, false, false));
    }

    private static void executeFlight(Player player, String value) {
        boolean enable = value.isEmpty() || Boolean.parseBoolean(value);
        player.setAllowFlight(enable);
        player.setFlying(enable);
    }

    /**
     * Отброс. Формат: knockback[:РАДИУС]
     */
    private static void executeKnockback(Player player, String value, boolean includePlayers, boolean includeMobs) {
        double range = value.isEmpty() ? 3.0 : parseDouble(value, 3.0);

        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof LivingEntity) || entity == player) continue;
            if (entity instanceof Player && !includePlayers) continue;
            if (!(entity instanceof Player) && !includeMobs) continue;

            Location loc = entity.getLocation();
            Location playerLoc = player.getLocation();
            double dx = loc.getX() - playerLoc.getX();
            double dz = loc.getZ() - playerLoc.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 0) {
                double strength = 1.5 / dist;
                entity.setVelocity(entity.getVelocity().add(new org.bukkit.util.Vector(dx * strength, 0.5, dz * strength)));
            }
        }
    }

    /**
     * Подброс вверх. Формат: launch[:СИЛА]
     */
    private static void executeLaunch(Player player, String value, boolean includePlayers, boolean includeMobs) {
        double power = value.isEmpty() ? 1.5 : parseDouble(value, 1.5);

        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(4, 4, 4)) {
            if (!(entity instanceof LivingEntity) || entity == player) continue;
            if (entity instanceof Player && !includePlayers) continue;
            if (!(entity instanceof Player) && !includeMobs) continue;
            entity.setVelocity(entity.getVelocity().add(new org.bukkit.util.Vector(0, power, 0)));
        }
    }

    /**
     * Оглушение (Slowness 255 + Mining Fatigue 255). Формат: stun:СЕКУНДЫ[:РАДИУС]
     */
    private static void executeStun(Player player, String value, boolean includePlayers, boolean includeMobs) {
        String[] parts = value.split(":");
        int duration = parts.length > 0 ? parseInt(parts[0], 3) : 3;
        double range = parts.length > 1 ? parseDouble(parts[1], 4.0) : 4.0;

        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof LivingEntity) || entity == player) continue;
            if (entity instanceof Player && !includePlayers) continue;
            if (!(entity instanceof Player) && !includeMobs) continue;

            ((LivingEntity) entity).addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, duration * 20, 127, false, false));
            ((LivingEntity) entity).addPotionEffect(new PotionEffect(
                PotionEffectType.MINING_FATIGUE, duration * 20, 127, false, false));
        }
    }

    // === КОМАНДЫ ===

    /**
     * Выполнить команду от имени консоли (без прав игрока).
     * Формат: command:КОМАНДА (поддерживается %player%)
     */
    private static void executeCommand(Player player, String value) {
        String command = value.replace("%player%", player.getName());
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
    }

    private static void executeConsoleCommand(String value) {
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), value);
    }

    // === ПРОДВИНУТЫЕ ДЕЙСТВИЯ ===

    /**
     * Кастомные частицы с полным контролем.
     * Формат: particles_custom:ТИП:КОЛИЧЕСТВО:X:Y:Z:OFF_X:OFF_Y:OFF_Z
     */
    private static void executeParticleCustom(Player player, String value) {
        String[] parts = value.split(":");
        if (parts.length < 2) return;

        Particle particle = EnumCache.getParticle(parts[0]);
        if (particle == null) return;
        int count = parseInt(parts[1], 10);
        double x = parts.length > 2 ? parseDouble(parts[2], 0) : 0;
        double y = parts.length > 3 ? parseDouble(parts[3], 1) : 1;
        double z = parts.length > 4 ? parseDouble(parts[4], 0) : 0;
        double offX = parts.length > 5 ? parseDouble(parts[5], 0.5) : 0.5;
        double offY = parts.length > 6 ? parseDouble(parts[6], 0.5) : 0.5;
        double offZ = parts.length > 7 ? parseDouble(parts[7], 0.5) : 0.5;

        Location loc = player.getLocation().add(x, y, z);
        player.getWorld().spawnParticle(particle, loc, count, offX, offY, offZ);
    }

    /**
     * Последовательность звуков с задержками.
     * Формат: sound_sequence:ЗВУК1:ГРОМКОСТЬ1:ТОН1;ЗВУК2:ГРОМКОСТЬ2:ТОН2;...
     */
    private static void executeSoundSequence(Player player, String value) {
        String[] sounds = value.split(";");
        for (String soundStr : sounds) {
            String[] parts = soundStr.split(":");
            if (parts.length < 1) continue;

            Sound sound = EnumCache.getSound(parts[0]);
            if (sound == null) continue;
            float volume = parts.length > 1 ? (float) parseDouble(parts[1], 1.0) : 1.0f;
            float pitch = parts.length > 2 ? (float) parseDouble(parts[2], 1.0) : 1.0f;

            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    /**
     * Последовательность заголовков.
     * Формат: title_sequence:ЗАГОЛОВОК:ПОДЗАГОЛОВОК:FADEIN:STAY:FADEOUT;...
     */
    private static void executeTitleSequence(Player player, String value) {
        String[] titles = value.split(";");
        for (String titleStr : titles) {
            String[] parts = titleStr.split(":");
            if (parts.length < 1) continue;

            String title = parts.length > 0 ? ColorUtils.processMessage(player, parts[0]) : "";
            String subtitle = parts.length > 1 ? ColorUtils.processMessage(player, parts[1]) : "";
            int fadeIn = parts.length > 2 ? parseInt(parts[2], 10) : 10;
            int stay = parts.length > 3 ? parseInt(parts[3], 40) : 40;
            int fadeOut = parts.length > 4 ? parseInt(parts[4], 10) : 10;

            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    /**
     * Последовательность команд с задержками.
     * Формат: command_sequence:КОМАНДА1:ЗАДЕРЖКА1;КОМАНДА2:ЗАДЕРЖКА2;...
     */
    private static void executeCommandSequence(Player player, String value) {
        String[] commands = value.split(";");
        long delay = 0;
        for (String cmdStr : commands) {
            String[] parts = cmdStr.split(":");
            if (parts.length < 1) continue;

            String command = parts[0].replace("%player%", player.getName());
            long cmdDelay = parts.length > 1 ? Long.parseLong(parts[1]) : 0;

            final long finalDelay = delay;
            scheduleLater(() ->
                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command),
                finalDelay);
            delay += cmdDelay;
        }
    }

    /**
     * Последовательность телепортаций.
     * Формат: teleport_sequence:X1:Y1:Z1:ЗАДЕРЖКА1;X2:Y2:Z2:ЗАДЕРЖКА2;...
     */
    private static void executeTeleportSequence(Player player, String value) {
        String[] points = value.split(";");
        long delay = 0;
        for (String pointStr : points) {
            String[] parts = pointStr.split(":");
            if (parts.length < 3) continue;

            double x = parseDouble(parts[0], 0);
            double y = parseDouble(parts[1], 0);
            double z = parseDouble(parts[2], 0);
            long tpDelay = parts.length > 3 ? Long.parseLong(parts[3]) : 0;

            final long finalDelay = delay;
            final Location loc = new Location(player.getWorld(), x, y, z);
            scheduleLater(() -> player.teleport(loc), finalDelay);
            delay += tpDelay;
        }
    }

    /**
     * Последовательность эффектов.
     * Формат: effect_sequence:ТИП1:ДЛИТЕЛЬНОСТЬ1:УРОВЕНЬ1:ЗАДЕРЖКА1;...
     */
    private static void executeEffectSequence(Player player, String value) {
        String[] effects = value.split(";");
        long delay = 0;
        for (String effectStr : effects) {
            String[] parts = effectStr.split(":");
            if (parts.length < 2) continue;

            PotionEffectType effectType = resolveEffect(parts[0].toUpperCase());
            int duration = parseInt(parts[1], 0);
            int level = parts.length > 2 ? parseInt(parts[2], 1) - 1 : 0;
            long effectDelay = parts.length > 3 ? Long.parseLong(parts[3]) : 0;

            if (effectType != null) {
                final PotionEffectType finalType = effectType;
                final int finalLevel = level;
                final long finalDelay = delay;
                scheduleLater(() ->
                    player.addPotionEffect(new PotionEffect(finalType, duration * 20, finalLevel, false, false)),
                    finalDelay);
            }

            delay += effectDelay;
        }
    }

    /**
     * Кастомный урон с параметрами.
     * Формат: damage_custom:УРОН:ШИРИНА:ВЫСОТА:ГЛУБИНА
     */
    private static void executeDamageCustom(Player player, String value) {
        String[] parts = value.split(":");
        if (parts.length < 1) return;

        double amount = parseDouble(parts[0], 5.0);
        double width = parts.length > 1 ? parseDouble(parts[1], 3.0) : 3.0;
        double height = parts.length > 2 ? parseDouble(parts[2], 3.0) : 3.0;
        double depth = parts.length > 3 ? parseDouble(parts[3], 3.0) : 3.0;

        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(width, height, depth)) {
            if (entity instanceof LivingEntity && entity != player) {
                ((LivingEntity) entity).damage(amount, player);
            }
        }
    }

    /**
     * Кастомное исцеление с параметрами.
     * Формат: heal_custom:ИСЦЕЛЕНИЕ:ШИРИНА:ВЫСОТА:ГЛУБИНА[:ТИП_ЧАСТИЦ]
     */
    private static void executeHealCustom(Player player, String value) {
        String[] parts = value.split(":");
        if (parts.length < 1) return;

        double amount = parseDouble(parts[0], 10.0);
        double width = parts.length > 1 ? parseDouble(parts[1], 5.0) : 5.0;
        double height = parts.length > 2 ? parseDouble(parts[2], 5.0) : 5.0;
        double depth = parts.length > 3 ? parseDouble(parts[3], 5.0) : 5.0;
        String particleType = parts.length > 4 ? parts[4] : "HEART";

        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(width, height, depth)) {
            if (entity instanceof Player && entity != player) {
                Player target = (Player) entity;
                if (target.getAttribute(Attribute.MAX_HEALTH) != null) {
                    double max = target.getAttribute(Attribute.MAX_HEALTH).getValue();
                    target.setHealth(Math.min(target.getHealth() + amount, max));
                }
                try {
                    Particle particle = EnumCache.getParticle(particleType);
                    if (particle != null) {
                        target.getWorld().spawnParticle(particle, target.getLocation().add(0, 2, 0), 20);
                    }
                } catch (Exception exception) {
                    Main plugin = Main.getInstance();
                    if (plugin != null) {
                        plugin.getLogger().log(java.util.logging.Level.WARNING,
                            "Error spawning custom healing particle " + particleType, exception);
                    }
                }
            }
        }
    }

    // === ХЕЛПЕРЫ ===

    private static PotionEffectType resolveEffect(String name) {
        PotionEffectType effectType = effectMap().get(name);
        if (effectType == null) {
            effectType = EnumCache.getPotionEffect(name);
        }
        return effectType;
    }

    /**
     * Координата: "5" = 5, "~" = текущая, "~5" = текущая + 5.
     */
    private static double parseRelative(String raw, double current) {
        if (raw.startsWith("~")) {
            String offset = raw.substring(1);
            if (offset.isEmpty()) return current;
            return current + parseDouble(offset, 0);
        }
        return parseDouble(raw, current);
    }

    private static int parseInt(String raw, int def) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static double parseDouble(String raw, double def) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static void scheduleLater(Runnable task, long delayMs) {
        Main plugin = Main.getInstance();
        if (plugin == null) {
            task.run();
            return;
        }
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, task, Math.max(1L, delayMs / 50));
    }
}