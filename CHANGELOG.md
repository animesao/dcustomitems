# Changelog

Все изменения DC-CustomItems по версиям.

Формат секций: `## v<версия>` — скрипт релиза (`.github/workflows/build.yml`)
извлекает секцию нужной версии для заметок GitHub Release.

---

## v1.336.0

### 🔒 MySQL: проверка сертификата сервера

- Новая опция `database.mysql.verify-server-certificate` (по умолчанию
  `false` — чтобы не сломать существующие подключения).
  При `use-ssl: true` + `verify-server-certificate: true` используется
  `sslMode=VERIFY_IDENTITY`: проверяются и сертификат, и имя хоста.
- Раньше `use-ssl: true` давал только шифрование без проверки сертификата —
  MITM оставался возможным.
- Плагин пишет предупреждение в лог, если соединение не зашифровано или
  сертификат не проверяется, и если пароль лежит в `config.yml` открытым
  текстом (подсказка про `DCI_MYSQL_PASSWORD`).

---

## v1.335.0

### 🔒 Security

Full security review of the plugin (attack-surface mapping, input-to-sink
tracing, authorization, concurrency, data exposure, dependencies).

- **Module commands did not check their permission at all.**
  `registerDynamicCommand(name, executor, permission)` only *set* the
  permission — `Command.setPermission()` just fills a field, and
  `SimpleCommandMap.dispatch()` never calls `testPermission()` (only
  `PluginCommand` does, and the plugin registers a raw `Command`).
  **Any player could run every module command**, including the admin ones
  from the samples: `/eco give|take|set|reset <player> <amount>` gave full
  control over the economy. The permission is now checked before the
  executor runs, and tab-completion respects it too.
- **`java-compilation: false` did not stop `.jar` files from being loaded.**
  A jar in `items/` was loaded and its classes instantiated — the same
  arbitrary-code-execution path the flag is supposed to close. Jar loading
  is now behind the same switch, so the mitigation promised in `SECURITY.md`
  actually works.
- **An item's `permission:` was only checked on click.** A player without
  the right still got the item's passive effects, attributes, armour-set
  bonus and every trigger (`on_kill`, `on_pickup`, …). The permission now
  gates all of those paths as well.
- **The `break:` action bypassed region protection.** It called
  `block.breakNaturally()` directly, which never fires `BlockBreakEvent` —
  WorldGuard, GriefPrevention and Lands never saw the block being broken.
  The action now goes through a real `BlockBreakEvent` and respects its
  cancellation.
- **Raw exception messages were sent to players** (a failed custom command
  could leak SQL text or a file path). Players now get a generic message;
  the details stay in the log.
- **MySQL: the server certificate was never verified.** `use-ssl: true` only
  encrypted the connection — `sslMode=REQUIRED` does not check the
  certificate, so a MITM stayed possible. There is now a
  `database.mysql.verify-server-certificate` option (uses
  `sslMode=VERIFY_IDENTITY`, which also checks the hostname), and the
  plugin warns in the log when the connection is unencrypted or
  unverified.

### 🧯 Concurrency (PlaceholderAPI runs on its own thread)
- `EffectManager.playerAppliedEffects` and `ItemRegistry.registeredItems`
  were plain `HashMap`/`LinkedHashMap` written by the main thread and read
  by the PlaceholderAPI thread — a data race that can corrupt the hash
  table and hang the request thread. Both are `ConcurrentHashMap` now.

### 🐛 Correctness

- **Effects and attributes were not re-applied after `/ci reload`.** The
  equipment hash is built from item IDs, and the IDs did not change after a
  reload — so editing an item's `effects:` or `attributes:` had no effect
  until the player took the item off and put it back on. The cache is now
  invalidated on reload.
- **Items with `duration:` never expired.** The acquisition time was written
  into a *copy* of the item stack (`Inventory#getContents()` returns
  copies), so the timestamp was lost every second and the item was never
  considered expired. The stack is now written back.
