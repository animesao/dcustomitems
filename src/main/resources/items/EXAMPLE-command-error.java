import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import org.bukkit.entity.Player;
import org.bukkit.command.*;

/**
 * ❌ ПРИМЕР КОМАНДЫ С ОШИБКАМИ (для обучения)
 * 
 * Этот файл показывает ЧАСТЫЕ ОШИБКИ при создании команд.
 * 
 * В консоли выведет красное сообщение:
 * [API] ❌ EXAMPLE-command-error.java:XX - error message
 * 
 * Исправь ошибки чтобы команда заработала!
 */
public class EXAMPLEcommandError extends CustomCommand {

    // ❌ ОШИБКА 1: Неправильный конструктор
    // Правильно: super("name", "desc", "/usage", "permission");
    // Ошибка: super("name"); - не хватает параметров

    public EXAMPLEcommandError() {
        super("errorcmd", "Пример с ошибками", "/errorcmd", "dci.error");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // ❌ ОШИБКА 2: Использование methods которые не существуют
        // sendTitle(player, "title"); - не существует такого метода
        // Правильно: title(player, "title", "subtitle");

        Player target = getTarget(sender, args, 0);
        if (target == null) return false;

        // ❌ ОШИБКА 3: Неправильные импорты
        // Particle.FLAME - может не существовать в старых версиях
        // Правильно: использовать try-catch

        // ❌ ОШИБКА 4: Отсутствие проверок
        // target.setHealth(100); - может вызвать ошибку если > maxHealth

        // ❌ ОШИБКА 5: Медленные операции в main thread
        // Thread.sleep(1000); - НИКОГДА не делай так!
        // Правильно: использовать scheduler

        // ❌ ОШИБКА 6: Неправильный return
        // return; - не работает в void методе execute
        // Правильно: return true;

        return true;
    }
}

/**
 * КАК ИСПРАВИТЬ:
 * 
 * 1. Убедись что все методы существуют
 * 2. Проверяй импорты
 * 3. Используй try-catch для опасных операций
 * 4. Проверяй границы (health, food, level)
 * 5. Не блокируй main thread
 * 6. Возвращай true/false правильно
 */
