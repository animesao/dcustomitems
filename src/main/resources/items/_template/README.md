# 📦 Module Template

Шаблон для создания модулей DC-CustomItems.

## 🚀 Быстрый старт

### 1. Скопируйте шаблон

```bash
cd plugins/DC-CustomItems/items/
cp -r _template/ my-module/
```

### 2. Переименуйте файл

```bash
cd my-module/
mv template.java my-module.java
```

### 3. Замените имена в файле

Откройте `my-module.java` и замените:

| Было | Стало |
|------|-------|
| `templateModule` | `myModule` |
| `"template"` | `"my-module"` |
| `"Template"` | `"My Module"` |
| `mymodule_` | `mymodule_` (в плейсхолдерах) |

### 4. Настройте config.yml

```yaml
name: "My Module"
version: "1.0"
enabled: true
description: "Мой первый модуль"
author: "YourName"

commands:
  - mycommand

permissions:
  - mymodule.use
  - mymodule.admin
```

### 5. Добавьте команду в plugin.yml (опционально)

```yaml
commands:
  mycommand:
    description: My module command
    usage: /mycommand <args>
    permission: mymodule.use
```

### 6. Перезагрузите

```bash
/ci reload
```

## 📁 Структура модуля

```
items/my-module/
├── config.yml        — Настройки
├── my-module.java    — Основной класс
├── items.yml         — Предметы (опционально)
└── README.md         — Документация (опционально)
```

## 🔧 Доступные хуки

### Основные

| Метод | Описание |
|-------|----------|
| `onEnable()` | Вызывается при включении модуля |
| `onDisable()` | Вызывается при выключении модуля |

### Поля

| Поле | Тип | Описание |
|------|-----|----------|
| `plugin` | `Main` | Главный класс плагина |
| `id` | `String` | ID модуля (имя папки) |
| `folder` | `File` | Папка модуля |
| `config` | `YamlConfiguration` | Конфиг модуля |
| `itemsConfig` | `YamlConfiguration` | Предметы модуля |

## 📝 Примеры

### Регистрация команды

```java
@Override
protected void onEnable() {
    registerCommand("mycommand", this);
}

@Override
public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    sender.sendMessage("Привет!");
    return true;
}
```

### Плейсхолдер

```java
private void registerPlaceholders() {
    plugin.getPlaceholderManager().register("mymodule_stat", (player) -> {
        return "42"; // %dci_mymodule_stat%
    });
}
```

### Слушатель событий

```java
@Override
protected void onEnable() {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
}

@EventHandler
public void onJoin(PlayerJoinEvent event) {
    event.getPlayer().sendMessage("Привет!");
}
```

### GUI-меню

```java
private void openMenu(Player player) {
    Inventory menu = Bukkit.createInventory(null, 27, "§6Меню");
    // ... заполнение меню
    player.openInventory(menu);
}
```

### Работа с БД

```java
plugin.getDatabaseManager().execute(
    "CREATE TABLE IF NOT EXISTS my_data (uuid TEXT, key TEXT, value TEXT)"
);
```

## 🗑️ Удаление модуля

Просто удалите папку:

```bash
rm -rf plugins/DC-CustomItems/items/my-module/
/ci reload
```

## 📚 Готовые модули

В jar/repository модули лежат как образцы с префиксом `EXAMPLE-*` —
скопируйте папку в `items/` и уберите префикс из имени:

| Образец | Описание |
|---------|----------|
| `EXAMPLE-vault/` | 💰 Экономика (Vault) |
| `EXAMPLE-deluxemenux/` | 🎨 GUI-меню |
| `EXAMPLE-shop/` | 🛒 Магазин |
