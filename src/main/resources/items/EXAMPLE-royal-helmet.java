import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * Пример королевского шлема (броня) на Java API.
 * 
 * При экипировке даёт эффекты защиты.
 * В бою отражает урон.
 */
public class RoyalHelmet extends AbstractCustomItem {

    @Override
    public String getId() { return "royal_helmet"; }

    @Override
    public String getDisplayName() { return "&6👑 Королевский Шлем"; }

    @Override
    public Material getMaterial() { return Material.NETHERITE_HELMET; }

    @Override
    public java.util.List<String> getLore() {
        return java.util.List.of(
            "",
            " &7Шлем древних королей",
            "",
            " &6Пассивно: &fСопротивление I",
            " &6При ударе: &fОтражение урона",
            ""
        );
    }

    @Override
    public String getItemModel() { return "royal_helmet"; }

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public boolean isGlowing() { return true; }

    @Override
    public String getActivationSlot() { return "HEAD"; }

    @Override
    public String getType() { return "ARMOR"; }

    @Override
    public java.util.List<me.dcplugin.dcustomitems.api.RecipeDef> getRecipes() {
        // Рецепт попадает и в обычный верстак, и в GUI-крафт (/craft).
        // Ингредиенты: материал (NETHERITE_INGOT) или ID другого кастомного
        // предмета — YAML или Java (например "vampire-blade").
        java.util.Map<Character, String> keys = new java.util.HashMap<>();
        keys.put('N', "NETHERITE_INGOT");
        return java.util.List.of(
            me.dcplugin.dcustomitems.api.RecipeDef.shaped(
                java.util.List.of("NNN", "N N"), keys)
        );
    }

    @Override
    public long getPeriodicInterval() { return 60; } // Каждые 3 секунды

    @Override
    public void onPeriodic(Player player) {
        // Пассивный эффект сопротивления
        ItemAPI.effect(player, PotionEffectType.RESISTANCE, 5, 1);
        ItemAPI.particles(player, Particle.ENCHANT, 5);
    }

    @Override
    public void onEquip(Player player) {
        ItemAPI.effect(player, PotionEffectType.RESISTANCE, 999, 1);
        ItemAPI.sound(player, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 1f);
        ItemAPI.particles(player, Particle.TOTEM_OF_UNDYING, 30);
        ItemAPI.title(player, "&6👑 КОРОЛЕВСКИЙ ШЛЕМ", "&7Сила королей с тобой!");
    }

    @Override
    public void onUnequip(Player player) {
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        ItemAPI.sound(player, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 0.8f);
    }

    @Override
    public void onDamageTaken(org.bukkit.event.entity.EntityDamageEvent event, Player player) {
        // Отражаем 30% урона
        double reflected = event.getDamage() * 0.3;
        event.setDamage(event.getDamage() * 0.7);
        
        if (event.getDamageSource().getCausingEntity() instanceof Player) {
            Player attacker = (Player) event.getDamageSource().getCausingEntity();
            attacker.damage(reflected);
            ItemAPI.message(attacker, "&6👑 Урон отражён!");
        }
        
        ItemAPI.particlesAt(player.getLocation(), Particle.TOTEM_OF_UNDYING, 20);
        ItemAPI.sound(player, Sound.ITEM_SHIELD_BLOCK, 1f, 1.5f);
    }
}