- **The SQLite connection proxy closed the shared connection.**
  `try (Connection c = db.getConnection())` closed the one shared
  connection for the whole plugin; every later query failed with
  "SQLite is not connected". `close()` is a no-op for SQLite now (MySQL
  still returns a pooled connection that has to be closed).
- The Java API item cooldown was shared by all items of a player; it is
  per item now, like the YAML path.
- `toLowerCase()` without `Locale.ROOT` in the placeholder expansion broke
  every ID in a Turkish locale.

### ⚡ Performance

- **The equipment check re-read the PDC of all six slots on every tick**
  whenever inventory events kept firing (auto-clickers, an open chest):
  `markForRecalculation` reset the 150 ms gate. The gate is no longer reset
  — the hash detects changes on its own, so a change is still noticed
  within 150 ms at a third of the cost.
- **The equipment hash was rebuilt every tick** for every player (20
  `StringBuilder`s per second each) although the slots only change every
  150 ms. It is now built together with the slot snapshot.
- **`ApiEventListener.onPlayerMove` was the most expensive line in the
  plugin**: 8 metadata clones per move event (~160/s per player) even when
  no Java item existed. It now exits immediately when the Java API is
  empty and reads the metadata once instead of twice.
- `CustomItem.getParsedEffects()` copied an already immutable map on every
  call (up to 12 copies per equipment recalculation).
- The placeholder expansion no longer copies the whole placeholder set or
  the whole effect map just to call `.size()`.

### 📖 Documentation

- `SECURITY.md` and `wiki/15-permissions.md` now describe the real
  semantics of `permission:` (it gates clicking, wearing and triggers) and
  the `java-compilation` switch (it covers `.jar` files too).

### ⚠️ Update notes

- If you distribute items to players, add `permission:` to them — without
  it every holder gets the effects and triggers.
- If you have a module whose command should work without permissions,
  pass `null` as the permission explicitly.

---

## v1.334.0

### 📖 Wiki

A complete A-to-Z guide for beginners now lives in the `wiki/` folder of the
repository — 19 pages, ~8300 lines, everything in Russian.

- **Start here:** `wiki/README.md` (index), `wiki/01-Установка.md`,
  `wiki/02-Как-это-работает.md`
- **What you can build:** YAML items, Java items, all 54 actions, all 12
  triggers, all five recipe types, custom commands, placeholders, modules,
  the central message system, the database API and the events for
  third-party plugins
- **Reference:** the full `config.yml`, every permission node, a copy-paste
  example for every feature, a troubleshooting guide and a complete Java API
  reference
- All cross-links and table-of-contents anchors were verified; the pages are
  valid UTF-8 without BOM.

### 🐛 Fixed

- **`armor-set`, `has-set-bonus` and `attributes` were ignored** for every
  YAML item that declared them at the top level — which is how all the
  samples and the README do it. The loader only looked inside the `item:`
  section, so **no armor set and no attribute bonus from a YAML item ever
  worked**. Both places are now supported, the top level takes priority.
- `samples/EXAMPLE-messages.java` did not compile (it referenced about fifty
  fields that do not exist in `MessagesConfig`), so copying it to
  `items/messages.java` never applied the messages.

### ⚠️ Update notes

- If you use `armor-set` or `attributes` in a YAML item **inside** the `item:`
  section, nothing changes. If you declared them at the top level, they
  start working after this update — check that the bonus is what you want.

---

## v1.333.0

### 💬 One central message system

Every text the player sees now lives in one place and can be overridden by
the administrator.

- **All hard-coded player-facing strings were moved to the message system**:
  item expiry and world bans, the permission and "uses depleted" texts, the
  `/ci` output (help, validation, debug, backup), the custom-command texts
  (`Command not found`, `Player not found`, `Specify a player`), the
  permission message of `CustomCommand` and the update-checker notification.
