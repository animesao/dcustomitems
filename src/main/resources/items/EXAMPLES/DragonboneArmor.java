import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.api.ItemAPI;
import me.dcplugin.dcustomitems.api.RecipeDef;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;

/**
 * 🐉 Dragonbone Armor — полный справочный пример Java-предмета.
 *
 * Файл лежит в items/EXAMPLES/ — этой папке НЕ загружается и НЕ копируется
 * на сервер (см. DefaultContentExtractor и ItemRegistry). Чтобы включить
 * предмет: скопируйте файл в plugins/DC-CustomItems/items/ (или в любую
 * другую подпапку) и выполните /ci reload.
 *
 * Что показывает пример:
 *   1. getRecipes()            — крафт-рецепты (shaped + shapeless). Попадают
 *                                в обычный верстак И в GUI /craft. Ингредиент
 *                                может быть материалом или ID кастомного
 *                                предмета — YAML или Java.
 *   2. getActivationSlot/тип   — броня: работает только в слоте CHEST.
 *   3. onEquip/onUnequip       — хуки экипировки (вызывает глобальный чекер;
 *                                перед ними стреляет CustomItemEquipEvent).
 *   4. getPeriodicInterval+onPeriodic — пассивный эффект, пока предмет надет.
 *   5. onDamageTaken           — боевой хук (защита + частицы).
 *
 * Идентичность предмета — PDC-ключ dcustomitems:custom_item_id (ItemAPI),
 * поэтому рецепты/события видят его одинаково с YAML-предметами.
 */
public class DragonboneArmor extends AbstractCustomItem {

    // ===== ИДЕНТИФИКАЦИЯ =====

    @Override
    public String getId() {
        return "dragonbone_armor";
    }

    @Override
    public String getDisplayName() {
        return "&6🐉 Нагрудник из костей дракона";
    }

    @Override
    public Material getMaterial() {
        return Material.NETHERITE_CHESTPLATE;
    }

    @Override
    public List<String> getLore() {
        return List.of(
            "",
            " &7Доспех, выкованный из костей",
            " &7древнего дракона.",
            "",
            " &6Пассивно: &fСила II + Регенерация I",
            " &6При ударе: &f-25% урона",
            "",
            " &8Крафт: 4x NETHERITE_INGOT, 4x vampire-blade, 1x dragon_breath",
            ""
        );
    }

    @Override
    public String getItemModel() {
        return "dragonbone_armor";
    }

    @Override
    public boolean isUnbreakable() {
        return true;
    }

    @Override
    public boolean isGlowing() {
        return true;
    }

    // ===== СЛОТ И ТИП =====

    /** Предмет активен только когда надет в слот нагрудника. */
    @Override
    public String getActivationSlot() {
        return "CHEST";
    }

    @Override
    public String getType() {
        return "ARMOR";
    }

    // ===== КРАФТ-РЕЦЕПТЫ =====

    /**
     * Shaped: сетка 3x3, пробел = пустая ячейка.
     * 'B' ссылается на ДРУГОЙ кастомный предмет по его ID (vampire-blade —
     * штатный YAML-предмет; можно ссылаться и на Java-предмет, и на сам себя).
     */
    @Override
    public List<RecipeDef> getRecipes() {
        Map<Character, String> keys = Map.of(
            'N', "NETHERITE_INGOT", // материал
            'B', "vampire-blade",   // YAML-предмет как ингредиент
            'D', "dragon_breath"    // ванильный материал (дроп дракона)
        );
        return List.of(
            // 3 строки одинаковой длины (максимум 3x3)
            RecipeDef.shaped(List.of(
                "NBN",
                "NDN",
                "N N"
            ), keys),
            // Вариант без формы: 6 алмазов + сам предмет (починка/апгрейд)
            RecipeDef.shapeless(List.of(
                "DIAMOND", "DIAMOND", "DIAMOND",
                "DIAMOND", "DIAMOND", "DIAMOND",
                "dragonbone_armor"
            ))
        );
    }

    // ===== ЭКИПИРОВКА =====

    /** Вызывается при надевании (после CustomItemEquipEvent). */
    @Override
    public void onEquip(Player player) {
        ItemAPI.effect(player, PotionEffectType.STRENGTH, 999, 2);   // Сила II
        ItemAPI.effect(player, PotionEffectType.REGENERATION, 999, 1);
        ItemAPI.sound(player, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 0.9f);
        ItemAPI.particles(player, Particle.DRAGON_BREATH, 30);
        ItemAPI.message(player, "&6🐉 Сила древнего дракона течёт в твоих жилах!");
    }

    /** Вызывается при снятии (после CustomItemEquipEvent). */
    @Override
    public void onUnequip(Player player) {
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.REGENERATION);
        ItemAPI.sound(player, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 0.5f);
        ItemAPI.message(player, "&7Сила дракона покинула тебя.");
    }

    /**
     * Пассивный эффект, пока нагрудник надет: вызывается глобальным чекером
     * раз в getPeriodicInterval() тиков. Здесь эффекты обновляются, чтобы их
     * не мог «съесть» другой источник.
     */
    @Override
    public long getPeriodicInterval() {
        return 60; // каждые 3 секунды
    }

    @Override
    public void onPeriodic(Player player) {
        ItemAPI.effect(player, PotionEffectType.STRENGTH, 6, 2);
        ItemAPI.effect(player, PotionEffectType.REGENERATION, 6, 1);
        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                ? player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()
                : 20.0;
        if (player.getHealth() < maxHealth) {
            ItemAPI.particles(player, Particle.DRAGON_BREATH, 3);
        }
    }

    // ===== БОЙ =====

    /** Защита: -25% входящего урона, пока нагрудник надет. */
    @Override
    public void onDamageTaken(EntityDamageEvent event, Player player) {
        event.setDamage(event.getDamage() * 0.75);
        ItemAPI.particlesAt(player.getLocation(), Particle.DRAGON_BREATH, 10);
        ItemAPI.sound(player, Sound.BLOCK_ANVIL_PLACE, 0.4f, 1.6f);
    }

    // ===== ПРОЧЕЕ =====

    /** Кулдаун кликов (если добавите onRightClick/onLeftClick). */
    @Override
    public long getClickCooldown() {
        return 1500;
    }

    /** Право на использование (null = без ограничений). */
    @Override
    public String getPermission() {
        return null;
    }
}
