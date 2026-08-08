# DC-CustomItems - Полная документация

## Описание

DC-CustomItems — мощный плагин для создания кастомных предметов для Minecraft 1.21.8+. Полная настройка через YAML-файлы без изменения кода.

## Установка

1. Скачайте последний релиз с [GitHub Releases](https://github.com/animesao/dcustomitems/releases)
2. Поместите `DC-CustomItems-x.x.x.jar` в папку `plugins/`
3. Перезапустите сервер
4. Настройте предметы в `plugins/DC-CustomItems/items/`

## Структура плагина

```
plugins/DC-CustomItems/
├── config.yml              # Основные настройки
├── messages.yml            # Сообщения плагина
└── items/                  # Папка с предметами
    ├── shadow-blade.yml
    ├── vampire-blade.yml
    ├── thunder-axe.yml
    ├── healer-amulet.yml
    ├── frost-wand.yml
    ├── berserker-axe.yml
    ├── elemental-staff.yml
    ├── shadow-helmet.yml
    ├── archer-bow.yml
    └── nature-totem.yml
```

---

## Команды

| Команда | Описание |
|---------|----------|
| `/customitems list` | Список всех предметов |
| `/customitems give <id> [игрок]` | Выдать предмет |
| `/customitems reload` | Перезагрузить конфиг |
| `/customitems update` | Проверить обновления |

---

## Настройка предметов

### Базовая структура

```yaml
my-item:
  # Основные параметры
  type: TOOL              # TOOL, RUNE, ARMOR, CONSUMABLE
  activation-slot: HAND   # HAND, OFFHAND, HEAD, CHEST, LEGS, FEET
  placeable: false        # Можно ли ставить
  permission: my.permission # Право на использование

  # Параметры предмета
  item:
    type: DIAMOND_SWORD   # Тип материала
    title: '&6Мой Меч'    # Название
    glowing: true         # Свечение
    unbreakable: true     # Неломаемость
    custom-model-data: 1001 # ID модели для ресурспака
    enchantments:
      SHARPNESS: 5
      DURABILITY: 3

  # Описание (лор)
  lore:
    - ''
    - ' &7Описание предмета'
    - ''

  # Постоянные эффекты
  effects:
    - 'INCREASE_DAMAGE:3'  # Сила III
    - 'SPEED:2'            # Скорость II

  # Атрибуты
  attributes:
    GENERIC_ATTACK_DAMAGE: 5.0
    GENERIC_ATTACK_SPEED: 1.5

  # Кулдаун кликов (мс)
  click-cooldown: 1000

  # Максимум использований (-1 = бесконечно)
  max-uses: -1

  # Сет брони
  armor-set: my-set
  has-set-bonus: true
```

---

## Действия при клике

### ПКМ (Правый клик)

```yaml
right-click-actions:
  - 'lightning:3'                    # 3 молнии
  - 'effect:INCREASE_DAMAGE:10:2'    # Сила II на 10 сек
  - 'particle:FLAME:30'              # 30 огненных частиц
  - 'sound:ENTITY_LIGHTNING:1:1'     # Звук молнии
  - 'message:&aВы активировали!'     # Сообщение
  - 'title:Заголовок::Подзаголовок'  # Заголовок
  - 'heal:10'                        # Исцелить на 10 HP
  - 'teleport:~:5:~'                 # Телепорт +5 по Y
  - 'give:DIAMOND:5'                 # Выдать 5 алмазов
  - 'exp:100:5'                      # 100 опыта + 5 уровней
```

### ЛКМ (Левый клик)

```yaml
left-click-actions:
  - 'lightning:5'
  - 'effect:SPEED:8:2'
  - 'particle:EXPLOSION_HUGE:10'
```

---

## Триггеры

### Типы триггеров

| Триггер | Описание |
|---------|----------|
| `on_kill` | При убийстве моба/игрока |
| `on_death` | При смерти игрока |
| `on_damage_taken` | При получении урона |
| `on_damage_dealt` | При нанесении урона |
| `on_jump` | При прыжке |
| `on_pickup` | При подборе предмета |
| `on_drop` | При выбрасывании предмета |

### Пример триггеров

```yaml
trigger-actions:
  # При убийстве
  - 'on_kill:heal:20'                          # Исцелить
  - 'on_kill:effect:REGENERATION:10:2'         # Эффект
  - 'on_kill:particle:HEART:15'                # Частицы
  - 'on_kill:message:&aВы убили врага!'        # Сообщение
  - 'on_kill:announce:%player% убил врага!'    # Объявление

  # При прыжке
  - 'on_jump:effect:SPEED:3:2'
  - 'on_jump:particle:CLOUD:10'

  # При ударе
  - 'on_damage_dealt:particle:CRIT:10'
  - 'on_damage_dealt:lightning:1'

  # При получении урона
  - 'on_damage_taken:effect:RESISTANCE:5:2'
  - 'on_damage_taken:particle:SMOKE:10'

  # При подборе
  - 'on_pickup:message:&aПредмет подобран!'

  # При выбрасывании
  - 'on_drop:message:&cПредмет выброшен.'
```

---

## Доступные действия

### 📢 Коммуникация

| Действие | Формат | Описание |
|----------|--------|----------|
| `message` | `message:Текст` | Сообщение игроку |
| `announce` | `announce:Текст` | Сообщение всем |
| `title` | `title:Заголовок:Подзаголовок:fadeIn:stay:fadeOut` | Заголовок |
| `actionbar` | `actionbar:Текст` | Текст над хотбаром |

### ⚡ Эффекты

| Действие | Формат | Описание |
|----------|--------|----------|
| `effect` | `effect:ТИП:СЕКУНДЫ:УРОВЕНЬ` | Эффект зелья |

**Доступные эффекты:**
- `SPEED` - Скорость
- `SLOW` - Замедление
- `INCREASE_DAMAGE` / `STRENGTH` - Сила
- `HEAL` - Исцеление
- `HASTE` - Спешка
- `JUMP` - Прыжок
- `REGENERATION` - Регенерация
- `RESISTANCE` / `DAMAGE_RESISTANCE` - Сопротивление
- `FIRE_RESISTANCE` - Огнестойкость
- `WATER_BREATHING` - Водное дыхание
- `INVISIBILITY` - Невидимость
- `NIGHT_VISION` - Ночное зрение
- `HEALTH_BOOST` - Прилив здоровья
- `ABSORPTION` - Поглощение
- `SATURATION` - Насыщение
- `LUCK` - Удача
- `GLOWING` - Сияние

### 💥 Боевые

| Действие | Формат | Описание |
|----------|--------|----------|
| `lightning` | `lightning:КОЛ` | Молния |
| `damage` | `damage:СУММА` | Урон |
| `heal` | `heal:СУММА` | Исцеление |

### 🎁 Предметы

| Действие | Формат | Описание |
|----------|--------|----------|
| `give` | `give:МАТЕРИАЛ:КОЛ` | Выдать предмет |
| `remove` | `remove:МАТЕРИАЛ:КОЛ` | Убрать предмет |
| `exp` | `exp:ОПЫТ:УРОВНИ` | Опыт |

### 🌍 Мир

| Действие | Формат | Описание |
|----------|--------|----------|
| `teleport` | `teleport:X:Y:Z` | Телепортация |
| `sethealth` | `sethealth:ЗНАЧЕНИЕ` | Здоровье |
| `setfood` | `setfood:ЗНАЧЕНИЕ` | Голод |

### 🎆 Эффекты

| Действие | Формат | Описание |
|----------|--------|----------|
| `particle` | `particle:ТИП:КОЛ` | Частицы |
| `sound` | `sound:ТИП:ГРОМКОСТЬ:ТОНАЛЬНОСТЬ` | Звук |
| `fireworks` | `fireworks:1` | Фейерверк |
| `vanish` | `vanish:СЕКУНДЫ` | Невидимость |
| `glow` | `glow:СЕКУНДЫ` | Сияние |

### 🔧 Команды

| Действие | Формат | Описание |
|----------|--------|----------|
| `command` | `command:КОМАНДА` | Команда от консоли |

---

## Экипировка

### Частицы и звуки при экипировке

```yaml
equip-particles:
  - 'FLAME:20'
  - 'ENCHANTMENT_TABLE:15'
equip-sounds:
  - 'ITEM_TRIDENT_RETURN:1:1.2'

unequip-particles:
  - 'SMOKE_NORMAL:10'
unequip-sounds:
  - 'ENTITY_ENDER_DRAGON_GROWL:0.5:1.2'
```

### Сообщения

```yaml
activation-message: '&aПредмет активирован!'
deactivation-message: '&cПредмет деактивирован.'
equip-message: '&aПредмет экипирован!'
unequip-message: '&cПредмет снят.'
cooldown-message: '&cКулдаун {seconds} сек!'
uses-depleted-message: '&cИсчерпано!'
```

---

## Атрибуты

```yaml
attributes:
  GENERIC_ATTACK_DAMAGE: 5.0      # Урон атаки
  GENERIC_ATTACK_SPEED: 1.5       # Скорость атаки
  GENERIC_MAX_HEALTH: 4.0         # Макс. здоровье
  GENERIC_MOVEMENT_SPEED: 0.1     # Скорость
  GENERIC_ARMOR: 3.0              # Броня
  GENERIC_ARMOR_TOUGHNESS: 2.0    # Прочность брони
  GENERIC_KNOCKBACK_RESISTANCE: 0.3 # Сопротивление отбросу
  GENERIC_LUCK: 1.0               # Удача
```

---

## Плейсхолдеры

### В лоре

- `%uses%` - Оставшиеся использования
- `%cooldown%` - Текущий кулдаун (сек)

### В сообщениях

- `%player%` - Имя игрока
- `{seconds}` - Секунды кулдауна
- `{uses}` - Использования

---

## Пример полного предмета

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
    custom-model-data: 1001
    enchantments:
      SHARPNESS: 5
      DURABILITY: 3

  lore:
    - ''
    - ' &6Легендарный меч'
    - ''

  effects:
    - 'INCREASE_DAMAGE:3'
    - 'SPEED:2'

  attributes:
    GENERIC_ATTACK_DAMAGE: 8.0

  click-cooldown: 1000

  equip-particles:
    - 'FLAME:20'
  equip-sounds:
    - 'ITEM_TRIDENT_RETURN:1:1.2'

  activation-message: '&6Меч извлечен!'
  deactivation-message: '&6Меч убран.'

  right-click-actions:
    - 'lightning:2'
    - 'particle:FLAME:30'
    - 'message:&6Молнии призваны!'

  trigger-actions:
    - 'on_kill:heal:20'
    - 'on_kill:message:&aИсцеление!'
    - 'on_jump:effect:SPEED:3:2'
```