- **`MessagesConfig` is now addressed by path** — `items.expired`,
  `items.world-blocked`, `commands.not-found`, `ci.reload.success`,
  `update.title` and so on. The old constants still work.
- **Modules can plug their own texts into the same system**:
  - `registerMessage("my-module.no-money", "...")` in a module
  - `getMessage("my-module.no-money", "...")` to read it
  - `MessageManager#registerMessage(path, value)` for third-party plugins
  - `CustomCommand#message(path, default)` for custom commands
- **Any path works**, including your own: `MessagesConfig.set("my.text", "...")`
  in `items/messages.java` and the module reads it back by the same path.
- **Priority:** override from `items/messages.java` → the module's own text →
  the plugin's built-in message → the fallback default.
- Overrides that were removed from `items/messages.java` no longer survive a
  reload.

### 🐛 Fixed

- **`samples/EXAMPLE-messages.java` did not compile**: it referenced about
  fifty fields that do not exist in `MessagesConfig` (`CI_HELP_GIVE`,
  `LIST_HEADER`, `API_HEADER`, `BERSERK_ENABLED`, …). Copying it to
  `items/messages.java` produced a compile error, so the messages were never
  applied. The sample now only uses existing fields and demonstrates the
  custom-path API. Verified by compiling the sample the same way the plugin
  does.

### ⚠️ Update notes

- Nothing changes for existing servers: `items/messages.java` keeps working
  as before.
- If you use a custom command that relied on the old default permission
  message, it is now resolved through the message system — the text is the
  same unless you override it.

---

## v1.332.0

### 📦 The plugin installs exactly one file

A fresh install now contains **only** `plugins/DC-CustomItems/items/messages.java`
— one file, one compiled class. Nothing else is created or copied.

- **The samples are no longer shipped inside the jar.** They moved to the
  `samples/` folder in the repository, so the jar contains only `config.yml`
  and `plugin.yml` from the plugin's own resources.
- **`DefaultContentExtractor` was removed** together with the
  `extract-default-modules` option — there is nothing left to extract.
  The key is still ignored when reading an old `config.yml`, so existing
  servers keep working.
- **How to use a sample now:** copy the file or folder you need from
  `samples/` into `items/`, remove the `EXAMPLE-` prefix from its name and
  run `/ci reload`.
  - `samples/EXAMPLE-runes.yml` → `items/runes.yml`
  - `samples/EXAMPLE-vault/` → `items/vault/`
- `samples/README.md` documents what every sample shows.
- Docs and `config.yml` were updated to describe the new layout.

### ⚠️ Update notes

- Nothing is removed from an existing server: `items/` keeps whatever you
  already have.
- The `extract-default-modules` option no longer exists. Remove it from
  your `config.yml` if you want — it is simply ignored.

---

## v1.331.0

### 🌍 English by default

The plugin now ships with **English** as the default language instead of Russian.

- **All player-facing messages** are English: `MessagesConfig`, the built-in
  message fallbacks (`items.no-permission`, `items.uses-depleted`,
  `plugin.update-*`), inline messages in the listeners and the equipment
  checker, `/ci` command output, the update-checker messages and the
  `CustomCommand` base-class messages.
- **`items/messages.java`**, generated on first start, is now English.
  Existing servers keep their own file, so nothing changes for them.
- **All log output** is English (compiler, recipe loader, item loader,
  extractor, command registrar, module manager).
- **`config.yml`** comments are English.
- **All samples in `items/`** (`EXAMPLE-*.yml`, `EXAMPLE-*.java`, module
  folders, `_template/`, `EXAMPLES/`) are English: item names, lore,
  messages and documentation.
- **All source comments and javadoc** are English.

### ⚠️ Update notes

- No configuration keys were renamed and no behaviour changed — only text.
- Servers that want Russian back can simply edit `items/messages.java` or
  set the `MessagesConfig.*` fields again.

---

## v1.330.0

### 💥 Исправлено

