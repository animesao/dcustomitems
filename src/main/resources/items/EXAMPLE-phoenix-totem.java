import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * Пример талисмана Феникса на Java API.
 * 
 * При смерти воскрешает игрока с эффектами.
 * Использование: автоматически при смерти.
 */
public class PhoenixTotem extends AbstractCustomItem {

    @Override
    public String getId() { return "phoenix_totem"; }

    @Override
    public String getDisplayName() { return "&c🔥 Талисман Феникса"; }

    @Override
    public Material getMaterial() { return Material.TOTEM_OF_UNDYING; }

    @Override
    public java.util.List<String> getLore() {
        return java.util.List.of(
            "",
            " &7Древний талисман, созданный из",
            " &7перьев огненной птицы",
            "",
            " &cДействие:",
            " &f• Воскрешение при смерти",
            " &f• Полное исцеление",
            " &f• Сила + Скорость на 30 сек",
            "",
            " &7Перезарядка: 5 минут"
        );
    }

    @Override
    public String getItemModel() { return "phoenix_totem"; }

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public boolean isGlowing() { return true; }

    @Override
    public String getActivationSlot() { return "OFFHAND"; }

    @Override
    public long getClickCooldown() { return 300000; } // 5 минут

    @Override
    public void onDeath(Player player, org.bukkit.event.entity.PlayerDeathEvent event) {
        // Отменяем смерть и воскрешаем
        event.setCancelled(true);
        
        // Воскрешаем с полным здоровьем
        player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        
        // Даём мощные эффекты
        ItemAPI.effect(player, PotionEffectType.STRENGTH, 30, 2);
        ItemAPI.effect(player, PotionEffectType.SPEED, 30, 2);
        ItemAPI.effect(player, PotionEffectType.FIRE_RESISTANCE, 60, 1);
        
        // Эффекты воскрешения
        ItemAPI.particles(player, Particle.FLAME, 200);
        ItemAPI.particles(player, Particle.TOTEM_OF_UNDYING, 100);
        ItemAPI.sound(player, Sound.ITEM_TOTEM_USE, 2f, 0.5f);
        ItemAPI.title(player, "&c🔥 ФЕНИКС!", "&7Вы воскресли из пепла!");
        
        // Забираем талисман
        player.getInventory().removeItem(org.bukkit.inventory.ItemStack.from(
            new org.bukkit.inventory.ItemStack(Material.TOTEM_OF_UNDYING)
        ));
    }

    @Override
    public void onRightClick(org.bukkit.event.player.PlayerInteractEvent event, Player player) {
        ItemAPI.message(player, "&c🔥 Талисман Феникса готов к защите!");
        ItemAPI.particles(player, Particle.FLAME, 20);
        ItemAPI.sound(player, Sound.BLOCK_FIRE_AMBIENT, 1f, 1f);
    }
}
