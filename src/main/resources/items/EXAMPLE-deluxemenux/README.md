# 🎨 DeluxeMenuX - Advanced Menu System

Полноценная система меню для Minecraft сервера.

> 📦 Эта папка — образец модуля (`EXAMPLE-*`). Чтобы включить модуль:
> скопируй её в `items/` и убери префикс `EXAMPLE-` из имени папки
> (`EXAMPLE-deluxemenux/` → `deluxemenux/`), затем `/ci reload`.
> Пока папка называется `EXAMPLE-*`, она не загружается.

## 📁 Структура модуля

```
items/deluxemenux/   (после включения EXAMPLE-deluxemenux/ -> deluxemenux/)
├── config.yml          # Настройки модуля
├── deluxemenux.java    # Основной класс
├── menu-command.java   # Команда /menu
├── kits-command.java   # Команда /kits
├── shop-command.java   # Команда /shop
├── README.md           # Документация
└── menus/              # Папка с меню
    ├── shop.yml        # Меню магазина, открывается через /shop
    ├── kits.yml        # Меню наборов, открывается через /kits
    ├── main.yml        # Главное меню
    └── ...
```

## 🚀 Быстрый старт

### 1. Создай меню

Создай файл `menus/my-menu.yml`:

```yaml
title: "&6Моё Меню"
size: 27
open-sound: "UI_BUTTON_CLICK"
close-sound: "UI_BUTTON_CLICK"

# Фон (заполнение пустых слотов)
fill:
  material: GRAY_STAINED_GLASS_PANE
  name: " "

# Предметы
items:
  # Слот 13 (центр)
  13:
    material: DIAMOND
    name: "&bАлмаз"
    lore:
      - ""
      - "&7Нажмите чтобы получить"
      - ""
    command: "give %player% diamond 1"
    message: "&aВы получили алмаз!"
    sound: "ENTITY_PLAYER_LEVELUP"
    close: false
```

### 2. Открой меню

```
/menu my-menu
```

## 📋 Формат YAML

### Основные настройки

```yaml
title: "&6Заголовок"    # Заголовок меню (с цветами)
size: 27                # Размер (9, 18, 27, 36, 45, 54)
open-sound: "UI_BUTTON_CLICK"  # Звук открытия
close-sound: "UI_BUTTON_CLICK" # Звук закрытия
```

### Фон (fill)

```yaml
fill:
  material: GRAY_STAINED_GLASS_PANE
  name: " "
  lore:
    - "&7Описание"
```

### Предметы (items)

```yaml
items:
  # Номер слота
  13:
    material: DIAMOND           # Материал
    name: "&bНазвание"         # Название (с цветами)
    lore:                       # Описание
      - "&7Строка 1"
      - "&7Строка 2"
    amount: 1                   # Количество
    data: 0                     # Data value (для старых версий)
    command: "give %player% diamond 1"  # Команда
    message: "&aСообщение"     # Сообщение игроку
    permission: "my.permission" # Права
    sound: "ENTITY_PLAYER_LEVELUP"  # Звук
    close: false                # Закрыть меню после клика
```

## 🎯 Примеры меню

### Магазин

```yaml
title: "&6🛒 Магазин"
size: 45
open-sound: "UI_BUTTON_CLICK"

fill:
  material: GRAY_STAINED_GLASS_PANE
  name: " "

items:
  # Заголовок
  4:
    material: PLAYER_HEAD
    name: "&6&lМагазин"
    lore:
      - ""
      - "&7Выберите товар"
      - ""

  # Товары
  19:
    material: DIAMOND
    name: "&bАлмаз"
    lore:
      - ""
      - "&7Цена: &a100$"
      - ""
    command: "give %player% diamond 1"
    message: "&aВы купили алмаз!"
    sound: "ENTITY_PLAYER_LEVELUP"

  22:
    material: EMERALD
    name: "&aИзумруд"
    lore:
      - ""
      - "&7Цена: &a150$"
      - ""
    command: "give %player% emerald 1"
    message: "&aВы купили изумруд!"
    sound: "ENTITY_PLAYER_LEVELUP"

  25:
    material: GOLD_INGOT
    name: "&6Золото"
    lore:
      - ""
      - "&7Цена: &a50$"
      - ""
    command: "give %player% gold_ingot 1"
    message: "&aВы купили золото!"
    sound: "ENTITY_PLAYER_LEVELUP"

  # Закрыть
  40:
    material: BARRIER
    name: "&cЗакрыть"
    lore:
      - ""
      - "&7Закрыть меню"
      - ""
    close: true
    sound: "UI_BUTTON_CLICK"
```