- **Атрибуты предметов не применялись вообще**: модификаторы создавались через `NamespacedKey.fromString(...)` без namespace — ключ уходил в `minecraft:` (или был `null` при ID в верхнем регистре), и Bukkit отклонял его. Добавлен `EnumCache.getAttribute()`: понимает и современные имена (`attack_damage`), и legacy (`GENERIC_ATTACK_DAMAGE`, `PLAYER_*`), и регистровые ключи.
- **Уровни зелий складывались вместо выбора максимума**: два предмета с `SPEED:1` давали Speed II. Теперь берётся максимум (`Math::max`).
- **`cooldown:МС` в триггерах ничего не блокировал**: значение записывалось, но никогда не проверялось. Теперь псевдо-действие реально откладывает следующее срабатывание триггера.
- **Кулдаун клика был общим на игрока**: кулдаун одного предмета блокировал другой. Теперь ключ — игрок + предмет.
- **NPE при пустом действии**: `ActionParser.execute(player, " ")` падал с NullPointerException вне блока try.
- **`remove:`/`ItemAPI.removeItem` ничего не удаляли**: `Inventory#getContents()` возвращает копии стеков, изменения не попадали обратно в инвентарь.
- **Ключ счётчика использований в lore не совпадал с тем, которым пишется**: читался `dcustomitems:item_uses`, а писался ключ в namespace плагина (`dc-customitems:item_uses`). Из-за этого `%uses%` всегда показывал максимум вместо остатка.
- **`onRegister()` у команд и плейсхолдеров не вызывался при старте сервера** — только при `/ci reload`. Java-плейсхолдеры появлялись лишь после первой перезагрузки.
- **Расходник с `max-uses` не давал эффект на последнем использовании** — предмет исчезал раньше, чем применялся эффект.
- **Слот не освобождался после расхода**: `setAmount(0)` оставлял «пустой» стак в инвентаре.
- **`type` и `activation-slot` сравнивались с учётом регистра**: `type: consumable` и `activation-slot: hand` не работали. Значения нормализуются в верхний регистр.
- **Предметы на Java API не проверялись на `placeable` и на запрет в мире** при установке и подборе.
- **Команды модулей и Java-API не регистрировались**: рефлексия искала поле `commandMap` в классе `Bukkit` (в Paper оно в `CraftServer`). Добавлен `CommandMapAccessor` с тремя стратегиями поиска и корректным снятием команд при reload.
- **Команды оставались в `CommandMap` после reload**: при недоступной `knownCommands` возвращалась пустая копия карты — команды «удалялись» только формально.
- **NPE при загрузке YAML-предметов**: реестр Java API создавался после YAML-предметов, а рецепт мог ссылаться на Java-предмет (`getApiItemRegistry()` возвращал `null`).
- **Файл `.java` с собственным `package`** не компилировался: импорты вставлялись в начало файла, а объявление `package` оставалось вторым. Импорты теперь вставляются после `package`, а собственное объявление удаляется (иначе получалось два `package`).
- **Файл с любым импортом из `api` не получал остальные импорты** → не компилировался. Импорты проверяются поимённо.
- **Имя генерируемого класса могло быть невалидным** (`1abc.java` → `1abcItem`, `my.item.java` → `my.itemItem`) — такой исходник не собирался.
- **Текстура головы из base64 не применялась** (ошибка смещения при разборе JSON) — исправлено.
- **`teleportToNearest` падал на игроков в других мирах** (`Location#distance` бросает исключение).
- **Отбрасывание рядом стоящих игроков давало NaN-скорость** при нулевом векторе.
- **Java-предметы светились за счёт `SHARPNESS`** (+1 к урону) — теперь используется безобидный `UNBREAKING`.
- **Сбой `UpdateChecker` на версиях вида `1.2.3-beta`** и незакрытое HTTP-соединение при ошибке.
- **Сообщения из `messages.java` писались в системной кодировке** (на Windows — «кракозябры»). Теперь всегда UTF-8.
- **YAML-модули с именем папки в верхнем регистре было невозможно найти** — ID нормализуется.

