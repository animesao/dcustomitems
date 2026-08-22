<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.11-green?style=for-the-badge&logo=minecraft" alt="Minecraft">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/Version-1.321.1-blue?style=for-the-badge" alt="Version">
  <img src="https://img.shields.io/github/license/animesao/dcustomitems-purple?style=for-the-badge" alt="License">
</p>

<h1 align="center">⚔️ DC-CustomItems</h1>

<p align="center">
  <b>Модульный плагин кастомных предметов для Minecraft 1.21.11</b><br>
  Эффекты, атрибуты, сеты брони, триггеры, кастомные модели, Java API и многое другое!
</p>

<p align="center">
  <a href="#установка">Установка</a> •
  <a href="#быстрый-старт">Быстрый старт</a> •
  <a href="#команды">Команды</a> •
  <a href="#документация">Документация</a>
</p>

---

## ✨ Возможности

| Возможность | Описание |
|-------------|----------|
| 🗡️ Кастомные предметы | Руны, инструменты, броня, расходники |
| ⚡ Эффекты зелий | Автоприменение при экипировке |
| 📊 Атрибуты | Урон, скорость, броня и др. |
| 🛡️ Сеты брони | Бонусы за полный комплект |
| 🎯 Действия при клике | Молния, команды, эффекты, частицы, звуки |
| 🔄 Триггеры | Автодействия по событиям |
| 📦 YAML-файлы | Каждый предмет в отдельном файле |
| 🎨 Кастомные модели | Поддержка Resource Pack через `item-model` |
| 🔐 Права | Права на каждый предмет |
| ✨ Частицы и звуки | Эффекты экипировки/снятия |
| 🔗 PlaceholderAPI | 25+ экспортируемых плейсхолдеров |
| 🏪 Vault Economy | Покупка/продажа предметов |
| ☕ Java API | Создание предметов, команд, плейсхолдеров на Java |
| 📦 Модули | Включение/выключение компонентов через папки |
| 🎒 /give | Выдача кастомных + ванильных предметов |

---

## 📦 Установка

### Быстрая установка

```bash
# Скачать последний релиз
cd ~/server/plugins
curl -L -o dcustomitems.jar https://github.com/animesao/dcustomitems/releases/download/v1.321.1/DC-CustomItems-1.321.1.jar

# Перезапустить сервер
./restart.sh
```

### Ручная установка

