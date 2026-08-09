/**
 * ============================================================
 * ПРИМЕР: ТЁМНЫЙ КЛИНОК - Java предмет с кастомной моделью
 * ============================================================
 * 
 * 📁 Структура файлов:
 * 
 * plugins/DC-CustomItems/
 * └── items/
 *     └── dark-sword.java          ← этот файл
 * 
 * plugins/DC-CustomItems/resource-pack/  (или в папке ресурс-пака сервера)
 * └── assets/minecraft/
 *     ├── models/item/
 *     │   └── dark_sword.json      ← модель
 *     └── textures/item/
 *         └── dark_sword.png       ← текстура
 * 
 * 🎮 Команды:
 *   /ci reload                     - перезагрузить предметы
 *   /api-item give dark_sword      - выдать предмет
 *   /api-item info dark_sword      - информация о предмете
 * 
 * 🎨 Как работает модель:
 *   1. В Java коде: getItemModel() → "dark_sword"
 *   2. Плагин применяет: CustomModelData.setStrings(["dark_sword"])
 *   3. Ресурс-пак подхватывает модель: models/item/dark_sword.json
 *   4. Предмет отображается с кастомной текстурой!
 * 
 * ============================================================
 */

import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;

public class DarkSword extends AbstractCustomItem {

    // ===== ОСНОВНЫЕ СВОЙСТВА =====

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
            " &5ЛКМ: &7Тёмная волна (AoE урон)",
            " &5ПКМ: &7Невидимость на 5 сек",
            "",
            " &5Урон: &c+12",
            " &5Шанс вампиризма: &c30%"
        );
    }

    // ===== 🎨 КАСТОМНАЯ МОДЕЛЬ =====
    // Укажи имя модели из ресурс-пака
    // Файл: assets/minecraft/models/item/dark_sword.json
    // Текстура: assets/minecraft/textures/item/dark_sword.png
    @Override
    public String getItemModel() { return "dark_sword"; }

    // ===== ДОПОЛНИТЕЛЬНЫЕ СВОЙСТВА =====

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public boolean isGlowing() { return true; }

    @Override
    public long getClickCooldown() { return 1000; }

    @Override
    public String getPermission() { return "items.dark_sword"; }

    // ===== ТРИГГЕРЫ =====

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

        // 30% шанс вампиризма
        if (Math.random() < 0.30) {
            ItemAPI.heal(player, 3);
            ItemAPI.particlesAt(event.getEntity().getLocation(), Particle.HEART, 10);
            ItemAPI.sound(player, Sound.ENTITY_PLAYER_BURP, 0.5f, 2f);
            ItemAPI.message(player, "&5Вы吸收или жизнь!");
        }

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

    @Override
    public void onEquip(Player player) {
        // При экипировке
        ItemAPI.effect(player, PotionEffectType.DARKNESS, 999, 1);
        ItemAPI.particles(player, Particle.SMOKE_LARGE, 30);
    }

    @Override
    public void onUnequip(Player player) {
        // При снятии
        player.removePotionEffect(PotionEffectType.DARKNESS);
    }
}
