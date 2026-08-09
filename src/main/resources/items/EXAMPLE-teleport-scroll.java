import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.api.ItemAPI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;
import java.util.Random;

public class TeleportScroll extends AbstractCustomItem {

    private final Random random = new Random();
    private final java.util.Map<java.util.UUID, Integer> charges = new java.util.HashMap<>();

    @Override
    public String getId() { return "teleport_scroll"; }

    @Override
    public String getDisplayName() { return "&d📜 &fСвиток Телепортации"; }

    @Override
    public Material getMaterial() { return Material.PAPER; }

    @Override
    public List<String> getLore() {
        int currentCharges = charges.getOrDefault(
            org.bukkit.Bukkit.getPlayerExact("") != null ? 
            org.bukkit.Bukkit.getPlayerExact("").getUniqueId() : null, 3
        );
        return List.of(
            "",
            " &7Телепортирует к случайному игроку",
            " &7ПКМ - телепортация",
            "",
            " &d✦ &fИспользований: &e3/3",
            "",
            " &7&oПредмет исчезнет после"
            + " исчерпания зарядов"
        );
    }

    @Override
    public String getItemModel() { return "teleport_scroll"; }

    @Override
    public boolean hasCooldown() { return true; }

    @Override
    public int getCooldownSeconds() { return 10; }

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        // Получить текущие заряды
        int currentCharges = charges.getOrDefault(player.getUniqueId(), 3);

        if (currentCharges <= 0) {
            ItemAPI.message(player, "&c❌ &7Свиток закончился!");
            ItemAPI.sound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            player.getInventory().getItemInMainHand().setAmount(0);
            return;
        }

        // Найти рандомного игрока
        List<Player> players = new java.util.ArrayList<>(org.bukkit.Bukkit.getOnlinePlayers());
        players.remove(player);

        if (players.isEmpty()) {
            ItemAPI.message(player, "&c❌ &7Нет игроков для телепортации!");
            return;
        }

        Player target = players.get(random.nextInt(players.size()));

        // Эффект перед телепортацией
        ItemAPI.particles(player, Particle.PORTAL, 100);
        ItemAPI.sound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        ItemAPI.title(player, "&d📜 &fТЕЛЕПОРТАЦИЯ", "&7К " + target.getName() + "...");

        // Телепортация
        ItemAPI.teleportToPlayer(player, target, 2);

        // Эффект после телепортации
        ItemAPI.particles(player, Particle.PORTAL, 100);
        ItemAPI.sound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        ItemAPI.title(player, "&a✅ &fПРИБЫТИЕ!", "&7Вы телепортированы к " + target.getName());

        // Уменьшить заряды
        currentCharges--;
        charges.put(player.getUniqueId(), currentCharges);

        if (currentCharges <= 0) {
            ItemAPI.message(player, "&d📜 &7Свиток использован полностью!");
            player.getInventory().getItemInMainHand().setAmount(0);
        } else {
            ItemAPI.message(player, "&d📜 &7Осталось зарядов: &e" + currentCharges);
        }
    }
}
