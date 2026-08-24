# DC-CustomItems - Полная документация по действиям и триггерам

## 📋 Формат YAML (v1.320.225+)

```yaml
item-id:
  triggers:
    on_click_right:
      - 'action1:param1:param2'
      - 'action2:param1:param2'
```

---

## Доступные триггеры

| Триггер | Описание |
|---------|----------|
| `on_equip` | При экипировке предмета |
| `on_unequip` | При снятии предмета |
| `on_click_right` | ПКМ с предметом |
| `on_click_left` | ЛКМ с предметом |
| `on_kill` | При убийстве моба/игрока |
| `on_death` | При смерти игрока |
| `on_damage_taken` | При получении урона |
| `on_damage_dealt` | При нанесении урона |
| `on_jump` | При прыжке |
| `on_sprint` | При начале бега |
| `on_pickup` | При подборе предмета |
| `on_drop` | При выбрасывании предмета |

---

## Все доступные действия

### 📢 Коммуникация

| Действие | Формат | Описание |
|----------|--------|----------|
| `message` | `message:Текст` | Сообщение игроку |
| `title` | `title:Заголовок:Подзаголовок:IN:STAY:OUT` | Заголовок на экране |
| `actionbar` | `actionbar:Текст` | Текст над хотбаром |
| `broadcast` | `broadcast:Текст` | Объявление всем игрокам |

---

### ⚡ Эффекты

| Действие | Формат | Описание |
|----------|--------|----------|
| `effect` | `effect:ТИП:СЕКУНДЫ:УРОВЕНЬ` | Эффект зелья |
| `speed` | `speed:СЕКУНДЫ:УРОВЕНЬ` | Скорость (короткий) |
| `vanish` | `vanish:СЕКУНДЫ` | Невидимость |
| `glow` | `glow:СЕКУНДЫ` | Сияние |
| `flight` | `flight:true/false` | Полёт |

---

### 💥 Боевые действия

| Действие | Формат | Описание |
|----------|--------|----------|
| `heal` | `heal:СУММА` | Исцелить себя |
| `heal_nearby` | `heal_nearby:СУММА:РАДИУС` | Исцелить ближайших игроков |
| `damage` | `damage:СУММА` | Урон себе |
| `damage_nearby` | `damage_nearby:УРОН:РАДИУС` | AoE урон |
| `damage_mobs` | `damage_mobs:УРОН:РАДИУС` | Урон только мобам |
| `damage_players` | `damage_players:УРОН:РАДИУС` | Урон только игрокам |
| `lightning` | `lightning:КОЛИЧЕСТВО` | Молния на месте |
| `lightning_forward` | `lightning_forward:ДАЛЬНОСТЬ` | Молния далеко вперед |
| `knockback` | `knockback:РАДИУС` | Отбросить nearby |
| `launch` | `launch:СИЛА` | Подбросить nearby |
| `stun` | `stun:СЕКУНДЫ:РАДИУС` | Оглушить nearby |

---

### 🌍 Телепортация

| Действие | Формат | Описание |
|----------|--------|----------|
| `teleport` | `teleport:X:Y:Z` | Телепорт в точку |
| `teleport_relative` | `teleport_relative:X:Y:Z` | Относительный телепорт |

---

### 🎁 Предметы

| Действие | Формат | Описание |
|----------|--------|----------|
| `give` | `give:МАТЕРИАЛ:КОЛИЧЕСТВО` | Выдать предмет |
| `remove` | `remove:МАТЕРИАЛ:КОЛИЧЕСТВО` | Забрать предмет |
| `exp` | `exp:ОПЫТ:УРОВНИ` | Выдать опыт |

---

### 🌎 Мир

| Действие | Формат | Описание |
|----------|--------|----------|
| `sethealth` | `sethealth:ЗНАЧЕНИЕ` | Установить здоровье |
| `setfood` | `setfood:ЗНАЧЕНИЕ` | Установить голод |

---

### 🎆 Эффекты

| Действие | Формат | Описание |
|----------|--------|----------|
| `particles` | `particles:ТИП:КОЛИЧЕСТВО` | Частицы |
| `sound` | `sound:ТИП:ГРОМКОСТЬ:ТОНАЛЬНОСТЬ` | Звук |
| `fireworks` | `fireworks:1` | Фейерверк |

