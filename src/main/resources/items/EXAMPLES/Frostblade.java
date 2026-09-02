import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.api.ItemAPI;
import me.dcplugin.dcustomitems.api.RecipeDef;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;

/**
 * ❄ Frostblade — полный справочный пример Java-ОРУЖИЯ (клики + кулдаун).
 *
 * Файл лежит в items/EXAMPLES/ — папка НЕ копируется на сервер и НЕ
 * компилируется рантаймом (см. DefaultContentExtractor / ItemRegistry).
 * Чтобы включить: скопируйте в plugins/DC-CustomItems/items/ и /ci reload,
 * затем /api-item give frostblade.
 *
 * Что показывает пример:
 *   1. onRightClick / onLeftClick — кликовые хуки (вызывает ApiEventListener,
 *      только пока предмет в руке; ПКМ/ЛКМ по блоку/воздуху).
 *   2. getClickCooldown() — кулдаун между кликами в миллисекундах.
 *      ⚠️ В текущей реализации это ОДИН таймер на игрока: оба клика (ЛКМ и
 *      ПКМ) делят его. Не ставьте разные кулдауны на левую и правую кнопку.
 *   3. onDamageDealt — хук при ударе по существу (ЛКМ по мобу идёт сюда,
 *      а НЕ в onLeftClick).
 *   4. onKill — убийство игрока (хук получает только Player-жертв).
 *   5. getRecipes() — крафт в верстаке и в GUI /craft.
 *
 * getPermission() проверяется при кликах (ЛКМ/ПКМ), как у YAML-предметов:
 * без права клик отменяется и предмет не срабатывает.
 */
public class Frostblade extends AbstractCustomItem {

    // ===== ИДЕНТИФИКАЦИЯ =====

    @Override
    public String getId() {
        return "frostblade";
    }

    @Override
    public String getDisplayName() {
        return "&b❄ Ледяной Клинок";
    }

    @Override
    public Material getMaterial() {
        return Material.NETHERITE_SWORD;
    }

    @Override
    public List<String> getLore() {
        return List.of(
            "",
            " &7Клинок из вечной мерзлоты.",
            "",
            " &bЛКМ: &fЛедяной рывок вперёд",
            " &bПКМ: &fЛедяной взрыв (заморозка + урон)",
            " &bУдары: &fзамедляют цель и добавляют урон",
            "",
            " &8Кулдаун: 1.5 сек"
        );
    }

    @Override
    public String getItemModel() {
        // Ресурс-пак: assets/minecraft/models/item/frostblade.json
        return "frostblade";
    }

    @Override
    public boolean isUnbreakable() {
        return true;
    }

    @Override
    public boolean isGlowing() {
        return true;
    }

    /**
     * Общий кулдаун кликов (ЛКМ и ПКМ) в миллисекундах.
     * Пока кулдаун активен, клик игнорируется полностью (и событие
     * CustomItemUseEvent не стреляет).
     */
    @Override
    public long getClickCooldown() {
        return 1500;
    }

    // ===== КРАФТ =====

    @Override
    public List<RecipeDef> getRecipes() {
        Map<Character, String> keys = Map.of(
            'N', "NETHERITE_INGOT",
            'B', "BLUE_ICE",
            'S', "STICK"
        );
        return List.of(RecipeDef.shaped(List.of(
            " N ",
            "NSN",
            "B B"
        ), keys));
    }

    // ===== КЛИКИ =====

    /**
     * ПКМ — Ледяной взрыв: AoE вокруг игрока.
     * Замедляет и отбрасывает живых существ, наносит урон (4 HP).
     */
    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        double radius = 5.0;

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity) || entity.equals(player)) continue;
            LivingEntity target = (LivingEntity) entity;

            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 4 * 20, 1, false, true));
            target.damage(4.0, player);

            // Лёгкий отброс от игрока
            org.bukkit.util.Vector away = target.getLocation().toVector()
                    .subtract(player.getLocation().toVector()).normalize();
            target.setVelocity(away.multiply(0.9).setY(0.4));
        }

        ItemAPI.particlesCustom(player, Particle.SNOWFLAKE, 120, 2, 2, 2);
        ItemAPI.sound(player, Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.6f);
        ItemAPI.sound(player, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.8f);
        ItemAPI.title(player, "&b❄ ЛЕДЯНОЙ ВЗРЫВ", "&fВсё вокруг замерзает");
    }

    /**
     * ЛКМ — Ледяной рывок: рывок вперёд + ледяной след.
     * (Атака по мобам идёт в onDamageDealt, не сюда.)
     */
    @Override
    public void onLeftClick(PlayerInteractEvent event, Player player) {
        org.bukkit.util.Vector direction = player.getLocation().getDirection();
        player.setVelocity(direction.multiply(1.6).setY(0.15));

        ItemAPI.particles(player, Particle.SNOWFLAKE, 60);
        ItemAPI.sound(player, Sound.ENTITY_SNOWBALL_THROW, 1f, 1.4f);
    }

    // ===== БОЙ =====

    /**
     * Удар по цели: замораживает (Slowness III на 3 сек) и добавляет урон.
     */
    @Override
    public void onDamageDealt(EntityDamageByEntityEvent event, Player player) {
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getEntity();
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 3 * 20, 2, false, true));
            event.setDamage(event.getDamage() + 3.0);
        }

        ItemAPI.particlesAt(event.getEntity().getLocation(), Particle.ITEM_SNOWBALL, 15);
        ItemAPI.sound(player, Sound.BLOCK_GLASS_BREAK, 0.5f, 2f);
    }

    /**
     * Убийство игрока: «осколки» + немного поглощения победителю.
     * Внимание: onKill вызывается только при убийстве ИГРОКА (PlayerDeathEvent).
     */
    @Override
    public void onKill(Player killer, Player victim) {
        ItemAPI.particlesAt(victim.getLocation(), Particle.SNOWFLAKE, 200);
        ItemAPI.sound(killer, Sound.BLOCK_GLASS_BREAK, 1f, 0.8f);
        ItemAPI.effect(killer, PotionEffectType.ABSORPTION, 30, 2);
        ItemAPI.message(killer, "&b❄ Жертва рассыпалась во льду!");
    }
}