### Навигация между меню

```yaml
title: "&6Главное Меню"
size: 27

fill:
  material: BLACK_STAINED_GLASS_PANE
  name: " "

items:
  11:
    material: EMERALD_BLOCK
    name: "&a&lМагазин"
    lore:
      - ""
      - "&7Открыть магазин"
      - ""
    command: "menu shop"
    sound: "UI_BUTTON_CLICK"

  13:
    material: CHEST
    name: "&b&lИнвентарь"
    lore:
      - ""
      - "&7Управление"
      - ""
    command: "ci list"
    sound: "UI_BUTTON_CLICK"

  15:
    material: COMMAND_BLOCK
    name: "&e&lКоманды"
    lore:
      - ""
      - "&7Список команд"
      - ""
    command: "help"
    sound: "UI_BUTTON_CLICK"
```

## 🎮 Команды

| Команда | Описание |
|---------|----------|
| `/menu` | Открыть главное меню |
| `/menu <id>` | Открыть меню по ID |
| `/menu list` | Список всех меню |
| `/menu reload` | Перезагрузить меню |
| `/kits` | Открыть меню наборов `menus/kits.yml` |
| `/kit` | Алиас команды `/kits` |
| `/shop` | Открыть магазин `menus/shop.yml` |

Права:

```text
deluxemenux.use       # /menu, /kits и /shop
deluxemenux.admin     # /menu reload
deluxemenux.kit.start # стартовый набор
deluxemenux.kit.iron  # железный набор
deluxemenux.kit.diamond
deluxemenux.shop.diamond
deluxemenux.shop.emerald
deluxemenux.shop.gold
```

Файлы `kits.yml` и `shop.yml` создаются автоматически только при их отсутствии. Если ты изменишь YAML, `/ci reload` не перезапишет его.

## 🔧 Плейсхолдеры

| Плейсхолдер | Описание |
|-------------|----------|
| `%player%` | Имя игрока |
| `%player_name%` | Имя игрока |
| `%health%` | Здоровье |
| `%food%` | Еда |
| `%level%` | Уровень |

## 📝 Примеры команд

```yaml
# Выдать предмет
command: "give %player% diamond 1"

# Телепортация
command: "tp %player% 100 64 100"

# Эффект
command: "effect give %player% speed 30 1"

# Открыть другое меню
command: "menu shop"

# Оповещение
command: "bc %player% купил алмаз!"

# Переключение полёта
command: "fly %player%"
```

## 🎨 Цветовые коды

| Код | Цвет |
|-----|------|
| `&0` | Чёрный |
| `&1` | Тёмно-синий |
| `&2` | Тёмно-зелёный |
| `&3` | Тёмно-бирюзовый |
| `&4` | Тёмно-красный |
| `&5` | Фиолетовый |
| `&6` | Золотой |
| `&7` | Серый |
| `&8` | Тёмно-серый |
| `&9` | Синий |
| `&a` | Зелёный |
| `&b` | Бирюзовый |
| `&c` | Красный |
| `&d` | Розовый |
| `&e` | Жёлтый |
| `&f` | Белый |
| `&l` | Жирный |
| `&o` | Курсив |
| `&n` | Подчёркнутый |
| `&k` | Обfuscated |
| `&r` | Сброс |

## ⚡ Советы

1. **Размер меню**: Используй 9, 18, 27, 36, 45 или 54
2. **Слоты**: Нумеруются с 0 (левый верх) до size-1 (правый низ)
3. **Команды**: Используй `%player%` для подстановки имени
4. **Звуки**: Полный список: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html
5. **Материалы**: Полный список: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html