### 🧯 Утечки памяти и ресурсов

- `ApiEventListener` не удалял кулдауны игроков при выходе — карта росла бесконечно.
- `EffectManager` не чистил состояние игроков при выходе (`stopPeriodicEffects` был заглушкой) — эффекты оставались на игроке до 60 с, записи — в памяти.
- `ItemRegistry` не закрывал `URLClassLoader` загруженных jar — оставались открытые дескрипторы файлов.
- Плейсхолдеры Java API не снимались с регистрации при reload — старые классы (и classloader) оставались в карте.
- Статическая ссылка `Main.instance` не обнулялась при выключении — старый classloader не выгружался.
- `JavaItemCompiler` не закрывал `StandardJavaFileManager` при исключении внутри компиляции.
- Счётчики `CooldownStore` сохранялись YAML-путём — ID с точкой ломали запись/чтение. Новый формат — список записей (старый читается для совместимости).

### ⚡ Оптимизация

- `EquippedItemsChecker`: PDC каждого слота читается один раз за 150мс (было каждый тик — 20 раз/сек на игрока), два обхода онлайн-игроков объединены, следы частиц и периодические хуки пропускаются, если таких предметов нет.
- `ApiEventListener.onMove` больше не срабатывает на поворот камеры (событие шло ~20 раз/сек на игрока).
- `EffectManager`: разбор сет-бонусов кэшируется (был на каждый пересчёт экипировки).
- `JavaItemCompiler`: шаблоны замены имён компилируются один раз, а не на каждый файл × алиас.
- `EnumCache` переведён на `ConcurrentHashMap` (потокобезопасен) и кэширует отрицательные ответы.
- `DCIPlaceholderExpansion`: список ID предметов кэшируется вместо копирования на каждый запрос плейсхолдера.
- `ItemRegistry.getAll*()` возвращают снимки — обход внешнего кода больше не может получить `ConcurrentModificationException` во время reload.
- `CustomItemHandler.getAllCustomItems()` больше не копирует карту на каждый вызов; карта подменяется атомарно (без «полупустого» окна при reload) и безопасна для обхода из потока PlaceholderAPI.
- `reloadAsync()` использует отдельный поток вместо блокировки потока общего пула ForkJoin.

### 🔒 Безопасность

- Имена таблиц и колонок в `DatabaseManager` проверяются как SQL-идентификаторы — больше нет подстановки произвольных строк в SQL.
- Обращения к общему SQLite-соединению сериализуются (иначе «database is locked» и риск повреждения данных при параллельных запросах).
- `getConnection()` для SQLite возвращает потокобезопасную обёртку, которая пробрасывает `SQLException` как есть.

### 🆕 Новое

- **`file-logging: false`** (в `config.yml`) — логи плагина в `plugins/DC-CustomItems/logs/latest.log` вместо консоли. Раньше `PluginLogManager` был написан, но не подключён.
- `/ci validate` и `/ci debug` теперь видят и Java API-предметы.

### ⚠️ Обновление с прошлых версий

- `config.yml` не перезаписывается автоматически: ключ `file-logging` добавится при следующем обновлении вручную (по умолчанию выключено, поведение не меняется).
- Уровни зелий от нескольких предметов теперь берут максимум, а не сумму — если баланс полагался на сумму, задавайте нужный уровень явно.

---

## v1.329.0

### 🔧 Исправлено

- **Образцы `EXAMPLE-*` больше не спамят `items/` по умолчанию**: `extract-default-modules` теперь `false` из коробки (было `true`). На чистом сервере папка `items/` остаётся пустой — только рабочий `items/messages.java`. Автокопирование включается только явно (`extract-default-modules: true`), копируются лишь отсутствующие файлы.
- **Включение образца — копированием без префикса**: `EXAMPLE-runes.yml` → `runes.yml`, `EXAMPLE-vault/` → `vault/`, далее `/ci reload` (комментарии в `config.yml` и javadoc обновлены).
- **`config.yml`: ключи `java-compilation`, `backup`, `extract-default-modules` больше не считаются предметами** — добавлены в skip-списки `ItemLoader.CONFIG_SKIP_KEYS` и `ConfigMigrator` (убирает ложные варнинги и ложное предложение миграции).

