# DC-CustomItems Документация

## 📚 Оглавление

- [Обзор](#обзор)
- [Возможности](#возможности)
- [Установка](#установка)
- [Быстрый Старт](#быстрый-старт)
- [Java Предметы](#java-предметы)
- [Java Команды](#java-команды)
- [Java Плейсхолдеры](#java-плейсхолдеры)
- [Конфигурация Сообщений](#конфигурация-сообщений)
- [Команды](#команды)
- [Поддержка](#поддержка)

---

## 📖 Обзор

DC-CustomItems - плагин для серверов Minecraft (Spigot/Paper 1.20+), который позволяет создавать кастомные предметы, команды и плейсхолдеры на чистом Java.

**Всё горячо перезагружается!** Просто положи .java файл в папку `items/` и сделай `/ci reload`.

---

## ✨ Возможности

| Функция | Описание |
|---------|----------|
| **Java Предметы** | Создавай предметы с способностями |
| **Java Команды** | Создавай кастомные команды |
| **Java Плейсхолдеры** | Создавай кастомные плейсхолдеры |
| **Горячая Перезагрузка** | Обновление без перезапуска |
| **База Данных** | SQLite для хранения данных |
| **Кастомные Сообщения** | Редактируй все сообщения |

---

## 🚀 Установка

```bash
cd ~/plugins
curl -L -o dcustomitems.jar https://github.com/animesao/dcustomitems/releases/latest/download/DC-CustomItems.jar
```

Перезапусти сервер или сделай `/ci reload`.

---

## 🎮 Быстрый Старт

### Шаг 1: Создай Предмет

Создай `my-sword.java` в `plugins/DC-CustomItems/items/`:

```java
import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class MySword extends AbstractCustomItem {
    
    @Override
    public String getId() { return "my_sword"; }
    
    @Override
    public String getDisplayName() { return "&6Мой Меч"; }
    
    @Override
    public Material getMaterial() { return Material.DIAMOND_SWORD; }
    
    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        player.setHealth(20);
        player.sendMessage("Исцелён!");
    }
}
```

### Шаг 2: Перезагрузи

```
/ci reload
```

### Шаг 3: Получи Предмет

```
/ci give my_sword
```

---

## ☕ Java Предметы

### Базовый Класс

Наследуй `AbstractCustomItem`:

```java
public class MyItem extends AbstractCustomItem {
    @Override
    public String getId() { return "my_item"; }
    
    @Override
    public String getDisplayName() { return "&6Мой Предмет"; }
    
    @Override
    public Material getMaterial() { return Material.DIAMOND_SWORD; }
}
```

### Доступные Методы

| Метод | Когда Вызывается |
|-------|------------------|
| `onLeftClick(event, player)` | ЛКМ |
| `onRightClick(event, player)` | ПКМ |
| `onEquip(player)` | Экипировка |
| `onUnequip(player)` | Снятие |
| `onDamageDealt(event, player)` | Нанесение урона |
| `onDamageTaken(event, player)` | Получение урона |
| `onKill(killer, victim)` | Убийство |
| `onBlockBreak(player, event)` | Ломание блока |

### Утилиты ItemAPI

```java
ItemAPI.heal(player, 10);
ItemAPI.effect(player, PotionEffectType.SPEED, 30, 2);
ItemAPI.particles(player, Particle.FLAME, 50);
ItemAPI.sound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
ItemAPI.teleport(player, x, y, z);
ItemAPI.title(player, "Заголовок", "Подзаголовок");
```

---

## 📝 Java Команды

### Базовый Класс

Наследуй `CustomCommand`:

```java
import me.dcplugin.dcustomitems.api.commands.CustomCommand;

public class HealCommand extends CustomCommand {
    
    public HealCommand() {
        super("heal", "Исцелить", "/heal [игрок]", "dci.heal");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player target = getTarget(sender, args, 0);
        if (target == null) return false;
        
        target.setHealth(20);
        msg(sender, "&aВы исцелили " + target.getName());
        return true;
    }
}
```

### Доступные Методы

| Метод | Описание |
|-------|----------|
| `msg(sender, text)` | Отправить цветное сообщение |
| `title(player, title, sub)` | Отправить заголовок |
| `getTarget(sender, args, index)` | Получить игрока из аргументов |
| `getInt(args, index, def)` | Получить число из аргументов |
| `colorize(text)` | Преобразовать & в § |

---

## 🏷️ Java Плейсхолдеры

### Базовый Класс

Наследуй `CustomPlaceholder`:

```java
import me.dcplugin.dcustomitems.api.placeholders.CustomPlaceholder;

public class MoneyPlaceholder extends CustomPlaceholder {
    
    public MoneyPlaceholder() {
        super("money"); // %money%
    }
    
    @Override
    public String getValue(Player player) {
        return "1000"; // Твоя логика
    }
}
```

### Использование

```
Баланс: %money%
```

---

## 🔧 Конфигурация Сообщений

Редактируй `EXAMPLE-messages.java` в `items/` для изменения сообщений:

```java
MessagesConfig.PREFIX = "&8[&6МойПлагин&8] &r";
MessagesConfig.ITEM_GIVEN = PREFIX + "&aВыдали &e{item}&a!";
```

---

## 📋 Команды

| Команда | Описание |
|---------|----------|
| `/ci give <id> [игрок]` | Выдать предмет |
| `/ci list` | Список предметов |
| `/ci reload` | Перезагрузить плагин |
| `/api-item give <id> [игрок]` | Выдать API предмет |
| `/api-item list` | Список API предметов |

---

## 📁 Структура Файлов

```
plugins/DC-CustomItems/
├── items/
│   ├── my-sword.java         <- Предмет
│   ├── heal-command.java     <- Команда
│   ├── money-placeholder.java <- Плейсхолдер
│   └── messages.java         <- Конфиг сообщений
├── compiled/                 <- Авто-генерация
└── data.db                  <- База данных
```

---

## 📞 Поддержка

- **GitHub:** https://github.com/animesao/dcustomitems
- **Issues:** https://github.com/animesao/dcustomitems/issues

---

**Версия:** 1.320.249
