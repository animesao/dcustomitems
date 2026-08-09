# 🎨 Примеры моделей для DC-CustomItems API

## 📁 Структура ресурс-пака

```
resource-pack/
├── pack.mcmeta
└── assets/
    └── minecraft/
        ├── models/
        │   └── item/
        │       ├── dark_sword.json      ← Модель тёмного клинка
        │       ├── fire_sword.json      ← Модель огненного клинка
        │       ├── teleport_staff.json  ← Модель посоха телепорта
        │       └── dark_helmet.json     ← Модель тёмного шлема
        └── textures/
            └── item/
                ├── dark_sword.png       ← Текстура (создай сам!)
                ├── fire_sword.png
                ├── teleport_staff.png
                └── dark_helmet.png
```

---

## 🎯 Как подключить модель к Java предмету

### 1. Создай модель JSON (`models/item/dark_sword.json`):
```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "0": "minecraft:item/dark_sword",
    "particle": "minecraft:item/dark_sword"
  }
}
```

### 2. Укажи модель в Java коде:
```java
public class DarkSword extends AbstractCustomItem {
    
    // ... другие методы ...
    
    // 🎨 КАСТОМНАЯ МОДЕЛЬ
    @Override
    public String getItemModel() { return "dark_sword"; }
    //      ↑
    //      Имя модели БЕЗ расширения .json
}
```

### 3. Положи текстуру (`textures/item/dark_sword.png`):
- Размер: 16x16 пикселей (рекомендуется)
- Формат: PNG с прозрачностью

### 4. Заресурс-пак и перезагрузи:
```
/ci reload
/api-item give dark_sword
```

---

## 📋 Готовые модели

### 🗡 dark_sword - Тёмный Клинок
- Parent: `handheld` (для мечей/кирк)
- Текстура: `dark_sword.png`
- Использование: `getItemModel() → "dark_sword"`

### 🔥 fire_sword - Огненный Клинок
- Parent: `handheld`
- Текстура: `fire_sword.png`
- Использование: `getItemModel() → "fire_sword"`

### 🌀 teleport_staff - Посох Телепорта
- Parent: `handheld`
- Текстура: `teleport_staff.png`
- Использование: `getItemModel() → "teleport_staff"`

### ⛑ dark_helmet - Тёмный Шлем
- Parent: `generated` (для брони/предметов)
- Текстура: `dark_helmet.png`
- Использование: `getItemModel() → "dark_helmet"`

---

## 🔧 Типы моделей

### Для оружия/инструментов (parent: handheld):
```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "0": "minecraft:item/my_item",
    "particle": "minecraft:item/my_item"
  }
}
```

### Для брони/предметов (parent: generated):
```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "0": "minecraft:item/my_item",
    "particle": "minecraft:item/my_item"
  }
}
```

### Для головы (parent: player_head):
```json
{
  "parent": "minecraft:player_head",
  "textures": {
    "0": "minecraft:entity/heads/my_head"
  }
}
```

---

## 💡 Советы

1. **Имя модели** должно совпадать с тем что в `getItemModel()`
2. **Текстура** должна лежать в `textures/item/`
3. **Размер текстуры** - 16x16 или 32x32 (для HD)
4. **Прозрачность** - PNG поддерживает альфа-канал
5. **Формат pack.mcmeta** - используй `pack_format: 34` для 1.21+

---

## 🎮 Команды

| Команда | Описание |
|---------|----------|
| `/ci reload` | Перезагрузить все предметы |
| `/api-item give <id>` | Выдать предмет |
| `/api-item info <id>` | Информация о предмете |
| `/api-item list` | Список всех предметов |

---

## 📥 Установка ресурс-пака

1. Упакуй папку `resource-pack/` в ZIP
2. Переименуй `.zip` → `.zip` (без изменений)
3. Положи в папку сервера
4. Укажи в `server.properties`:
   ```
   resource-pack=https://ссылка/на/resource-pack.zip
   resource-pack-sha1=хеш
   ```
