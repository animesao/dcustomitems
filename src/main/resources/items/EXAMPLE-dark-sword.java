/**
 * Пример Java предмета для DC-CustomItems.
 * 
 * Как использовать:
 * 1. Скопируйте этот файл в папку plugins/DC-CustomItems/items/
 * 2. Переименуйте (уберите EXAMPLE-)
 * 3. Напишите /ci reload
 * 4. Получите предмет: /api-item give dark_sword
 * 
 * Или через команду: /ci give dark_sword
 */
import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;

public class DarkSword extends AbstractCustomItem {

    @Override
    public String getId() { return "dark_sword"; }

    @Override
    public String getDisplayName() { return "&5Тёмный Клинок"; }

    @Override
    public Material getMaterial() { return Material.NETHERITE_SWORD; }

    @Override
    public java.util.List<String> getLore() {
        return java.util.List.of(
            "",
            " &7Тёмная энергия пульсирует в клинке",
            "",
            " &5ЛКМ: &7Тёмная волна",
            " &5ПКМ: &7Невидимость на 5 сек",
            "",
            " &5Урон: &c+12",
            " &5Шанс вампиризма: &c30%"
        );
    }

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public boolean isGlowing() { return true; }

    @Override
    public String getItemModel() { return "dark_sword"; }

    @Override
    public long getClickCooldown() { return 1000; }

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        // ПКМ - Невидимость на 5 сек
        ItemAPI.vanish(player, 5);
        ItemAPI.particles(player, Particle.SMOKE_LARGE, 50);
        ItemAPI.sound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
        ItemAPI.title(player, "&5ТЁМНАЯ ТЕНЬ", "&7Вы стали невидимы!");
    }

    @Override
    public void onLeftClick(PlayerInteractEvent event, Player player) {
        // ЛКМ - Тёмная волна (AoE урон)
        ItemAPI.damageNearby(player, 8, 5);
        ItemAPI.particles(player, Particle.SMOKE_LARGE, 100);
        ItemAPI.sound(player, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 2f);
        ItemAPI.title(player, "&5ТЁМНАЯ ВОЛНА", "");
    }

    @Override
    public void onDamageDealt(EntityDamageByEntityEvent event, Player player) {
        // +12 урона
        event.setDamage(event.getDamage() + 12);

        // 30% шанс вампиризма (восстановление здоровья)
        if (Math.random() < 0.30) {
            ItemAPI.heal(player, 3); // +3 сердца
            ItemAPI.particlesAt(event.getEntity().getLocation(), Particle.HEART, 10);
            ItemAPI.sound(player, Sound.ENTITY_PLAYER_BURP, 0.5f, 2f);
            ItemAPI.message(player, "&5Вы吸收или жизнь!");
        }

        // Эффект при ударе
        ItemAPI.particlesAt(event.getEntity().getLocation(), Particle.SMOKE_LARGE, 20);
    }

    @Override
    public void onKill(Player killer, Player victim) {
        // При убийстве - исцеление + сила
        ItemAPI.heal(killer, 5);
        ItemAPI.effect(killer, PotionEffectType.STRENGTH, 15, 2);
        ItemAPI.particles(killer, Particle.SMOKE_LARGE, 80);
        ItemAPI.sound(killer, Sound.ENTITY_WITHER_DEATH, 0.3f, 2f);
        ItemAPI.title(killer, "&5УБИЙСТВО!", "&7+5 ❤ &5+Сила II");
    }
}
