# Сообщения и база данных — русский справочник

## 1. Сообщения в версии 1.320.281

Встроенные сообщения хранятся в классе:

```text
src/main/java/me/dcplugin/dcustomitems/api/config/MessagesConfig.java
```

При первом запуске плагин создаёт:

```text
plugins/DC-CustomItems/items/messages.java
```

Этот файл является редактируемым шаблоном и шпаргалкой с названиями полей. В версии `1.320.281` runtime-компилятор не вызывает метод `load()` автоматически, поэтому простое изменение `items/messages.java` не изменит сообщения после `/ci reload`.

### Как изменить сообщение

1. Остановите сервер или работайте в копии проекта.
2. Откройте `MessagesConfig.java` в исходниках.
3. Измените нужное поле.
4. Соберите новый JAR.
5. Сделайте резервную копию папки плагина.
6. Установите новый JAR и перезапустите сервер.

Пример:

```java
public static String PREFIX = "&8[&bМой сервер&8] &r";
public static String CI_GIVE_SELF = PREFIX + "&aПолучено: &e{item}";
```

Не удаляйте `{item}`, `{player}`, `{count}`, `{error}` и другие placeholders, если код должен подставлять значение.

### Цвета

Используйте старый формат Bukkit:

```text
&a зелёный   &c красный   &e жёлтый   &b бирюзовый
&l жирный    &o курсив    &r сброс цвета
```

## 2. Где смотреть ошибки сообщений

Если после изменения нет результата:

- проверьте, что изменили именно `MessagesConfig.java` в исходном проекте;
- убедитесь, что собранный JAR заменён на сервере;
- проверьте версию `/version DC-CustomItems`;
- посмотрите первую ошибку компиляции Maven или Java;
- не ожидайте, что `/ci reload` перечитает стандартные поля из `items/messages.java`.

## 3. SQLite и MySQL-файл

Полная инструкция по выбору SQLite/MySQL/YAML, подключению MySQL и промокодам: [DATABASE_RU.md](DATABASE_RU.md).

По умолчанию плагин использует SQLite-файл:

```text
plugins/DC-CustomItems/data.db
```

Не редактируйте его обычным текстовым редактором. Перед экспериментами остановите сервер и скопируйте файл:

```bash
cp plugins/DC-CustomItems/data.db plugins/DC-CustomItems/data.db.backup
```

Не удаляйте `data.db`, если нужно сохранить прогресс игроков.

## 4. DatabaseManager для Java-модулей

В Java-механике можно получить менеджер через главный класс плагина:

```java
import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.database.DatabaseManager;

DatabaseManager db = Main.getInstance().getDatabaseManager();
```

Доступны основные методы:

- `connect()` и `disconnect()`;
- `reconnect()`;
- `createTable(table, columns)`;
- `execute(sql, params...)`;
- `insert(table, data)`;
- `update(table, data, where, params...)`;
- `delete(table, where, params...)`;
- `queryInt`, `queryString`, `queryDouble`, `queryBoolean`;
- `queryOne` и `queryAll`;
- `increment` и `add`.

Пример таблицы статистики:

```java
db.createTable("player_stats", "uuid TEXT PRIMARY KEY, kills INTEGER DEFAULT 0");
db.execute(
    "INSERT INTO player_stats(uuid, kills) VALUES(?, 0)",
    player.getUniqueId().toString()
);
db.increment(
    "player_stats",
    "kills",
    "uuid = ?",
    player.getUniqueId().toString()
);
int kills = db.queryInt(
    "SELECT kills FROM player_stats WHERE uuid = ?",
    player.getUniqueId().toString()
);
```

Параметры передавайте через `?`, а не вставляйте пользовательский текст прямо в SQL. Если строка уже существует, обработайте это отдельно или используйте `upsertSql()` с SQL, подходящим для выбранной базы.

## 5. Производительность базы

Не выполняйте SQL-запросы каждый тик и не записывайте данные при каждом движении игрока. Лучше:

- хранить часто используемое значение в памяти;
- сохранять изменения при важных событиях;
- использовать одну таблицу для связанных данных;
- не делать большие запросы во время входа всех игроков;
- закрывать соединение при выключении модуля;
- проверять работу на копии `data.db`.

## 6. Резервная копия перед обновлением

```bash
cp -a plugins/DC-CustomItems plugins/DC-CustomItems.backup
```

Сохраняйте отдельно:

- `items/`;
- `config.yml`;
- `data.db`;
- папки модулей и меню.
