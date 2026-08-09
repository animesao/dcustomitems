import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * Пример бесконечного зелья на Java API.
 * 
 * Расходуемый предмет, дающий мощные эффекты.
 * Использование: ПКМ с предметом в руке.
 */
public class InfinityPotion extends AbstractCustomItem {

    @Override
    public String getId() { return "infinity_potion"; }

    @Override
    public String getDisplayName() { return "&b✨ Зелье Бесконечности"; }

    @Override
    public Material getMaterial() { return Material.EXPERIENCE_BOTTLE; }

    @Override
    public java.util.List<String> getLore() {
        return java.util.List.of(
            "",
            " &7Зелье, созданное из звёздной пыли",
            "",
            " &bДействие:",
            " &f• Скорость III на 30 сек",
            " &f• Сила II на 30 сек",
            " &f• Сопротивление II на 30 сек",
            " &f• Полное исцеление",
            "",
            " &c⚠ Одноразовое!"
        );
    }

    @Override
    public String getItemModel() { return "infinity_potion"; }

    @Override
    public int getMaxUses() { return 1; }

    @Override
    public void onRightClick(org.bukkit.event.player.PlayerInteractEvent event, Player player) {
        // Применяем мощные эффекты
        ItemAPI.effect(player, PotionEffectType.SPEED, 30, 3);        // Скорость III
        ItemAPI.effect(player, PotionEffectType.STRENGTH, 30, 2);     // Сила II
        ItemAPI.effect(player, PotionEffectType.RESISTANCE, 30, 2);   // Сопротивление II
        ItemAPI.effect(player, PotionEffectType.REGENERATION, 10, 2); // Регенерация II
        
        // Полное исцеление
        ItemAPI.heal(player, 20);
        
        // Эффекты
        ItemAPI.particles(player, Particle.TOTEM_OF_UNDYING, 100);
        ItemAPI.sound(player, Sound.ENTITY_PLAYER_LEVELUP, 2f, 0.5f);
        ItemAPI.title(player, "&b✨ БЕСКОНЕЧНОСТЬ!", "&7Все силы пробуждены!");
        
        // Забираем предмет
        event.getItem().setAmount(event.getItem().getAmount() - 1);
    }
}