### ⚠️ Обновление с прошлых версий

- На существующем сервере `config.yml` не перезаписывается автоматически: удалите вручную `items/EXAMPLE-*` и выставьте `extract-default-modules: false`, если не хотите автокопирования.

---

## v1.328.0

### 💥 Удалено (breaking)

- **`buy-price` / `sell-price` удалены**: поля выпилены из `CustomItem`, `ItemLoader` их игнорирует с варнингом. Магазин мёртв с v1.326 — экономика только через Vault/shop-модули.
- **`trigger-actions:` удалён**: остался только канон `triggers:` (секция с v1.324). Старый ключ игнорируется с SEVERE-логом. Примеры `EXAMPLE-tools.yml`, `EXAMPLE-advanced.yml` переведены на `triggers:` скриптом.

### ✨ Новое (из прошлого спринта, релизится здесь)

- **`/ci validate`** — проверка рецептов (`ItemValidator`): shaped/shapeless/smithing/stonecutting, неизвестные ингредиенты.
- **`/ci debug <id>`** — дамп предмета (тип/слот/uses/cd/рецепты/PDC).
- **`/ci backup` + автобэкап** перед `/ci reload` (`backup.on-reload/keep`, `BackupManager`).
- **Рецепты `smithing` / `stonecutting`** — YAML + Java API (`RecipeDef.smithing()/stonecutting()`), `schema/items.schema.json` для IDE.
- **`CooldownStore`** — персистентные кулдауны (`cooldowns.yml`).
- **Безопасность**: лимиты компилятора (300 файлов / 512KB), варнинги на `System.exit/Runtime.exec/ProcessBuilder/setAccessible`.
- **БД**: `shutdownAsync()` с `awaitTermination(5с)` — гонки при disable больше нет.
- **`CustomItem.parsedEffects`** — `volatile` + defensive copy.
- **Дока**: `docs.md` стал редиректом на `docs/`.

---

## v1.327.0

### 📦 Контент — только образцы по умолчанию

- **Всё содержимое `items/` переведено в образцы `EXAMPLE-*`**: предметы
  (`EXAMPLE-vampire-blade.yml`, `EXAMPLE-runes.yml`, ...), команды
  (`EXAMPLE-give-command.java`, ...) и модули (`EXAMPLE-vault/`,
  `EXAMPLE-shop/`, `EXAMPLE-deluxemenux/`, `EXAMPLE-customcraft/`).
- **По умолчанию при первом запуске копируются только файлы `EXAMPLE-*`**
  (компилятор и загрузчики их игнорируют), плюс создаётся рабочий
  `items/messages.java`. Ни один предмет/команда/модуль не активируется
  автоматически — сервер стартует «чистым».
- **Как включить образец**: убрать префикс `EXAMPLE-` из имени файла/папки
  (`EXAMPLE-vampire-blade.yml` → `vampire-blade.yml`, `EXAMPLE-vault/` →
  `vault/`) и выполнить `/ci reload`.
- `extract-default-modules` в `config.yml` по-прежнему управляет
  автокопированием образцов из jar в `items/`.
- Документация обновлена: README, docs/, комментарии в конфигах и шапки
  образцов описывают новое правило.

---

## v1.326.0

### ✨ Модули

