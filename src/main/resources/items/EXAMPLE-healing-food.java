import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class HealingFood extends AbstractCustomItem {

    @Override
    public String getId() { return "healing_food"; }

    @Override
    public String getDisplayName() { return "&a🍎 &fВолшебное Яблоко"; }

    @Override
    public Material getMaterial() { return Material.GOLDEN_APPLE; }

    @Override
    public List<String> getLore() {
        return List.of(
            "",
            " &7Восстанавливает здоровье и",
            " &7даёт мощные эффекты",
            "",
            " &7ПКМ - съесть",
            "",
            " &a✦ &fИсцеление: +8 сердец",
            " &b✦ &fСкорость II: 30 сек",
            " &e✦ &fСопротивление I: 30 сек"
        );
    }

    @Override
    public String getItemModel() { return "healing_food"; }

    @Override
    public boolean hasCooldown() { return true; }

    @Override
    public int getCooldownSeconds() { return 30; }

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        // Исцеление
        ItemAPI.heal(player, 16); // +8 сердец

        // Эффекты
        ItemAPI.effect(player, PotionEffectType.SPEED, 30, 1);
        ItemAPI.effect(player, PotionEffectType.DAMAGE_RESISTANCE, 30, 0);

        // Эффекты
        ItemAPI.particles(player, Particle.HEART, 30);
        ItemAPI.sound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);

        // Заголовок
        ItemAPI.title(player, "&a🍎 &fВолшебное Яблоко", "&7Здоровье восстановлено!");

        // Убрать предмет из руки
        player.getInventory().getItemInMainHand().setAmount(0);
    }
}
