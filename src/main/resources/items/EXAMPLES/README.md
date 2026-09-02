# 📚 Справочные Java-примеры (папка `EXAMPLES/`)

Эта папка содержит **полноценные справочные Java-предметы** для изучения Java API.

## ⚠️ Эта папка не загружается

`EXAMPLES/` **не копируется** на сервер (см. `DefaultContentExtractor`) и
**не компилируется** рантайм-компилятором (см. `ItemRegistry`). То же самое
относится к файлам с префиксом `EXAMPLE-*` в корне `items/` и папке `_template/`.

## ▶️ Как включить пример

1. Скопируйте файл из этой папки в `plugins/DC-CustomItems/items/`
   (или в любую другую подпапку, кроме `EXAMPLES/` и `_template/`).
2. Выполните `/ci reload`.
3. Проверьте: `api-item list` → должен появиться ID предмета.
4. Выдайте: `/api-item give <id>`.

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
