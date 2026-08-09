import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.api.ItemAPI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Arrow;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.List;

public class ExplosiveBow extends AbstractCustomItem {

    @Override
    public String getId() { return "explosive_bow"; }

    @Override
    public String getDisplayName() { return "&c🏹 &fВзрывной Лук"; }

    @Override
    public Material getMaterial() { return Material.BOW; }

    @Override
    public List<String> getLore() {
        return List.of(
            "",
            " &7Стреляет взрывными стрелами",
            " &7Каждая стрела вызывает взрыв",
            "",
            " &c✦ &fУрон: x2",
            " &e✦ &fВзрыв: 3 блока",
            "",
            " &7ПКМ - зарядить мощность"
        );
    }

    @Override
    public String getItemModel() { return "explosive_bow"; }

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public boolean isGlowing() { return true; }

    @Override
    public void onBowShoot(EntityShootBowEvent event, Player player, Arrow arrow) {
        // Пометить стрелу как взрывную
        arrow.setMetadata("explosive", new FixedMetadataValue(getPlugin(), true));
        arrow.setMetadata("power", new FixedMetadataValue(getPlugin(), 2));

        // Эффекты
        ItemAPI.particles(player, Particle.FLAME, 30);
        ItemAPI.sound(player, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);
        ItemAPI.message(player, "&c🏹 &7Взрывная стрела выпущена!");
    }

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        // Зарядить мощность
        ItemAPI.effect(player, org.bukkit.potion.PotionEffectType.STRENGTH, 10, 1);
        ItemAPI.particles(player, Particle.CRIT, 50);
        ItemAPI.sound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        ItemAPI.title(player, "&c🔥 &fМОЩНОСТЬ!", "&7Стрелы станут сильнее!");
    }

    private me.dcplugin.dcustomitems.Main getPlugin() {
        return me.dcplugin.dcustomitems.Main.getInstance();
    }
}
