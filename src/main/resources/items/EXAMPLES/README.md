# 📚 Справочные Java-примеры (папка `EXAMPLES/`)

Эта папка содержит **полноценные справочные Java-предметы** для изучения Java API.

## ⚠️ Эта папка не загружается

`EXAMPLES/` — справочные материалы: **не копируются** на сервер
(см. `DefaultContentExtractor`) и **не компилируются** рантайм-компилятором
(см. `ItemRegistry`). То же самое относится к папке `_template/`.

## 📋 Правило образцов `EXAMPLE-*`

Файлы с префиксом `EXAMPLE-*` в корне `items/` — это образцы: при первом
запуске плагин копирует их в `plugins/DC-CustomItems/items/` как справочный
материал, но компилятор и загрузчики их игнорируют.

## ▶️ Как включить пример

1. Скопируйте нужный файл из этой папки (или из `items/` в jar)
   в `plugins/DC-CustomItems/items/`.
2. Уберите префикс `EXAMPLE-` из имени файла (или папки).
3. Выполните `/ci reload`.
4. Проверьте: `api-item list` → должен появиться ID предмета.
5. Выдайте: `/api-item give <id>`.

> Имя публичного класса должно совпадать с именем файла
> (рантайм-компилятор сам приводит класс к имени файла, но лучше
> не переименовывать).

## 🧭 Что внутри

| Файл | Демонстрирует |
|------|---------------|
| `DragonboneArmor.java` | Полный предмет-броня: `getRecipes()` (shaped+shapeless, ингредиенты-материалы и кастомные ID), `getActivationSlot`/`getType`, хуки `onEquip`/`onUnequip`/`onPeriodic`, боевой `onDamageTaken`, helpers `ItemAPI` |
| `Frostblade.java` | Полное Java-оружие: кликовые хуки `onRightClick`/`onLeftClick`, `getClickCooldown()` (общий на оба клика), боевые `onDamageDealt`/`onKill`, крафт `RecipeDef.shaped`, урон/AoE/эффекты через `ItemAPI` |

См. также: `EXAMPLE-dark-sword.java` и `EXAMPLE-royal-helmet.java` (корень
`items/`) — короткие образцы оружия и брони.