- **Модуль `vault/` упрощён до чистой экономики**:
  - **Убран магазин кастомных предметов**: удалены команды `/buy`, `/sell`,
    `/sellhand`, `/worth` и все настройки `shop:` из `config.yml`. Покупка/продажа
    предметов теперь живёт в отдельных плагинах, работающих с валютой через
    Vault или публичный API модуля (`getBalance`, `withdraw`, `deposit`, `format`).
  - **Убран топ богачей**: удалены команда `/baltop`, настройки `baltop-size` /
    `baltop-refresh-seconds` и плейсхолдеры `%vault_top_name_N%` / `%vault_top_value_N%`.
  - Остались: `/balance` (алиасы `/bal`, `/money`), `/pay`,
    `/eco give|take|set|reset` (админ, право `dci.eco`), стартовый баланс,
    собственная БД (`economy.storage: own`), плейсхолдеры `%vault_balance%` /
    `%vault_currency%` — валюта для игроков как есть.

---

## v1.325.0

### 🔒 Безопасность

- **Флаг `java-compilation: true/false` в config.yml** — runtime-компиляция
  `.java` из `items/` теперь можно отключить (это исполнение произвольного
  кода; при недоверенном доступе к файлам плагина — выключать). При `false`
  все `.java`-файлы в `items/` игнорируются, YAML-предметы работают как обычно.
- **`SECURITY.md`** — политика сообщения об уязвимостях.
- **CodeQL** (security-extended) — отдельный workflow, еженедельно и на PR.
- **OWASP dependency-check** — профиль `-Psecurity` в Maven (аудит зависимостей,
  `failBuildOnCVSS=9`).

### 🧪 Тесты и CI

- **JaCoCo** — отчёт покрытия тестами, артефакт в GitHub Actions.
- **JDK 21** в CI и в `maven.compiler` (Paper 1.21.x требует Java 21 в рантайме).
- Надёжное проставление версии в CI: `mvn versions:set` вместо хрупкого `sed`.
- `build.sh` переписан: версия читается из `VERSION` (раньше был захардкожен
  `1.6.5` и неверный путь к jar).

### 🛠 Исправления и чистка

- Удалена мусорная строка в конце `.gitignore` (артефакт прошлой операции).
- Удалён `items/test.yml` из штатных ресурсов — тестовые предметы больше не
  копируются на серверы пользователей.
- Отключена Maven-фильтрация ресурсов (`filtering: false`) — подстановка
  `${...}` могла тихо ломать YAML-предметы с плейсхолдерами.
- Компиляция против **paper-api 1.21.8** (вместо spigot-api) — соответствует
  заявленной поддержке Paper/Folia/Purpur; версия Minecraft унифицирована
  по документации (1.21.8+).

---

## v1.324.0

### 🏗️ Модульность — теперь по-настоящему

- **Штатные модули/предметы копируются из jar в `items/` при первом запуске**
  (`DefaultContentExtractor`). Раньше папка `items/` создавалась пустой, и плагин
  был «голым» — теперь модули, команды и предметы появляются как обычные файлы.
- Всё функциональное — файлы: удали `items/give-command.java` — исчез `/give`;
  удали `items/vault/` — исчезли `/buy /sell`; удали любой `.yml` — предмет пропал.
- Ядро осталось «компилятором»: нотация файлов + базовый инфраструктура (реестр,
  события, БД). При пустом `items/` работает только `/ci reload`.
- Папки-справочники `EXAMPLES/`, `_template/` и файлы `EXAMPLE-*` на сервер
  не копируются и не компилируются.
- Правила экстракции: `vault/` копируется только при установленном Vault
  (иначе его Java не скомпилируется). Отключение: `extract-default-modules: false`.

### ⚡ Единый парсер действий

- Один `ActionParser` вместо трёх дублированных реализаций: клики (YAML) и
  триггеры выполняются одинаково.
- Дефисы и подчёркивания эквивалентны: `damage-mobs` = `damage_mobs`.
- Добавлены недостающие действия: `effect_nearby*`, `knockback/launch/stun_mobs/players`,
  `break`; `title` понимает все форматы; `teleport` — `~`-координаты и yaw;
  `give` умеет выдавать кастомные предметы по ID; `command` — от консоли.
