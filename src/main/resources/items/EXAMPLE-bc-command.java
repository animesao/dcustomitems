import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 📢 Команда /bc - Объявление серверу
 * 
 * Использование:
 *   /bc <текст>      - отправить объявление всем
 *   /bc -t <текст>   - отправить title всем
 *   /bc -a <текст>   - отправить actionbar всем
 * 
 * Право: dci.command.bc
 */
public class bc_commandItem extends CustomCommand {

    public bc_commandItem() {
        super(
            "bc",
            "Объявление серверу",
            "/bc <текст>",
            "dci.command.bc"
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            msg(sender, "&cИспользование: /bc <текст>");
            msg(sender, "&7Флаги: &e-t &7(title), &e-a &7(actionbar)");
            return true;
        }

        String type = "chat";
        String messageStart = args[0];
        int startIndex = 0;

        // Проверяем флаги
        if (messageStart.equals("-t") && args.length > 1) {
            type = "title";
            startIndex = 1;
        } else if (messageStart.equals("-a") && args.length > 1) {
            type = "actionbar";
            startIndex = 1;
        }

        // Собираем сообщение
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) messageBuilder.append(" ");
            messageBuilder.append(args[i]);
        }
        String message = messageBuilder.toString();

        if (message.isEmpty()) {
            msg(sender, "&cУкажите текст сообщения!");
            return true;
        }

        // Цветовые коды
        String coloredMessage = colorize(message);
        String prefix = "&8[&6Объявление&8] &r";

        // Получаем имя отправителя
        String senderName = sender instanceof Player ? sender.getName() : "Консоль";

        switch (type) {
            case "title":
                // Title объявление
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendTitle(coloredMessage, prefix + "&7от " + senderName, 10, 60, 20);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                }
                msg(sender, "&a✅ Title объявление отправлено!");
                break;

            case "actionbar":
                // Actionbar объявление
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.spigot().sendMessage(
                        net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(coloredMessage)
                    );
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                }
                msg(sender, "&a✅ Actionbar объявление отправлено!");
                break;

            default:
                // Обычное чат-объявление
                String fullMessage = prefix + coloredMessage + " &7(от " + senderName + ")";
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(colorize(fullMessage));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                }
                msg(sender, "&a✅ Объявление отправлено!");
                break;
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, int cursor) {
        if (cursor == 0) {
            List<String> options = new ArrayList<>();
            options.add("-t");
            options.add("-a");
            return options;
        }
        return Collections.emptyList();
    }
}
