<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.8-green?style=for-the-badge&logo=minecraft" alt="Minecraft">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/Version-1.7.0-blue?style=for-the-badge" alt="Version">
</p>

<h1 align="center">DC-CustomItems</h1>

<p align="center">
  <b>Мощный плагин кастомных предметов для Minecraft</b><br>
  Эффекты, атрибуты, сет-бонусы, триггеры и многое другое!
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
| Без лицензии | Полностью бесплатный |

---

## Новые функции v1.7.0

### Активация по рукам
Атрибуты и эффекты работают только в указанном слоте:
```yaml
my-sword:
  activation-slot: HAND  # Только основная рука
  item:
    attributes:
      GENERIC_ATTACK_DAMAGE: 5.0

my-shield:
  activation-slot: OFFHAND  # Только вторая рука
  item:
    attributes:
      GENERIC_ARMOR: 2.0
```

### Тотемы (10 штук)
Уникальные тотемы с эффектами и атрибутами:

| Тотем | Особенность |
|-------|-------------|
| Огня | Боевой тотем |
| Воды | Подводные исследования |
| Земли | Защита и добыча |
| Воздуха | Максимальная мобильность |
| Тьмы | Скрытность и урон |
| Света | Исцеление и поддержка |
| Льда | Максимальная защита |
| Грозы | Молниеносные атаки |
| Природы | Жизненная сила |

### Триггеры
Предметы реагируют на события:
```yaml
legendary-sword:
  trigger-actions:
    - 'on_kill:effect:REGENERATION:10:2'
    - 'on_kill:message:Враг повержен!'
    - 'on_damage_taken:effect:DAMAGE_RESISTANCE:5:1'
```

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

## Установка

1. **Скачайте** последний релиз из [Releases](https://github.com/animesao/dcustomitems/releases)
2. **Поместите** `dcustomitems-1.7.0-shaded.jar` в папку `plugins/`
3. **Перезапустите** сервер
4. **Готово!** Плагин автоматически создаст `config.yml`

### Требования
- Minecraft 1.21.8+
- Java 17+
- Spigot/Paper 1.21.8+

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
    - 'SPEED:1'
```

### С действиями
```yaml
lightning-staff:
  item:
    type: STICK
    title: '&6Посох Молнии'
  click-cooldown: 2000
  right-click-actions:
    - 'lightning:1'
    - 'sound:ENTITY_LIGHTNING_BOLT_THUNDER:1:1'
```

---

## Типы предметов

| Тип | Описание |
|-----|----------|
| `RUNE` | Руна - эффекты при экипировке |
| `TOOL` | Инструмент - действия при клике |
| `ARMOR` | Броня - экипируется в слоты |
| `CONSUMABLE` | Расходуемый - удаляется при использовании |

---

## Сборка

```bash
git clone https://github.com/animesao/dcustomitems.git
cd dcustomitems
mvn clean package
# JAR будет в target/dcustomitems-1.7.0-shaded.jar
```

---

## Лицензия

MIT License - свободное использование и модификация.

---

## Автор

**animesao** - Разработчик плагина
