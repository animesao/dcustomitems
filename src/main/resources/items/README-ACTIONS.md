# DC-CustomItems - Полная документация по действиям и триггерам

## 📋 Новый формат YAML (v1.320.225+)

Используйте вложенный `triggers` вместо старого `trigger-actions`:

```yaml
# СТАРЫЙ ФОРМАТ (устарел):
trigger-actions:
  - 'on_click_right:effect:SPEED:5:1'
  - 'on_click_right:lightning:1'

# НОВЫЙ ФОРМАТ (рекомендуется):
triggers:
  on_click_right:
    - 'effect:SPEED:5:1'
    - 'lightning:1'
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
| `on_pickup` | При подборе предмета |
| `on_drop` | При выбрасывании предмета |

---

## Доступные действия

### 📢 Коммуникация

| Действие | Формат | Описание |
|----------|--------|----------|
| `message` | `message:Текст` | Сообщение игроку |
| `title` | `title:Заголовок:Подзаголовок:fadeIn:stay:fadeOut` | Заголовок на экране |
| `actionbar` | `actionbar:Текст` | Текст над хотбаром |
| `broadcast` | `broadcast:Текст` | Объявление всем игрокам |

**Плейсхолдеры:** `%player%` - Имя игрока

**Примеры:**
```yaml
- 'message:&aВы получили алмаз!'
- 'title:&6ПОБЕДА!::Вы получили силу!:10:40:10'
- 'actionbar:&eУ вас осталось 5 использований'
- 'broadcast:&6%player% получил легендарный меч!'
```

---

### ⚡ Эффекты

| Действие | Формат | Описание |
|----------|--------|----------|
| `effect` | `effect:ТИП:СЕКУНДЫ:УРОВЕНЬ` | Эффект зелья |
| `speed` | `speed:СЕКУНДЫ:УРОВЕНЬ` | Скорость (короткий) |
| `vanish` | `vanish:СЕКУНДЫ` | Невидимость |
| `glow` | `glow:СЕКУНДЫ` | Сияние |
| `flight` | `flight:true/false` | Полёт |

**Доступные эффекты:**
- `SPEED` / `SLOWNESS` / `HASTE` / `MINING_FATIGUE`
- `STRENGTH` / `INCREASE_DAMAGE` / `JUMP_BOOST`
- `REGENERATION` / `RESISTANCE` / `DAMAGE_RESISTANCE`
- `FIRE_RESISTANCE` / `WATER_BREATHING` / `INVISIBILITY`
- `NIGHT_VISION` / `HEALTH_BOOST` / `ABSORPTION`
- `SATURATION` / `GLOWING` / `LEVITATION`
- `SLOW_FALLING` / `CONDUIT_POWER` / `DOLPHINS_GRACE`
- `BAD_OMEN` / `HERO_OF_THE_VILLAGE` / `DARKNESS`

**Примеры:**
```yaml
- 'effect:SPEED:10:2'          # Скорость II на 10 секунд
- 'effect:STRENGTH:5:3'        # Сила III на 5 секунд
- 'effect:REGENERATION:15:1'   # Регенерация I на 15 секунд
- 'speed:30:2'                 # Скорость II на 30 сек (короткий)
- 'vanish:10'                  # Невидимость на 10 сек
- 'glow:5'                     # Сияние на 5 сек
- 'flight:true'                # Включить полёт
```

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
| `lightning` | `lightning:КОЛИЧЕСТВО` | Молния на месте игрока |
| `lightning_forward` | `lightning_forward:ДАЛЬНОСТЬ` | Молния далеко вперед |
| `knockback` | `knockback:РАДИУС` | Отбросить nearby |
| `launch` | `launch:СИЛА` | Подбросить nearby |
| `stun` | `stun:СЕКУНДЫ:РАДИУС` | Оглушить nearby |

**Примеры:**
```yaml
- 'heal:10'              # Исцелить на 10 HP
- 'heal_nearby:10:5'     # Исцелить игроков в радиусе 5 блоков на 10 HP
- 'damage:6'             # Урон 6 HP
- 'damage_nearby:20:4'   # AoE урон 20 в радиусе 4 блоков
- 'damage_mobs:10:3'     # Урон мобам 10 в радиусе 3 блоков
- 'lightning_forward:100' # Молния на 100 блоков вперед
- 'knockback:4'          # Отбросить в радиусе 4 блоков
- 'stun:3:5'             # Оглушить на 3 сек в радиусе 5 блоков
```

---

### 🌍 Телепортация

| Действие | Формат | Описание |
|----------|--------|----------|
| `teleport` | `teleport:X:Y:Z` | Телепорт в точку |
| `teleport_relative` | `teleport_relative:X:Y:Z` | Относительный телепорт |

**Примеры:**
```yaml
- 'teleport:100:64:200'       # В точку 100 64 200
- 'teleport_relative:~:5:~'   # На 5 блоков вверх
```

---

### 🎁 Предметы

| Действие | Формат | Описание |
|----------|--------|----------|
| `give` | `give:МАТЕРИАЛ:КОЛИЧЕСТВО` | Выдать предмет |
| `remove` | `remove:МАТЕРИАЛ:КОЛИЧЕСТВО` | Забрать предмет |
| `exp` | `exp:ОПЫТ:УРОВНИ` | Выдать опыт |

**Примеры:**
```yaml
- 'give:DIAMOND:5'       # 5 алмазов
- 'give:GOLDEN_APPLE:1'  # 1 золотое яблоко
- 'remove:DIAMOND:1'     # Забрать 1 алмаз
- 'exp:500:5'            # 500 опыта + 5 уровней
```

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

**Примеры:**
```yaml
- 'particles:FLAME:50'                    # 50 огненных частиц
- 'particles:HEART:20'                    # 20 сердец
- 'sound:ENTITY_PLAYER_LEVELUP:1:1.5'    # Звук повышения уровня
```

---

### 🔧 Команды

| Действие | Формат | Описание |
|----------|--------|----------|
| `command` | `command:КОМАНДА` | Команда от имени игрока |
| `console_command` | `console_command:КОМАНДА` | Команда от консоли |

**Примеры:**
```yaml
- 'command:tp %player% 100 64 200'
- 'command:effect give %player% speed 60 2'
```

---

## Полный пример (новый формат)

```yaml
legendary-fire-sword:
  material: NETHERITE_SWORD
  display-name: '&c&l🔥 Огненный Клинок'
  unbreakable: true
  glowing: true
  model: "fire_sword"
  
  enchantments:
    SHARPNESS: 5
    FIRE_ASPECT: 2
    UNBREAKING: 3
    
  attributes:
    GENERIC_ATTACK_DAMAGE: 15.0
    GENERIC_ATTACK_SPEED: 1.8
    
  effects:
    - 'FIRE_RESISTANCE:999:1'
    - 'STRENGTH:10:1'
    
  cooldown: 1000
  
  triggers:
    on_equip:
      - 'particles:FLAME:30'
      - 'sound:ENTITY_BLAZE_AMBIENT'
      - 'message:&c🔥 Огненный клинок извлечен!'
      
    on_unequip:
      - 'particles:SMOKE:20'
      - 'sound:BLOCK_FIRE_EXTINGUISH'
      
    on_click_right:
      - 'damage_nearby:15:4'
      - 'particles:FLAME:100'
      - 'sound:ENTITY_GENERIC_EXPLODE'
      - 'effect:STRENGTH:10:2'
      - 'title:&c🔥 ОГНЕННЫЙ ШТОРМ!::&7Все враги горят!'
      
    on_click_left:
      - 'damage_mobs:10:3'
      - 'particles:CRIT:50'
      
    on_kill:
      - 'heal:20'
      - 'effect:REGENERATION:10:2'
      - 'particles:HEART:30'
      - 'sound:ENTITY_PLAYER_LEVELUP'
```