1. Скачай `DC-CustomItems-1.321.1.jar` из [Releases](https://github.com/animesao/dcustomitems/releases/tag/v1.321.1)
2. Помести в папку `plugins/`
3. Перезапусти сервер

---

## 🚀 Быстрый старт

### 1. Выдай предмет

```bash
# Кастомный предмет
/give vampire-blade

# Ванильный предмет
/give diamond
/give minecraft:netherite_sword
/give cooked_beef 64
```

### 2. Создай свой предмет

Создай файл `plugins/DC-CustomItems/items/my-sword.yml`:

```yaml
my-sword:
  type: TOOL
  activation-slot: HAND

  item:
    type: DIAMOND_SWORD
    title: '&bМой Меч'
    glowing: true

  effects:
    - 'SPEED:1'
    - 'STRENGTH:2'

  right-click-actions:
    - 'heal:10'
    - 'message:&aВы исцелены на 10 HP!'

  left-click-actions:
    - 'lightning'
    - 'sound:ENTITY_ENDER_DRAGON_GROWL:1:1.5'

  click-cooldown: 3000
  equip-message: '&aМеч экипирован!'
  unequip-message: '&cМеч снят!'
  buy-price: 1000
  sell-price: 500
```

### 3. Перезагрузи и используй

```bash
/ci reload
/give my-sword
```

---

## 🎮 Команды

### Основные команды

| Команда | Описание |
|---------|----------|
| `/ci reload` | Перезагрузка плагина |
| `/give <id\|material>` | Выдать предмет себе |
| `/give <id\|material> <player>` | Выдать игроку |
| `/give <id\|material> <player> <amount>` | Выдать N штук |
| `/give <id\|material> all` | Выдать всем онлайн |
| `/give list` | Список кастомных предметов |
| `/give materials` | Список ванильных материалов |
| `/give materials <filter>` | Поиск материалов |

### Примеры

```bash
# Кастомные предметы
/give vampire-blade
/give test-sword Steve 3

# Ванильные предметы
/give diamond
/give minecraft:netherite_sword
/give cooked_beef 64
/give minecraft:acacia_boat

# Выдать всем
/give diamond all 16
/give health-potion all 5

# Поиск
/give materials
/give materials diamond
```

### Модульные команды

Команды регистрируются модулями:

| Модуль | Команды | Описание |
|--------|---------|----------|
| `items/vault/` | `/buy`, `/sell` | Покупка/продажа предметов |
| `items/deluxemenux/` | `/menu`, `/shop`, `/kits` | GUI-меню |
| `items/give-command.java` | `/give` | Выдача предметов |

---

## 🏗️ Архитектура

```
DC-CustomItems/
├── src/main/java/me/dcplugin/dcustomitems/
│   ├── Main.java                    # Точка входа (делегирует в PluginBootstrap)
│   ├── bootstrap/
│   │   ├── PluginBootstrap.java     # Инициализация всех компонентов
│   │   ├── CommandRegistrar.java    # Динамическая регистрация команд
│   │   └── ConfigMigrator.java      # Миграция конфига
│   ├── handlers/
│   │   ├── CustomItemHandler.java   # Менеджер кастомных предметов
│   │   ├── ItemLoader.java          # Загрузка из YAML
│   │   ├── LoreManager.java         # Шаблоны lore
│   │   ├── UsesManager.java         # Счётчик использований
│   │   └── EquippedItemsChecker.java # Глобальный таск проверки экипировки
│   ├── managers/
│   │   ├── EffectManager.java       # Эффекты зелий
│   │   ├── AttributeManager.java    # Атрибуты предметов
│   │   ├── ArmorSetManager.java     # Сеты брони
│   │   └── MessageManager.java      # Сообщения
│   ├── listeners/
│   │   ├── PlayerListener.java      # События игрока
│   │   └── TriggerListener.java     # Триггеры
│   ├── api/                         # Java API
│   ├── commands/                    # Встроенные команды
│   ├── models/                      # Модели данных
│   └── utils/
│       ├── EnumCache.java           # Кэширование Bukkit enum
│       ├── ColorUtils.java          # Цвета + PAPI
│       └── ItemBuilder.java         # Билдер предметов
├── src/main/resources/
│   ├── plugin.yml                   # Конфигурация плагина
│   ├── config.yml                   # Настройки
│   └── items/                       # YAML-предметы + модули
│       ├── *.yml                    # Предметы
│       ├── *.java                   # Java API модули
│       ├── vault/                   # Модуль экономики
│       ├── deluxemenux/             # Модуль GUI
│       └── _template/              # Шаблон для новых модулей
```

---

## 🎨 YAML-формат предмета

```yaml
my-item:
  type: TOOL                    # RUNE | TOOL | ARMOR | CONSUMABLE
  activation-slot: HAND         # HAND | OFFHAND | HEAD | CHEST | LEGS | FEET
  placeable: false

  item:
    type: DIAMOND_SWORD         # Minecraft материал
    title: '&6Мой Предмет'
    amount: 1
    glowing: true
    unbreakable: true
    item-model: "custom_model"  # Resource Pack model (1.21.11+)
    custom-model-data: 100      # Legacy model data
    texture: "eyJ..."          # Skull texture (PLAYER_HEAD)

    enchantments:
      sharpness: 5
      unbreaking: 3

    item-flags:
      - HIDE_ENCHANTS
      - HIDE_ATTRIBUTES

    attributes:
      GENERIC_ATTACK_DAMAGE: 10
      GENERIC_ATTACK_SPEED: 2

  effects:
    - 'SPEED:2'
    - 'STRENGTH:1'
    - 'REGENERATION:1'

  armor-set: "shadow"           # ID сета
  has-set-bonus: true

  lore:
    - '&7Урон: &c+10'
    - '&7Скорость: &a+20%'
    - '&7Кулдаун: &e%cooldown%с'
    - '&7Использования: &b%uses%'

  left-click-actions:
    - 'lightning'
    - 'sound:ENTITY_ENDER_DRAGON_GROWL:1:1.5'
    - 'particle:FLAME:50'

  right-click-actions:
    - 'heal:10'
    - 'message:&aВы исцелены!'
    - 'effect:REGENERATION:15:1'
    - 'teleport:~0:3:~0'
    - 'title:&6Сpecial!::&7Описание'
    - 'announce:&eИгрок %player% использовал предмет!'
    - 'command:give %player% diamond 1'

  trigger-actions:
    - 'on_kill:heal:5'
    - 'on_jump:particle:FLAME:20'

  triggers:                     # Новый формат
    on_equip:
      - 'sound:ENTITY_PLAYER_LEVELUP:1:1.5'
    on_click_right:
      - 'damage:15'
    on_kill:
      - 'heal:10'
      - 'exp:50'

  click-cooldown: 3000          # мс
  max-uses: 10                  # -1 = бесконечно
  permission: 'myplugin.use'

  equip-particles: 'FLAME:30'
  equip-sounds: 'ENTITY_PLAYER_LEVELUP:1:1'
  unequip-particles: 'SMOKE:20'
  unequip-sounds: 'ENTITY_VILLAGER_NO:1:1'

  equip-message: '&aПредмет экипирован!'
  unequip-message: '&cПредмет снят!'
  cooldown-message: '&cКулдаун: {seconds}с'
  activation-message: '&6Предмет активирован!'
  deactivation-message: '&7Предмет деактивирован!'
  uses-depleted-message: '&cПредмет использован!'

  buy-price: 1000
  sell-price: 500
```

---

## 🔗 PlaceholderAPI

### Настройка

1. Установи [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)
2. Плагин автоматически регистрирует плейсхолдеры при старте

### Плейсхолдеры игрока

| Плейсхолдер | Описание | Пример |
|-------------|----------|--------|
| `%dci_player%` | Имя игрока | `Steve` |
| `%dci_player_uuid%` | UUID | `8667ba71-...` |
| `%dci_health%` | Текущее здоровье | `20` |
| `%dci_max_health%` | Макс. здоровье | `20` |
| `%dci_health_percent%` | % здоровья | `100` |
| `%dci_food%` | Уровень еды | `20` |
| `%dci_saturation%` | Насыщение | `5` |
| `%dci_level%` | Уровень опыта | `30` |
| `%dci_x%`, `%dci_y%`, `%dci_z%` | Координаты | `100`, `64`, `-200` |
| `%dci_world%` | Мир | `world` |
| `%dci_gamemode%` | Режим игры | `SURVIVAL` |
| `%dci_fly_status%` | Полёт | `on` / `off` |
| `%dci_item_in_hand%` | Предмет в руке | `DIAMOND_SWORD` |
| `%dci_online%` | Онлайн | `15` |
| `%dci_max_players%` | Макс. игроков | `50` |

### Плейсхолдеры плагина

| Плейсхолдер | Описание | Пример |
|-------------|----------|--------|
| `%dci_version%` | Версия плагина | `1.321.1` |
| `%dci_item_count%` | YAML-предметов | `45` |
| `%dci_java_item_count%` | Java API предметов | `6` |
| `%dci_command_count%` | Команд | `3` |
| `%dci_placeholder_count%` | Плейсхолдеров | `12` |
| `%dci_module_count%` | Модулей | `2` |
| `%dci_database_type%` | Тип БД | `sqlite` |
| `%dci_active_effects%` | Активные эффекты | `SPEED:II, STRENGTH:I` |
| `%dci_active_effects_count%` | Кол-во эффектов | `3` |

### Динамические плейсхолдеры предметов

| Плейсхолдер | Описание | Пример |
|-------------|----------|--------|
| `%dci_has_<id>%` | Есть ли предмет | `%dci_has_vampire-blade%` → `true` |
| `%dci_holding_<id>%` | Держит ли в руке | `%dci_holding_rune-1%` → `false` |
| `%dci_equipped_<id>%` | Экипирован ли | `%dci_equipped_helmet%` → `true` |
| `%dci_item_amount_<id>%` | Кол-во в инвентаре | `%dci_item_amount_potion%` → `3` |

### Плейсхолдеры в сообщениях

Плейсхолдеры работают **автоматически** во всех сообщениях плагина:

```yaml
my-sword:
  equip-message: '&aHP: %dci_health%/%dci_max_health%'
  cooldown-message: '&cКулдаун: {seconds}s | &bУровень: &a%dci_level%'
```

---

## 🏪 Vault Economy (Модуль)

### Настройка

1. Установи [Vault](https://www.spigotmc.org/resources/vault.34315/) + плагин экономики (EssentialsX)
2. Папка `items/vault/` автоматически подключает экономику
3. Для отключения — удали папку `items/vault/`

### YAML-конфиг модуля

```yaml
# items/vault/config.yml
name: "Vault Economy"
version: "1.0"
enabled: true

shop:
  buy-enabled: true
  sell-enabled: true
  max-amount: 64
```

### Предметы с ценами

```yaml
my-sword:
  buy-price: 1000
  sell-price: 500
```

### Команды

| Команда | Описание |
|---------|----------|
| `/buy <id>` | Купить предмет |
| `/buy <id> <amount>` | Купить N штук |
| `/sell <id>` | Продать предмет |
| `/sell <id> <amount>` | Продать N штук |

---

## ☕ Java API

### Кастомный предмет

```java
import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class MySword extends AbstractCustomItem {

    @Override
    public String getId() { return "my-sword"; }

    @Override
    public String getDisplayName() { return "§6My Sword"; }

    @Override
    public Material getMaterial() { return Material.DIAMOND_SWORD; }

    @Override
    public ItemStack buildItemStack() {
        return new ItemStack(Material.DIAMOND_SWORD);
    }

    @Override
    public String getItemModel() { return "custom_sword"; }
}
```

### Кастомная команда

```java
import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HealCommand extends CustomCommand {

    public HealCommand() {
        super("heal", "Исцелить игрока", "/heal [player]", "myplugin.heal");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player target = getTarget(sender, args, 0);
        if (target == null) return true;

        target.setHealth(target.getAttribute(Attribute.MAX_HEALTH).getValue());
        msg(target, "&aВы исцелены!");
        return true;
    }
}
```

### Кастомный плейсхолдер

```java
import me.dcplugin.dcustomitems.api.placeholders.CustomPlaceholder;
import org.bukkit.entity.Player;

public class KillsPlaceholder extends CustomPlaceholder {

    public KillsPlaceholder() {
        super("kills"); // %dci_kills%
    }

    @Override
    public String getValue(Player player) {
        return String.valueOf(getPlugin().getDatabaseManager().queryInt(
            "SELECT kills FROM stats WHERE uuid=?",
            player.getUniqueId().toString()
        ));
    }
}
```

### Примеры в плагине

| Файл | Описание |
|------|----------|
| `EXAMPLE-kills-placeholder.java` | Счётчик убийств |
| `EXAMPLE-playtime-placeholder.java` | Время игры |
| `EXAMPLE-streak-placeholder.java` | Серия входов |
| `EXAMPLE-balance-placeholder.java` | Виртуальная валюта |
| `EXAMPLE-combo-placeholder.java` | Комбо убийств |
| `EXAMPLE-status-placeholder.java` | Ранг игрока |
| `EXAMPLE-dark-sword.java` | Тёмный меч |
| `EXAMPLE-phoenix-totem.java` | Тотем Феникса |
| `give-command.java` | Команда /give |

---

## 📦 Модули

### Включение/выключение

```bash
# Включить модуль — оставить папку
items/vault/        # ✅ Включён

# Выключить модуль — удалить папку
rm -rf items/vault/ # ❌ Выключен

# Или в config.yml модуля:
enabled: false
```

### Шаблон модуля

Скопируй `items/_template/` для создания нового модуля:

```bash
cp -r items/_template/ items/my-module/
mv items/my-module/template.java items/my-module/my-module.java
```

### Структура модуля

```
items/my-module/
├── config.yml      # Настройки модуля
├── my-module.java  # Java-класс модуля
└── README.md       # Документация
```

---

## 🛠️ Требования

- Minecraft 1.21.11+
- Paper/Spigot
- Java 17+
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (опционально)
- [Vault](https://www.spigotmc.org/resources/vault.34315/) + economy plugin (опционально)

---

## 📚 Документация

| Язык | Ссылка |
|------|--------|
| 🇬🇧 English | [Full Documentation](docs/README_EN.md) |
| 🇷🇺 Русский | [Полная документация](docs/README_RU.md) |
| ☕ Java API | [Java API Guide](docs/JAVA_API_RU.md) |
| 📦 YAML Предметы | [YAML Items Guide](docs/YAML_ITEMS_RU.md) |
| ⚡ Команды | [Commands Guide](docs/COMMANDS_RU.md) |
| 🗄️ База данных | [Database Guide](docs/DATABASE_RU.md) |
| 🎨 Resource Pack | [Resource Pack Guide](docs/RESOURCE_PACK_RU.md) |
| 🔧 Модули GUI | [Modules Guide](docs/MODULES_GUI_RU.md) |
| 💬 Сообщения | [Messages Guide](docs/MESSAGES_DATABASE_RU.md) |
| ❓ Решение проблем | [Troubleshooting](docs/TROUBLESHOOTING_RU.md) |

---

## 🔗 Ссылки

- [GitHub Repository](https://github.com/animesao/dcustomitems)
- [Releases](https://github.com/animesao/dcustomitems/releases)
- [Issues](https://github.com/animesao/dcustomitems/issues)

---

## 📄 Лицензия

MIT License — см. [LICENSE](LICENSE) для подробностей.

---

**Сделано с ❤️ by animesao**