---

### 🔧 Команды

| Действие | Формат | Описание |
|----------|--------|----------|
| `command` | `command:КОМАНДА` | Команда от имени игрока |
| `console_command` | `console_command:КОМАНДА` | Команда от консоли |

---

## 🚀 Продвинутые действия (БЕЗ ОГРАНИЧЕНИЙ!)

### 🎨 Кастомные частицы

```yaml
# Формат: particles_custom:ТИП:КОЛИЧЕСТВО:X:Y:Z:OFF_X:OFF_Y:OFF_Z
- 'particles_custom:FLAME:100:0:1:0:2:2:2'
```

**Примеры:**
```yaml
- 'particles_custom:FLAME:200:0:1:0:3:1:3'           # Огненное кольцо
- 'particles_custom:DRAGON_BREATH:100:0:2:0:2:2:2'    # Дыхание дракона
- 'particles_custom:PORTAL:150:0:1:0:2:2:2'           # Портальные частицы
- 'particles_custom:HEART:50:0:2:0:1:1:1'             # Сердечки
- 'particles_custom:EXPLOSION_HUGE:10:0:1:0:2:2:2'    # Взрыв
```

---

### 🔊 Последовательность звуков

```yaml
# Формат: sound_sequence:ЗВУК1:ГРОМКОСТЬ1:ТОН1;ЗВУК2:ГРОМКОСТЬ2:ТОН2;...
- 'sound_sequence:ENTITY_PLAYER_LEVELUP:1:1;ENTITY_ORB_PICKUP:0.5:1.5'
```

**Примеры:**
```yaml
- 'sound_sequence:ENTITY_BLAZE_SHOOT:1:1;ENTITY_GENERIC_EXPLODE:1:0.8'
- 'sound_sequence:ENTITY_ENDERMAN_TELEPORT:1:1;ENTITY_PLAYER_LEVELUP:0.5:1.5'
```

---

### 🎬 Последовательность заголовков (анимация!)

```yaml
# Формат: title_sequence:ЗАГОЛОВОК:ПОД:IN:STAY:OUT;...
- 'title_sequence:🔥 ПОДГОТОВКА!:::10:20:10;🔥 ОГОНЬ!:::10:30:10;🔥 ШТОРМ!:::10:40:10'
```

**Примеры:**
```yaml
# Анимация перед ударом
- 'title_sequence:⚡ 3!:::5:10:5;⚡ 2!:::5:10:5;⚡ 1!:::5:10:5;💥 УДАР!:::10:40:10'

# Фазы боя
- 'title_sequence:🔥 ФАЗА 1!:::10:30:10;🔥 ФАЗА 2!:::10:30:10;🔥 ФАЗА 3!:::10:40:10'
```

---

### 📜 Последовательность команд с задержками

```yaml
# Формат: command_sequence:КОМАНДА1:ЗАДЕРЖКА1;КОМАНДА2:ЗАДЕРЖКА2;...
- 'command_sequence:give %player% diamond 1:20;say Привет!:40;give %player% emerald 1:60'
```

**Примеры:**
```yaml
# Награда поэтапно
- 'command_sequence:give %player% diamond 5:100;give %player% emerald 3:200;give %player% gold 10:300'
```

---

### 🌀 Последовательность телепортаций

```yaml
# Формат: teleport_sequence:X1:Y1:Z1:ЗАДЕРЖКА1;X2:Y2:Z2:ЗАДЕРЖКА2;...
- 'teleport_sequence:100:64:200:200;200:64:300:200;300:64:400'
```

**Примеры:**
```yaml
# Телепорт-серия
- 'teleport_sequence:100:64:200:100;200:64:300:100;300:64:400:100;400:64:500'
```

---

### 💫 Последовательность эффектов

```yaml
# Формат: effect_sequence:ТИП1:СЕКУНДЫ1:УРОВЕНЬ1:ЗАДЕРЖКА1;...
- 'effect_sequence:SPEED:10:2:0;STRENGTH:10:3:1000;REGENERATION:10:2:2000'
```

