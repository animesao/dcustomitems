# DC-CustomItems Документация Плагина

## 📚 Оглавление

- [Обзор](#обзор)
- [Возможности](#возможности)
- [Требования](#требования)
- [Установка](#установка)
- [Быстрый Старт](#быстрый-старт)
- [Конфигурация](#конфигурация)
- [Команды](#команды)
- [Права](#права)
- [Ресурс-Пак](#ресурс-пак)
- [Java API](#java-api)
- [Решение Проблем](#решение-проблем)
- [Поддержка](#поддержка)

---

## 📖 Обзор

DC-CustomItems — это мощный плагин для серверов Minecraft (Spigot/Paper 1.20+), который позволяет создавать кастомные предметы с уникальными способностями, эффектами и механиками.

### Что Можно Создать:

- ⚔️ **Кастомные Оружия** — Мечи, топоры, луки со.special способностями
- 🛡️ **Кастомная Броня** — Шлемы, нагрудники с уникальными эффектами
- 🧪 **Зелья и Расходники** — Кастомные зелья, еда, свитки
- 🔮 **Особые Предметы** — Тотемы, палочки, инструменты
- 🎯 **Любые Предметы** — Полная кастомизация без ограничений

---

## ✨ Возможности

### Основные Функции:

| Функция | Описание |
|---------|----------|
| **YAML Конфигурация** | Создавайте предметы через YAML файлы |
| **Java API** | Продвинутые предметы на Java |
| **Горячая Перезагрузка** | Обновляйте предметы без перезапуска |
| **Кастомные Модели** | Поддержка моделей из ресурс-пака |
| **Система Эффектов** | Частицы, звуки, зелья, заголовки |
| **Кулдауны** | Настраиваемые кулдауны для способностей |
| **Права** | Система прав для каждого предмета |
| **Мульти-Триггер** | Несколько действий на одно событие |

### Поддерживаемые События:

- Левый Клик (ЛКМ)
- Правый Клик (ПКМ)
- Экипировка/Снятие
- Ломание Блоков
- Получение Урона
- Смерть Игрока
- И многое другое...

---

## 📋 Требования

| Требование | Версия |
|------------|--------|
| Minecraft Сервер | 1.20+ (протестировано на 1.21.11) |
| Серверное ПО | Spigot, Paper или совместимое |
| Java | 17 или выше |
| Загрузчик Плагинов | Bukkit/Spigot |

---

## 🚀 Установка

### Шаг 1: Скачать

Скачайте последнюю версию с GitHub:
```bash
curl -L -o dcustomitems.jar https://github.com/animesao/dcustomitems/releases/latest/download/DC-CustomItems.jar
```

### Шаг 2: Установить

Поместите JAR файл в папку `plugins/` вашего сервера:
```
server/
└── plugins/
    └── dcustomitems.jar
```

### Шаг 3: Перезапустить Сервер

Перезапустите сервер или выполните `/ci reload`.

### Шаг 4: Проверить

Проверьте загрузку плагина:
```
/pl
```

Вы должны увидеть `DC-CustomItems` в списке.

---

## 🎮 Быстрый Старт

### Создание Первого Предмета

1. **Перейдите в папку предметов:**
```
plugins/DC-CustomItems/items/
```

2. **Создайте новый файл:** `my-sword.yml`

3. **Добавьте конфигурацию:**
```yaml
my_sword:
  item:
    type: DIAMOND_SWORD
    title: '&6Мой Первый Меч'
    glowing: true
  lore:
    - ''
    - '&7Мощный кастомный меч'
    - ''
  type: TOOL
  activation-slot: HAND
  trigger-actions:
    - 'on_click_right:effect:SPEED:10:1'
    - 'on_click_right:particle:FLAME:50'
    - 'on_click_right:message:&6Меч активирован!'
```

4. **Перезагрузите плагин:**
```
/ci reload
```

5. **Получите предмет:**
```
/ci give my_sword
```

### Готово! Вы создали свой первый кастомный предмет!

---

## ⚙️ Конфигурация

### Структура Конфигурации Предмета

```yaml
item_id:                          # Уникальный идентификатор
  item:
    type: MATERIAL                # Материал Minecraft
    title: '&6Название'          # Название предмета (поддерживает & цвета)
    glowing: true                 # Свечение чар
    unbreakable: true             # Неломаемость
    custom-model-data: 1001       # Кастомные данные модели
    item-model: "model_name"     # Модель из ресурс-пака (1.21+)
    enchantments:
      SHARPNESS: 5               # Чары
      UNBREAKING: 3
  lore:                           # Описание предмета
    - ''
    - '&7Строка 1'
    - '&eСтрока 2'
    - ''
  type: TOOL                      # Тип предмета (TOOL, ARMOR, RUNE и т.д.)
  activation-slot: HAND           # Где предмет активируется
  placeable: false                # Можно ли разместить
  click-cooldown: 500             # Кулдаун в миллисекундах
  permission: "myplugin.use"      # Требуемое право
  trigger-actions:                # Действия для выполнения
    - 'on_event:action'
```

---

## 📝 Команды

### Основные Команды

| Команда | Описание | Право |
|---------|----------|-------|
| `/ci give <предмет> [игрок]` | Выдать кастомный предмет | `customitems.give` |
| `/ci list` | Список всех предметов | `customitems.list` |
| `/ci reload` | Перезагрузить плагин | `customitems.reload` |
| `/api-item give <id> [игрок]` | Выдать Java API предмет | `customitems.give` |
| `/api-item list` | Список Java API предметов | `customitems.list` |

### Примеры Команд

```bash
# Выдать предмет себе
/ci give my_sword

# Выдать предмет другому игроку
/ci give my_sword Steve

# Список предметов
/ci list

# Перезагрузка плагина
/ci reload
```

---

## 🔐 Права

### Права по Умолчанию

| Право | Описание | По умолчанию |
|-------|----------|--------------|
| `customitems.give` | Выдавать предметы | op |
| `customitems.list` | Список предметов | true |
| `customitems.reload` | Перезагрузка | op |
| `customitems.admin` | Админ права | op |

### Права для Предметов

Добавьте кастомные права для ограничения использования:

```yaml
legendary_sword:
  permission: "myplugin.legendary"
  # Только игроки с этим правом могут использовать
```

---

## 🎨 Ресурс-Пак

### Базовая Настройка

1. **Создайте структуру папок:**
```
resource-pack/
├── pack.mcmeta
└── assets/
    └── minecraft/
        ├── models/
        │   └── item/
        │       └── my_model.json
        └── textures/
            └── item/
                └── my_model.png
```

2. **pack.mcmeta:**
```json
{
  "pack": {
    "description": "Кастомные Предметы",
    "pack_format": 34
  }
}
```

3. **Файл модели (my_model.json):**
```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "0": "minecraft:item/my_model"
  }
}
```

4. **Использование в конфиге:**
```yaml
my_item:
  item:
    item-model: "my_model"
```

---

## ☕ Java API

### Базовая Структура

```java
import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.api.ItemAPI;

public class MySword extends AbstractCustomItem {

    @Override
    public String getId() { return "my_sword"; }

    @Override
    public String getDisplayName() { return "&6Мой Меч"; }

    @Override
    public Material getMaterial() { return Material.DIAMOND_SWORD; }

    @Override
    public void onRightClick(PlayerInteractEvent e, Player p) {
        ItemAPI.heal(p, 5);
        ItemAPI.effect(p, PotionEffectType.SPEED, 10, 1);
    }
}
```

### Доступные Методы

| Метод | Описание |
|-------|----------|
| `onLeftClick()` | Действие при ЛКМ |
| `onRightClick()` | Действие при ПКМ |
| `onEquip()` | При экипировке |
| `onUnequip()` | При снятии |
| `onDamageDealt()` | При нанесении урона |
| `onDamageTaken()` | При получении урона |
| `onKill()` | При убийстве |
| `onDeath()` | При смерти |
| `onBlockBreak()` | При ломании блоков |

### Утилиты ItemAPI

| Метод | Описание |
|-------|----------|
| `ItemAPI.heal(player, amount)` | Лечение игрока |
| `ItemAPI.effect(player, type, sec, lvl)` | Эффект зелья |
| `ItemAPI.particles(player, particle, count)` | Частицы |
| `ItemAPI.sound(player, sound, vol, pitch)` | Звук |
| `ItemAPI.teleport(player, x, y, z)` | Телепортация |
| `ItemAPI.title(player, title, sub)` | Заголовок |

---

## 🔧 Решение Проблем

### Частые Проблемы

#### Предмет Не Появляется

**Проблема:** Предмет не появляется после `/ci give`

**Решение:**
1. Проверьте ID предмета: `/ci list`
2. Перезагрузите плагин: `/ci reload`
3. Проверьте консоль на ошибки

#### Эффекты Не Работают

**Проблема:** Частицы/эффекты не отображаются

**Решение:**
1. Проверьте название частицы
2. Убедитесь что у игрока есть права
3. Проверьте консоль

#### Плагин Не Загружается

**Проблема:** Плагин не появляется в `/pl`

**Решение:**
1. Проверьте версию Java (17+)
2. Проверьте наличие plugin.yml
3. Смотрите ошибки в консоли

---

## 📞 Поддержка

### Получить Помощь

1. **Читайте Документацию** — Изучите нужные разделы
2. **Ищите Проблемы** — Поищите похожие проблемы
3. **Создайте Issue** — Сообщите о багах на GitHub
4. **Сообщество** — Присоединяйтесь к Discord

### Полезные Ссылки

- **GitHub:** https://github.com/animesao/dcustomitems
- **Релизы:** https://github.com/animesao/dcustomitems/releases
- **Issues:** https://github.com/animesao/dcustomitems/issues

---

## 📄 Лицензия

Плагин является открытым под лицензией MIT.

---

**Версия:** 1.320.237  
**Последнее обновление:** Август 2026
