<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.8-green?style=for-the-badge&logo=minecraft" alt="Minecraft">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/Version-1.320.203-blue?style=for-the-badge" alt="Version">
  <img src="https://img.shields.io/github/actions/workflow/status/animesao/dcustomitems/build.yml?style=for-the-badge&label=build" alt="Build">
</p>

<h1 align="center">DC-CustomItems</h1>

<p align="center">
  <b>Мощный плагин кастомных предметов для Minecraft</b><br>
  Эффекты, атрибуты, сет-бонусы, триггеры и многое другое!
</p>

<p align="center">
  <a href="#установка">Установка</a> •
  <a href="#команды">Команды</a> •
  <a href="#конфигурация">Конфигурация</a> •
  <a href="#фичи">Фичи</a>
</p>

---

## Возможности

| Функция | Описание |
|---------|----------|
| Кастомные предметы | Руны, инструменты, броня, зелья |
| Эффекты зелий | Автоматическое применение при экипировке |
| Атрибуты | Урон, скорость, броня и другие характеристики |
| Сеты брони | Бонусы за полный комплект |
| Действия при клике | Молния, команды, частицы, звуки |
| Триггеры | Реакция на урон, убийство, прыжок |
| Per-item сообщения | Настройка сообщений для каждого предмета |
| CustomModelData | Поддержка ресурспаков |
| Авто-обновления | Проверка через GitHub Releases |

---

## Установка

1. **Скачайте** последний релиз из [Releases](https://github.com/animesao/dcustomitems/releases)
2. **Поместите** `dcustomitems-*.jar` в папку `plugins/`
3. **Перезапустите** сервер
4. **Готово!**

### Требования
- Minecraft 1.21.8+
- Java 17+
- Spigot/Paper 1.21.8+

---

## Команды

| Команда | Описание | Право |
|---------|----------|-------|
| `/ci give <id> [игрок]` | Выдать предмет | `customitems.give` |
| `/ci list` | Список предметов | `customitems.use` |
| `/ci reload` | Перезагрузить конфиг | `customitems.reload` |
| `/ci update` | Проверить обновления | `customitems.update` |

**Алиасы:** `/ci`, `/citems`

---

## Конфигурация

### Простой пример
```yaml
my-sword:
  item:
    type: DIAMOND_SWORD
    title: '&6Меч Силы'
    glowing: true
    unbreakable: true
  type: TOOL
  activation-slot: HAND
  placeable: false
  effects:
    - 'INCREASE_DAMAGE:2'
```

### С триггерами
```yaml
legendary-sword:
  trigger-actions:
    - 'on_kill:effect:REGENERATION:10:2'
    - 'on_damage_taken:effect:DAMAGE_RESISTANCE:5:1'
```

---

## Авто-обновления

Плагин автоматически проверяет обновления через GitHub Releases.
При входе администратора на сервер будет сообщение о доступном обновлении.

---

## Сборка

```bash
git clone https://github.com/animesao/dcustomitems.git
cd dcustomitems
mvn clean package
```

---

## Лицензия

MIT License

---

## Автор

**animesao**
