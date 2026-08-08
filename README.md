<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.8-green?style=for-the-badge&logo=minecraft" alt="Minecraft">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/Version-1.320.207-blue?style=for-the-badge" alt="Version">
  <img src="https://img.shields.io/github/license/animesao/dcustomitems-purple?style=for-the-badge" alt="License">
</p>

<h1 align="center">⚔️ DC-CustomItems</h1>

<p align="center">
  <b>Мощный плагин кастомных предметов для Minecraft</b><br>
  Эффекты, атрибуты, сет-бонусы, триггеры и многое другое!
</p>

<p align="center">
  <a href="#установка">Установка</a> •
  <a href="#команды">Команды</a> •
  <a href="#конфигурация">Конфигурация</a> •
  <a href="#фичи">Фичи</a> •
  <a href="#документация">Документация</a>
</p>

---

## Возможности

| Функция | Описание |
|---------|----------|
| 🗡️ Кастомные предметы | Руны, инструменты, броня, зелья |
| ⚡ Эффекты зелий | Автоматическое применение при экипировке |
| 📊 Атрибуты | Урон, скорость, броня и другие характеристики |
| 🛡️ Сеты брони | Бонусы за полный комплект |
| 🎯 Действия при клике | Молния, команды, эффекты, частицы, звуки |
| 🔄 Триггеры | Автоматические действия при событиях |
| 📦 Per-item файлы | Каждый предмет в отдельном YAML |
| 🎨 CustomModelData | Поддержка ресурспаков |
| 🔐 Пермишены | Права на каждый предмет |
| ✨ Частицы и звуки | Эффекты при экипировке/снятии |

---

## Установка

1. Скачайте последний релиз с [GitHub Releases](https://github.com/animesao/dcustomitems/releases/latest)
2. Поместите `DC-CustomItems.jar` в папку `plugins/`
3. Перезапустите сервер
4. Настройте предметы в `plugins/DC-CustomItems/items/`

---

## Команды

| Команда | Описание |
|---------|----------|
| `/customitems list` | Список всех предметов |
| `/customitems give <id> [игрок]` | Выдать предмет |
| `/customitems reload` | Перезагрузить конфиг |
| `/customitems update` | Проверить обновления |

---

## Конфигурация

### Структура файлов

```
plugins/DC-CustomItems/
├── config.yml              # Основные настройки
├── messages.yml            # Сообщения плагина
└── items/                  # Папка с предметами
    ├── shadow-blade.yml
    ├── vampire-blade.yml
    ├── thunder-axe.yml
    └── ... (10 предметов)
```

### Пример предмета

```yaml
my-sword:
  type: TOOL
  activation-slot: HAND
  placeable: false
  permission: my.sword

  item:
    type: NETHERITE_SWORD
    title: '&6Мой Меч'
    glowing: true
    unbreakable: true
    enchantments:
      SHARPNESS: 5

  effects:
    - 'INCREASE_DAMAGE:3'
    - 'SPEED:2'

  right-click-actions:
    - 'lightning:2'
    - 'particle:FLAME:30'
    - 'message:&6Молнии призваны!'

  trigger-actions:
    - 'on_kill:heal:20'
    - 'on_jump:effect:SPEED:3:2'
```

---

## Триггеры

| Триггер | Описание |
|---------|----------|
| `on_kill` | При убийстве |
| `on_death` | При смерти |
| `on_damage_taken` | При получении урона |
| `on_damage_dealt` | При нанесении урона |
| `on_jump` | При прыжке |
| `on_pickup` | При подборе |
| `on_drop` | При выбрасывании |

---

## Действия

### 📢 Коммуникация
- `message` - Сообщение игроку
- `announce` - Сообщение всем
- `title` - Заголовок на экране
- `actionbar` - Текст над хотбаром

### ⚡ Эффекты
- `effect:ТИП:СЕК:УРОВЕНЬ` - Эффект зелья

### 💥 Боевые
- `lightning:КОЛ` - Молния
- `damage:СУММА` - Урон
- `heal:СУММА` - Исцеление

### 🎁 Предметы
- `give:МАТЕРИАЛ:КОЛ` - Выдать предмет
- `remove:МАТЕРИАЛ:КОЛ` - Убрать предмет
- `exp:ОПЫТ:УРОВНИ` - Опыт

### 🌍 Мир
- `teleport:X:Y:Z` - Телепортация
- `sethealth:ЗНАЧЕНИЕ` - Здоровье
- `setfood:ЗНАЧЕНИЕ` - Голод

### 🎆 Эффекты
- `particle:ТИП:КОЛ` - Частицы
- `sound:ТИП:ГРОМ:ТОН` - Звук
- `fireworks:1` - Фейерверк
- `vanish:СЕК` - Невидимость
- `glow:СЕК` - Сияние

### 🔧 Команды
- `command:КОМАНДА` - Выполнить команду

---

## Документация

📄 **[Полная документация](docs.md)**

---

## Обновления

Плагин автоматически проверяет обновления через GitHub Releases.

---

## Вклад

Смотрите [CONTRIBUTING.md](CONTRIBUTING.md) для информации о том, как внести вклад.

---

## Лицензия

Этот проект лицензирован под MIT License - смотрите [LICENSE](LICENSE) для подробностей.

---

## Авторы

- **DC-CustomItems** - [GitHub](https://github.com/animesao/dcustomitems)
