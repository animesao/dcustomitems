import me.dcplugin.dcustomitems.api.gui.CustomGUI;
import me.dcplugin.dcustomitems.Main;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 🎨 Пример кастомного GUI на Java
 * 
 * Этот класс создаёт GUI программно
 * с полным контролем над логикой
 * 
 * Использование: /gui custom
 */
public class EXAMPLEcustom_gui extends CustomGUI {

    public EXAMPLEcustom_gui() {
        super("custom_gui", "&6Кастомное Меню", 27);
    }

    @Override
    public void onOpen(Player player) {
        // Заполняем фон стеклом
        fill(Material.GRAY_STAINED_GLASS_PANE, " ");
        
        // Устанавливаем предметы
        setItem(4, Material.NETHERITE_SWORD,
            "&c⚔ Боевой Меч",
            "",
            "&7Нажми чтобы получить меч!",
            ""
        );
        
        setItem(13, Material.ELYTRA,
            "&b✈ Элитры",
            "",
            "&7Нажми чтобы летать!",
            ""
        );
        
        setItem(22, Material.BARRIER,
            "&cЗакрыть",
            "",
            "&7Закрыть меню",
            ""
        );
    }

    @Override
    public void onClick(Player player, int slot, ItemStack item) {
        switch (slot) {
            case 4: // Боевой меч
                player.getInventory().addItem(createItem(Material.NETHERITE_SWORD, "&c⚔ Боевой Меч"));
                player.sendMessage("&aВы получили боевой меч!".replace("&", "§"));
                close(player);
                break;
                
            case 13: // Элитры
                player.getInventory().addItem(createItem(Material.ELYTRA, "&b✈ Элитры"));
                player.sendMessage("&aВы получили элитры!".replace("&", "§"));
                close(player);
                break;
                
            case 22: // Закрыть
                close(player);
                break;
        }
    }
}
