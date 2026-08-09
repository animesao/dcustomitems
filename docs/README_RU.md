# ⚔️ DC-CustomItems - Полная документация

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.11-green?style=for-the-badge&logo=minecraft" alt="Minecraft">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/Version-1.320.228-blue?style=for-the-badge" alt="Version">
  <img src="https://img.shields.io/badge/Paper-1.21.11-red?style=for-the-badge" alt="Paper">
</p>

**DC-CustomItems** — мощный плагин для Minecraft для создания кастомных предметов с эффектами, атрибутами, триггерами и поддержкой ресурс-паков.

---

## 📑 Содержание

- [Установка](#установка)
- [Конфигурация](#конфигурация)
- [Команды](#команды)
- [Разрешения](#разрешения)
- [Конфигурация предметов](#конфигурация-предметов)
- [Система моделей](#система-моделей)
- [Триггеры](#триггеры)
- [Действия](#действия)
- [Эффекты](#эффекты)
- [Атрибуты](#атрибуты)
- [Примеры](#примеры)
- [Решение проблем](#решение-проблем)

---

## 📦 Установка

### Шаг 1: Скачать

Скачайте последний релиз с [GitHub Releases](https://github.com/animesao/dcustomitems/releases/latest)

### Шаг 2: Установить

```bash
# Скопировать в папку plugins
cp DC-CustomItems-1.320.223.jar /path/to/server/plugins/

# Или через curl на VDS
cd ~/server/plugins
curl -L -o dcustomitems.jar https://github.com/animesao/dcustomitems/releases/download/v1.320.223/DC-CustomItems-1.320.223.jar
```

### Шаг 3: Перезапустить сервер

```bash
# Перезапустите сервер Minecraft
./restart.sh
# или
systemctl restart minecraft
```

### Шаг 4: Настроить предметы

Предметы хранятся в папке `plugins/DC-CustomItems/items/`.

---

## 📁 Конфигурация

### Структура файлов

```
plugins/DC-CustomItems/
├── config.yml              # Основные настройки плагина
├── messages.yml            # Сообщения плагина
└── items/                  # Папка предметов
    ├── vampire-blade.yml
    ├── shadow-blade.yml
    ├── thunder-axe.yml
    ├── frost-wand.yml
    └── ... (12 предметов в комплекте)
```

---

## 🎮 Команды

| Команда | Алиасы | Описание |
|---------|--------|----------|
| `/ci list` | `/customitems list` | Список всех предметов |
| `/ci give <id>` | `/customitems give <id>` | Выдать предмет себе |
| `/ci give <id> <player>` | `/customitems give <id> <player>` | Выдать предмет игроку |
| `/ci reload` | `/customitems reload` | Перезагрузить предметы |
| `/ci update` | `/customitems update` | Проверить обновления |

### Примеры

```bash
# Выдать vampire-blade себе
/ci give vampire-blade

# Выдать shadow-blade игроку
/ci give shadow-blade Steve

# Список всех предметов
/ci list

# Перезагрузить плагин
/ci reload
```

---

## 🔐 Разрешения

| Разрешение | Описание | По умолчанию |
|------------|----------|--------------|
| `customitems.use` | Базовое использование | op |
| `customitems.give` | Выдача предметов | op |
| `customitems.reload` | Перезагрузка плагина | op |
| `customitems.update` | Проверка обновлений | op |
| `customitems.*` | Все разрешения | op |

---

## 📝 Конфигурация предметов

### Базовая структура предмета

```yaml
item-id:
  type: TOOL                    # RUNE, TOOL, ARMOR, CONSUMABLE
  activation-slot: HAND         # HAND, OFFHAND, HEAD, CHEST, LEGS, FEET
  placeable: false              # Можно ли разместить?
  permission: custom.permission # Требуемое разрешение
  
  item:
    type: NETHERITE_SWORD       # Материал
    title: '&6Мой Меч'         # Отображаемое имя
    glowing: true               # Свечение чар
    unbreakable: true           # Неломаемость
    item-model: "smoke"         # Модель из ресурс-пака (1.21.11+)
    
    enchantments:
      SHARPNESS: 5
      UNBREAKING: 3
    
    item-flags:
      - HIDE_ENCHANTS
      - HIDE_UNBREAKABLE
  
  lore:
    - ''
    - ' &7Кастомное описание'
    - ''
  
  effects:
    - 'INCREASE_DAMAGE:3'
    - 'SPEED:2'
  
  attributes:
    GENERIC_ATTACK_DAMAGE: 10.0
    GENERIC_ATTACK_SPEED: 1.6
  
  click-cooldown: 1000          # Кулдаун в миллисекундах
  
  triggers:
    on_equip:
      - 'particles:FLAME:20'
    on_kill:
      - 'heal:20'
```

---

## 🎨 Система моделей (Minecraft 1.21.11+)

### Обзор

Плагин поддерживает новую систему `custom_model_data` со строками (1.21.4+).

### Поддерживаемые форматы

| Формат | Пример | Описание |
|--------|--------|----------|
| Короткий | `"smoke"` | Автоматически конвертируется в `minecraft:item/smoke` |
| Полный путь | `"minecraft:item/smoke"` | Прямой путь |
| Кастомный NS | `"myplugin:weapons/sword"` | Кастомное пространство имён |

### Конфигурация

```yaml
my-item:
  item:
    type: NETHERITE_SWORD
    # Все форматы работают:
    item-model: "smoke"
    # item-model: "minecraft:item/smoke"
    # item-model: "myplugin:custom_sword"
```

### Структура ресурс-пака

```
rp/assets/minecraft/
├── items/
│   └── netherite_sword.json    # Выбор модели
├── models/item/
│   ├── smoke.json              # 3D модель
│   ├── custom_sword.json       # Другая модель
│   └── ...
└── textures/item/
    ├── smoke.png               # Текстура
    ├── custom_sword.png        # Другая текстура
    └── ...
```

### Определение предметов (items/netherite_sword.json)

```json
{
  "model": {
    "type": "minecraft:select",
    "property": "minecraft:custom_model_data",
    "index": 0,
    "cases": [
      {
        "when": "smoke",
        "model": { "model": "minecraft:item/smoke" }
      },
      {
        "when": "minecraft:item/smoke",
        "model": { "model": "minecraft:item/smoke" }
      }
    ],
    "fallback": { "model": "minecraft:item/netherite_sword" }
  }
}
```

### Команды

```bash
# Использование custom_model_data со строками
/give @s minecraft:netherite_sword[minecraft:custom_model_data={strings:["smoke"]}]

# Использование item_model компонента (альтернатива)
/give @s minecraft:netherite_sword[item_model="minecraft:item/smoke"]
```

---

## 🔄 Триггеры

| Триггер | Описание |
|---------|----------|
| `on_equip` | При экипировке предмета |
| `on_unequip` | При снятии предмета |
| `on_click_right` | ПКМ с предметом |
| `on_click_left` | ЛКМ с предметом |
| `on_kill` | Убийство моба/игрока |
| `on_death` | Смерть игрока |
| `on_damage_taken` | Получение урона |
| `on_damage_dealt` | Нанесение урона |
| `on_jump` | Прыжок |
| `on_pickup` | Подбор предмета |
| `on_drop` | Выбрасывание предмета |

---

## ⚡ Действия

### 📢 Коммуникация

| Действие | Формат | Описание |
|----------|--------|----------|
| `message` | `message:Текст` | Сообщение игроку |
| `title` | `title:Заголовок:Подзаголовок:fadeIn:stay:fadeOut` | Заголовок на экране |
| `actionbar` | `actionbar:Текст` | Текст над хотбаром |
| `broadcast` | `broadcast:Текст` | Объявление всем игрокам |

**Плейсхолдеры:** `%player%` - Имя игрока

### ⚡ Эффекты

| Действие | Формат | Описание |
|----------|--------|----------|
| `effect` | `effect:ТИП:СЕКУНДЫ:УРОВЕНЬ` | Эффект зелья |
| `speed` | `speed:СЕКУНДЫ:УРОВЕНЬ` | Скорость (короткий) |
| `vanish` | `vanish:СЕКУНДЫ` | Невидимость |
| `glow` | `glow:СЕКУНДЫ` | Сияние |
| `flight` | `flight:true/false` | Полёт |

### 💥 Боевые

| Действие | Формат | Описание |
|----------|--------|----------|
| `heal` | `heal:СУММА` | Исцелить себя |
| `heal_nearby` | `heal_nearby:СУММА:РАДИУС` | Исцелить ближайших игроков |
| `damage` | `damage:СУММА` | Урон себе |
| `damage_nearby` | `damage_nearby:УРОН:РАДИУС` | AoE урон |
| `damage_mobs` | `damage_mobs:УРОН:РАДИУС` | Урон только мобам |
| `damage_players` | `damage_players:УРОН:РАДИУС` | Урон только игрокам |
| `lightning` | `lightning:КОЛИЧЕСТВО` | Молния на месте |
| `lightning_forward` | `lightning_forward:ДАЛЬНОСТЬ` | Молния вперед |
| `knockback` | `knockback:РАДИУС` | Отбросить nearby |
| `launch` | `launch:СИЛА` | Подбросить nearby |
| `stun` | `stun:СЕКУНДЫ:РАДИУС` | Оглушить nearby |

### 🎁 Предметы

| Действие | Формат | Описание |
|----------|--------|----------|
| `give` | `give:МАТЕРИАЛ:КОЛИЧЕСТВО` | Выдать предмет |
| `remove` | `remove:МАТЕРИАЛ:КОЛИЧЕСТВО` | Забрать предмет |
| `exp` | `exp:ОПЫТ:УРОВНИ` | Выдать опыт |

### 🌍 Мир

| Действие | Формат | Описание |
|----------|--------|----------|
| `teleport` | `teleport:X:Y:Z` | Телепортация |
| `teleport_relative` | `teleport_relative:X:Y:Z` | Относительный телепорт |
| `sethealth` | `sethealth:ЗНАЧЕНИЕ` | Установить здоровье |
| `setfood` | `setfood:ЗНАЧЕНИЕ` | Установить голод |

### 🎆 Эффекты

| Действие | Формат | Описание |
|----------|--------|----------|
| `particles` | `particles:ТИП:КОЛИЧЕСТВО` | Частицы |
| `sound` | `sound:ТИП:ГРОМКОСТЬ:ТОНАЛЬНОСТЬ` | Звук |
| `fireworks` | `fireworks:1` | Фейерверк |

### 🔧 Команды

| Действие | Формат | Описание |
|----------|--------|----------|
| `command` | `command:КОМАНДА` | Команда от имени игрока |
| `console_command` | `console_command:КОМАНДА` | Команда от консоли |

---

## 💊 Эффекты

### Доступные эффекты

| Эффект | Название |
|--------|----------|
| SPEED | Скорость |
| SLOW | Замедление |
| INCREASE_DAMAGE | Сила |
| HEAL | Мгновенное исцеление |
| HASTE | Спешка |
| JUMP | Усиление прыжка |
| REGENERATION | Регенерация |
| RESISTANCE | Сопротивление |
| FIRE_RESISTANCE | Огнестойкость |
| WATER_BREATHING | Водное дыхание |
| INVISIBILITY | Невидимость |
| NIGHT_VISION | Ночное зрение |
| HEALTH_BOOST | Прилив здоровья |
| ABSORPTION | Поглощение |
| SATURATION | Насыщение |

### Формат

```
effect:ТИП:СЕКУНДЫ:УРОВЕНЬ
```

**Примеры:**
```yaml
- 'effect:SPEED:10:2'          # Скорость II на 10 секунд
- 'effect:INCREASE_DAMAGE:5:3' # Сила III на 5 секунд
- 'effect:REGENERATION:15:1'   # Регенерация I на 15 секунд
```

---

## 📊 Атрибуты

### Доступные атрибуты

| Атрибут | Описание |
|---------|----------|
| GENERIC_ATTACK_DAMAGE | Урон атаки |
| GENERIC_ATTACK_SPEED | Скорость атаки |
| GENERIC_MAX_HEALTH | Максимальное здоровье |
| GENERIC_MOVEMENT_SPEED | Скорость передвижения |
| GENERIC_ARMOR | Броня |
| GENERIC_ARMOR_TOUGHNESS | Прочность брони |
| GENERIC_KNOCKBACK_RESISTANCE | Сопротивление отбрасыванию |
| GENERIC_LUCK | Удача |

### Формат

```yaml
attributes:
  GENERIC_ATTACK_DAMAGE: 10.0
  GENERIC_ATTACK_SPEED: 1.6
```

---

## 📦 Включённые предметы

### ⚔️ Оружие и Броня

| ID | Название | Тип | Описание |
|----|----------|-----|----------|
| `vampire-blade` | Клинок Вампира | Меч | Кража здоровья, AoE урон |
| `shadow-blade` | Теневой Клинок | Меч | Критические удары |
| `thunder-axe` | Громовой Топор | Топор | Атаки молнией |
| `frost-wand` | Ледяная Палочка | Палочка | Ледяные заклинания |
| `elemental-staff` | Стихийный Посох | Посох | М стихии |
| `berserker-axe` | Топор Берсерка | Топор | Режим ярости |
| `healer-amulet` | Амулет Целителя | Амулет | Исцеляющие способности |
| `nature-totem` | Тотем Природы | Тотем | Силы природы |
| `archer-bow` | Лук Лучника | Лук | Специальные стрелы |
| `shadow-helmet` | Теневой Шлем | Броня | Режим скрытности |
| `artifact-blade-of-destiny` | Клинок Судьбы | Меч | Ультимативное оружие |
| `artifact-chaos-orb` | Сфера Хаоса | Сфера | Случайные эффекты |

### 🧪 Расходуемые предметы

| ID | Название | Использований | Эффект |
|----|----------|---------------|--------|
| `health-potion` | Зелье Здоровья | 3 | Heal 10 + Regeneration |
| `speed-potion` | Зелье Скорости | 5 | Скорость II 30 сек |
| `golden-apple` | Золотое Яблоко | 2 | Heal + 3 эффекта |
| `xp-scroll` | Свиток Опыта | 1 | +500 XP + 5 уровней |
| `shield-totem` | Тотем Защиты | 4 | Absorption + Resistance |
| `fire-resistance-potion` | Зелье Огнестойкости | 3 | Огнестойкость 60 сек |
| `nature-talisman` | Талисман Природы | 1 | Регенерация 30 сек |
| `teleport-scroll` | Свиток Телепортации | 3 | Телепорт к случайному игроку |

---

## 🚀 Продвинутые действия (БЕЗ ОГРАНИЧЕНИЙ!)

### 🎨 Кастомные частицы
```yaml
# Формат: particles_custom:ТИП:КОЛИЧЕСТВО:X:Y:Z:OFF_X:OFF_Y:OFF_Z
- 'particles_custom:FLAME:100:0:1:0:2:2:2'
```

### 🔊 Последовательность звуков
```yaml
# Формат: sound_sequence:ЗВУК1:ГРОМКОСТЬ1:ТОН1;ЗВУК2:...
- 'sound_sequence:ENTITY_PLAYER_LEVELUP:1:1;ENTITY_ORB_PICKUP:0.5:1.5'
```

### 🎬 Последовательность заголовков (анимация!)
```yaml
# Формат: title_sequence:ЗАГОЛОВОК:ПОД:IN:STAY:OUT;...
- 'title_sequence:🔥 ПОДГОТОВКА!:::10:20:10;🔥 ОГОНЬ!:::10:30:10;🔥 ШТОРМ!:::10:40:10'
```

### 📜 Команды с задержками
```yaml
# Формат: command_sequence:КОМАНДА1:ЗАДЕРЖКА1;КОМАНДА2:...
- 'command_sequence:give %player% diamond 1:20;say Привет!:40'
```

### 💥 Кастомный урон (точные размеры)
```yaml
# Формат: damage_custom:УРОН:ШИРИНА:ВЫСОТА:ГЛУБИНА
- 'damage_custom:30:6:4:6'  # 30 урона в области 6x4x6
```

### ❤️ Кастомное исцеление с частицами
```yaml
# Формат: heal_custom:ИСЦЕЛЕНИЕ:ШИРИНА:ВЫСОТА:ГЛУБИНА:ЧАСТИЦЫ
- 'heal_custom:15:8:5:8:HEART'  # Лечение союзников в области 8x5x8
```

---

## 💡 Примеры

### Пример 1: Простой меч

```yaml
my-sword:
  type: TOOL
  activation-slot: HAND
  placeable: false
  
  item:
    type: DIAMOND_SWORD
    title: '&bАлмазный меч'
    glowing: true
    enchantments:
      SHARPNESS: 3
  
  effects:
    - 'SPEED:1'
```

### Пример 2: Полноценный предмет

```yaml
legendary-sword:
  type: TOOL
  activation-slot: HAND
  placeable: false
  permission: legendary.sword
  
  item:
    type: NETHERITE_SWORD
    title: '&6[⚔] &fЛегендарный Меч'
    glowing: true
    unbreakable: true
    item-model: "smoke"
    enchantments:
      SHARPNESS: 5
      UNBREAKING: 3
  
  lore:
    - ''
    - ' &7Легендарное оружие героев'
    - ''
    - ' &eПКМ: Удар молнией'
    - ' &eЛКМ: AoE урон'
    - ''
  
  effects:
    - 'INCREASE_DAMAGE:3'
    - 'SPEED:2'
  
  attributes:
    GENERIC_ATTACK_DAMAGE: 12.0
    GENERIC_ATTACK_SPEED: 1.8
  
  click-cooldown: 2000
  
  triggers:
    on_equip:
      - 'particles:FLAME:30'
      - 'sound:ENTITY_PLAYER_LEVELUP:1:1'
      - 'message:&6⚔ Легендарный меч экипирован!'
    
    on_click_right:
      - 'lightning_forward:100'
      - 'particles:EXPLOSION_LARGE:20'
      - 'title:&6⚡ МОЛНИЯ!::&7Берегись!'
    
    on_click_left:
      - 'damage_nearby:10:4'
      - 'knockback:3'
    
    on_kill:
      - 'heal:20'
      - 'effect:REGENERATION:10:2'
      - 'particles:HEART:20'
```

---

## 🔧 Решение проблем

### Модель предмета не работает

1. **Проверьте установку ресурс-пака:**
   - Скопируйте `SmokeSword_ResourcePack.zip` в `%appdata%\.minecraft\resourcepacks\`
   - Активируйте в Options → Resource Packs

2. **Перезагрузите ресурсы:**
   - Нажмите `F3 + T` в игре

3. **Проверьте формат:**
   - Используйте `item-model: "smoke"` в конфиге
   - Убедитесь что `items/netherite_sword.json` имеет совпадающее значение `when`

### Плагин не загружается

1. **Проверьте версию Java:** Требуется Java 17+
2. **Проверьте версию Paper:** Требуется Paper 1.21.11+
3. **Проверьте логи:** Ищите ошибки в `logs/latest.log`

### Команды не работают

1. **Проверьте разрешения:** Убедитесь что у вас есть разрешение `customitems.use`
2. **Проверьте ID предмета:** Используйте `/ci list` для просмотра доступных предметов

---

## 📚 Дополнительные ресурсы

- [GitHub репозиторий](https://github.com/animesao/dcustomitems)
- [Трекер ошибок](https://github.com/animesao/dcustomitems/issues)
- [Релизы](https://github.com/animesao/dcustomitems/releases)

---

## 📄 Лицензия

Этот проект лицензирован под MIT License.

---

**Сделано с ❤️ by animesao**