**Примеры:**
```yaml
# Наращивание силы
- 'effect_sequence:SPEED:10:1:0;SPEED:10:2:2000;SPEED:10:3:4000;SPEED:10:4:6000'
```

---

### 💥 Кастомный урон (точные размеры)

```yaml
# Формат: damage_custom:УРОН:ШИРИНА:ВЫСОТА:ГЛУБИНА
- 'damage_custom:20:5:3:5'  # 20 урона в области 5x3x5 блоков
```

**Примеры:**
```yaml
- 'damage_custom:30:6:4:6'  # Мощный AoE удар
- 'damage_custom:10:3:2:3'  # Маленький AoE удар
- 'damage_custom:50:8:6:8'  # Огромный AoE удар
```

---

### ❤️ Кастомное исцеление (с частицами)

```yaml
# Формат: heal_custom:ИСЦЕЛЕНИЕ:ШИРИНА:ВЫСОТА:ГЛУБИНА:ЧАСТИЦЫ
- 'heal_custom:10:5:5:5:HEART'  # Лечит 10 HP в области 5x5x5 с сердечками
```

**Примеры:**
```yaml
- 'heal_custom:15:8:5:8:HEART'      # Лечение союзников
- 'heal_custom:20:10:8:10:VILLAGER_HAPPY'  # Массовое лечение
```

---

## 🎮 Полный пример (все действия)

```yaml
ultimate-fire-sword:
  type: TOOL
  activation-slot: HAND
  
  item:
    type: NETHERITE_SWORD
    title: '&c🔥 Огненный Клинок'
    glowing: true
    unbreakable: true
    
  effects:
    - 'INCREASE_DAMAGE:5'
    - 'SPEED:3'
    - 'REGENERATION:3'
    
  triggers:
    on_equip:
      - 'particles_custom:FLAME:100:0:1:0:2:2:2'
      - 'sound_sequence:ENTITY_BLAZE_AMBIENT:1:1;ENTITY_PLAYER_LEVELUP:0.5:2'
      - 'title:&c🔥 КЛИНОК ИЗВЛЕЧЁН!::&7Стихия огня!:10:40:10'
      
    on_click_right:
      - 'title_sequence:🔥 ПОДГОТОВКА!:::10:20:10;🔥 ОГОНЬ!:::10:30:10;🔥 ШТОРМ!:::10:40:10'
      - 'particles_custom:FLAME:200:0:1:0:3:1:3'
      - 'damage_custom:30:6:4:6'
      - 'sound_sequence:ENTITY_BLAZE_SHOOT:1:1;ENTITY_GENERIC_EXPLODE:1:0.8'
      - 'effect_sequence:STRENGTH:15:5:0;SPEED:15:3:500'
      
    on_kill:
      - 'heal:50'
      - 'effect_sequence:REGENERATION:20:4:0;STRENGTH:20:5:1000'
      - 'particles_custom:HEART:100:0:2:0:2:2:2'
      - 'sound:ENTITY_PLAYER_LEVELUP:1:2'
      - 'fireworks:1'
```

---

## 🌍 Ограничения по мирам

Предмет может работать только в определённых мирах или быть запрещён в некоторых.

```yaml
my-item:
  # Только в этих мирах (пусто = все миры)
  allowed-worlds:
    - world
    - world_nether

  # Запрещён в этих мирах
  disabled-worlds:
    - world_the_end
```

**Приоритет:** `allowed-worlds` проверяется первым. Если оба списка пусты — предмет работает везде.

---

## ⏱️ Таймер предмета (auto-destroy)

Предмет автоматически удаляется через указанное время (в секундах).

```yaml
my-item:
  # Время жизни в секундах (-1 = бессрочно)
  duration: 30

  # Сообщение при истечении времени
  max-duration-message: '&cПредмет испарился!'
```

**Примеры:**
```yaml
# Граната — исчезает через 10 секунд
bomb:
  type: TOOL
  duration: 10
  max-duration-message: '&c💣 Граната взорвалась!'
  right-click-actions:
    - 'damage_nearby:30:5'
    - 'particles_custom:EXPLOSION_HUGE:50:0:1:0:3:3:3'
    - 'sound:ENTITY_GENERIC_EXPLODE:1:1'
```

