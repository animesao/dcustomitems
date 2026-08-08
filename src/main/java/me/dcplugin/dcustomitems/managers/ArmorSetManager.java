package me.dcplugin.dcustomitems.managers;

import me.dcplugin.dcustomitems.Main;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class ArmorSetManager {

    private final Main plugin;
    private final Map<String, List<String>> armorSetEffects;

    public ArmorSetManager(Main plugin) {
        this.plugin = plugin;
        this.armorSetEffects = new HashMap<>();
        loadSetBonuses();
    }

    private void loadSetBonuses() {
        armorSetEffects.clear();

        if (plugin.getConfig().contains("set-bonuses")) {
            for (String setId : plugin.getConfig().getConfigurationSection("set-bonuses").getKeys(false)) {
                List<String> effects = plugin.getConfig().getStringList("set-bonuses." + setId + ".effects");
                armorSetEffects.put(setId, effects);
            }
        }
    }

    public int countSetPieces(Player player, String setId) {
        if (setId == null || setId.isEmpty()) {
            return 0;
        }

        ItemStack[] armor = player.getInventory().getArmorContents();
        int setPieces = 0;

        for (ItemStack armorPiece : armor) {
            if (armorPiece != null && armorPiece.getType() != org.bukkit.Material.AIR) {
                String itemId = plugin.getItemHandler().getCustomItemId(armorPiece);
                if (itemId != null) {
                    var customItem = plugin.getItemHandler().getCustomItem(itemId);
                    if (customItem != null && setId.equals(customItem.getArmorSetId())) {
                        setPieces++;
                    }
                }
            }
        }

        return setPieces;
    }

    public boolean hasFullSet(Player player, String setId) {
        return countSetPieces(player, setId) >= 4;
    }

    public List<String> getSetEffects(String setId) {
        return armorSetEffects.getOrDefault(setId, Collections.emptyList());
    }

    public void applySetBonus(Player player, String setId) {
        List<String> effectStrings = getSetEffects(setId);
        if (effectStrings.isEmpty()) {
            return;
        }

        Map<PotionEffectType, Integer> effects = plugin.getEffectManager().parseEffects(effectStrings);

        for (Map.Entry<PotionEffectType, Integer> entry : effects.entrySet()) {
            player.addPotionEffect(new PotionEffect(
                entry.getKey(),
                Integer.MAX_VALUE,
                entry.getValue() - 1,
                false,
                false
            ), true);
        }
    }

    public void removeSetBonus(Player player, String setId) {
        List<String> effectStrings = getSetEffects(setId);
        if (effectStrings.isEmpty()) {
            return;
        }

        Map<PotionEffectType, Integer> effects = plugin.getEffectManager().parseEffects(effectStrings);

        for (PotionEffectType effectType : effects.keySet()) {
            player.removePotionEffect(effectType);
        }
    }

    public void reload() {
        loadSetBonuses();
    }
}