- Починен сломанный `set_xp` и кулдаун триггеров (ключи не совпадали —
  `cooldown:X` теперь реально работает).

### 🔔 События для сторонних плагинов (YAML и Java предметы)

| Событие | Когда |
|---|---|
| `CustomItemEquipEvent` | Предмет экипирован/снят (до FX/хука) — отменяемо |
| `CustomItemUseEvent` | ЛКМ/ПКМ предметом (до действий) — отменяемо |
| `CustomItemCraftEvent` | Крафт в обычном верстаке и в GUI `/craft` — отменяемо, `setResult()` |
| `CustomItemDamageDealtEvent` / `DamageTakenEvent` | Урон предметом / по игроку с предметом — урон меняется |
| `CustomItemKillEvent` / `DeathEvent` | Убийство игрока / смерть с предметом — отменяемо |
| `CustomItemPeriodicEvent` | Перед каждым `onPeriodic` Java-предмета — отменяемо |

### 🛠 Крафт-рецепты

- Рецепты прямо в YAML-секции предмета: `shaped`, `shapeless`, `furnace`
  (пример — `items/EXAMPLE-crafting.yml`). Перерегистрация при `/ci reload`.
- **Java-предметы** объявляют рецепты через `getRecipes()` + класс `RecipeDef`.
- **GUI-крафт**: модуль `items/customcraft/` — команда `/craft`, окно 3×3,
  предпросмотр результата. Идентичность ингредиентов по PDC/NBT: кастомный
  предмет матчится по ID (можно «сам себя»), чужие предметы — по материалу,
  их NBT не повреждается.
- `/give` теперь выдаёт и Java-предметы (резолв по ID, список, tab-complete,
  `all`/`<игрок>`/`<amount>`).

### ☕ Java API — «можно писать что угодно»

- Механики YAML-уровня у Java-предметов: `getMaxUses()`, `getDuration()`,
  `isAllowedInWorld()`, кастомные сообщения `get*Message()`.
- `getPermission()` теперь проверяется при кликах (как у YAML).
- Хуки `onEquip` / `onUnequip` / `onPeriodic` реально вызываются глобальным
  чекером экипировки (раньше были мёртвыми).
- **Компилятор**: classpath включает серверный jar (Paper / Folia / Purpur /
  Spigot), все `plugins/*.jar` и папку `plugins/DC-CustomItems/libs/` — любые
  сторонние библиотеки подключаются класть jar в `libs/` и `/ci reload`.
- Динамические команды модулей корректно удаляются при отключении/перезагрузке
  (раньше висели в CommandMap).
- `items/messages.java` — рабочий конфиг сообщений: `load()` вызывается на
  старте и при `/ci reload`, правки без пересборки JAR.

### 🐛 Исправления

- `SHADOW-dashblade.java` не компилировался на 1.21.8 (устаревшие particle /
  rayTrace / sendActionBar) — исправлен.
- Устаревший API в примерах (`EXAMPLE-royal-helmet`) приведён к актуальному.
- Удалён мёртвый `TriggerConfig`, неиспользуемый `loadJavaModule` и дубли кода FX.
- `EnumCache`/`ActionParser` инициализируются лениво — надёжнее при старте.

### 🧪 Тесты и документация

- 16 JUnit-тестов: нормализация действий `ActionParser`, `EnumCache`.
- Справочные примеры: `items/EXAMPLES/DragonboneArmor.java` (броня: рецепты,
  экипировка, периодика) и `items/EXAMPLES/Frostblade.java` (оружие: клики,
  кулдаун, бой).
- Документация актуализирована (события, рецепты, GUI-крафт, Java API,
  messages, `libs/`).

---

## Ранние версии

- **v1.323.0** — стабилизация модулей и компилятора.
- **v1.322.0** — duration, trail-частицы, запреты миров, триггер `on_sprint`,
  анимированные модели, CI-релизы из `VERSION`.
- **v1.321.1** — полное обновление документации и модульная архитектура.