---

## ✨ Частицы-следы (trail particles)

Предмет оставляет частицы при движении игрока.

```yaml
my-item:
  # Частицы-следы (формат как в equip-particles)
  trail-particles:
    - 'FLAME:5'
    - 'SMOKE:3'

  # Интервал спавна частиц (в тиках, 20 тиков = 1 секунда)
  trail-particle-interval: 3
```

**Примеры:**
```yaml
# Огненный клинок — огненный след
fire-sword:
  type: TOOL
  trail-particles:
    - 'FLAME:8'
    - 'LAVA:3'
  trail-particle-interval: 2

# Ледяной клинок — ледяной след
ice-sword:
  type: TOOL
  trail-particles:
    - 'SNOWFLAKE:5'
    - 'CLOUD:3'
  trail-particle-interval: 3

# Теневой клинок — теневой след
shadow-sword:
  type: TOOL
  trail-particles:
    - 'SMOKE:4'
    - 'PORTAL:6'
  trail-particle-interval: 2
```

---

## 🎨 Анимированные модели (item-model-variant)

Для использования анимированных текстур в Resource Pack.

```yaml
my-item:
  item:
    type: NETHERITE_SWORD
    # Вариант модели (для анимированных текстур)
    # Должен совпадать с именем в item_model_definition
    item-model-variant: "animated_sword"
```

**Пример Resource Pack структуры:**
```
assets/minecraft/
├── textures/
│   └── item/
│       └── animated_sword.png          # Спрайтшит анимации
├── items/
│   └── netherite_sword.json            # Определение модели
└── materials/
    └── animated_sword.png.mcmeta       # Метаданные анимации
```

**animated_sword.png.mcmeta:**
```json
{
  "animation": {
    "interpolate": true,
    "frametime": 2,
    "frames": [
      0, 1, 2, 3, 4, 5, 6, 7
    ]
  }
}
```

**netherite_sword.json:**
```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "layer0": "minecraft:item/animated_sword"
  },
  "overrides": [
    {"predicate": {"custom_model_data": 100}, "model": "minecraft:item/animated_sword_frame1"}
  ]
}
```

---

## 🔧 Полный пример с новыми фичами

```yaml
ultimate-flame-sword:
  type: TOOL
  activation-slot: HAND
  placeable: false
  permission: flame.sword

  # Таймер: живёт 60 секунд
  duration: 60
  max-duration-message: '&c🔥 Клинок расплавился!'

  # Ограничения по миру
  allowed-worlds:
    - world
    - world_nether
  disabled-worlds:
    - world_the_end

  # Частицы-следы
  trail-particles:
    - 'FLAME:8'
    - 'LAVA:3'
  trail-particle-interval: 2

  item:
    type: NETHERITE_SWORD
    title: '&c🔥 Пламенный Клинок'
    glowing: true
    unbreakable: true
    item-model: "flame_sword"
    item-model-variant: "flame_sword_animated"
    enchantments:
      SHARPNESS: 5
      FIRE_ASPECT: 2

  effects:
    - 'INCREASE_DAMAGE:3'
    - 'FIRE_RESISTANCE:1'

  click-cooldown: 2000

  triggers:
    on_equip:
      - 'particles_custom:FLAME:50:0:1:0:2:2:2'
      - 'sound:ENTITY_BLAZE_AMBIENT:1:1'
      - 'title:&c🔥 Пламенный Клинок извлечён!|||10:40:10'

    on_sprint:
      - 'trail-particles:FLAME:3'
      - 'effect:SPEED:3:1'

    on_kill:
      - 'heal:20'
      - 'effect:REGENERATION:10:2'
      - 'particles_custom:HEART:30:0:2:0:1:1:1'
      - 'sound:ENTITY_PLAYER_LEVELUP:1:2'

    on_damage_dealt:
      - 'particles_custom:CRIT:15:0:1:0:1:1:1'
      - 'sound:ENTITY_BLAZE_SHOOT:0.5:2'
```
