import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * 🚀 ОПТИМИЗИРОВАННЫЙ ПРЕДМЕТ
 * 
 * Этот файл показывает как создавать производительные предметы.
 * 
 * Правила оптимизации:
 * 1. Используй кулдауны (getClickCooldown)
 * 2. Ограничивай частицы (меньше 100)
 * 3. Не делай тяжёлых операций в onRightClick
 * 4. Кэшируй данные
 */
public class optimization extends AbstractCustomItem {

    @Override
    public String getId() { return "optimization_sword"; }

    @Override
    public String getDisplayName() { return "&a&l⚡ Оптимизированный Меч"; }

    @Override
    public Material getMaterial() { return Material.IRON_SWORD; }

    @Override
    public java.util.List<String> getLore() {
        return java.util.List.of(
            "",
            " &7Этот предмет оптимизирован",
            " &7для максимальной производительности!",
            "",
            " &7ПКМ: Исцеление (кулдаун 3 сек)",
            " &7ЛКМ: Урон (кулдаун 1 сек)",
            ""
        );
    }

    @Override
    public String getItemModel() { return "optimization_sword"; }

    @Override
    public long getClickCooldown() { return 3000; } // 3 секунды кулдаун!

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        // ✅ ОПТИМИЗИРОВАННО:
        // - Мало частиц (20 вместо 1000)
        // - Один звук
        // - Быстрое лечение
        ItemAPI.heal(player, 8);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 2, 0), 20);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
    }

    @Override
    public void onDamageDealt(org.bukkit.event.entity.EntityDamageByEntityEvent event, Player player) {
        // ✅ ОПТИМИЗИРОВАННО:
        // - Мало частиц
        // - Простая логика
        event.setDamage(event.getDamage() * 1.5);
        event.getEntity().getWorld().spawnParticle(Particle.CRIT, event.getEntity().getLocation().add(0, 1, 0), 15);
    }
}
