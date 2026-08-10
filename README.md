<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.11-green?style=for-the-badge&logo=minecraft" alt="Minecraft">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/Version-1.320.253-blue?style=for-the-badge" alt="Version">
  <img src="https://img.shields.io/github/license/animesao/dcustomitems-purple?style=for-the-badge" alt="License">
</p>

<h1 align="center">⚔️ DC-CustomItems</h1>

<p align="center">
  <b>Powerful custom items plugin for Minecraft 1.21.11</b><br>
  Effects, attributes, armor sets, triggers, custom models and more!
</p>

<p align="center">
  <a href="#installation">Installation</a> •
  <a href="#quick-start">Quick Start</a> •
  <a href="#commands">Commands</a> •
  <a href="#documentation">Documentation</a>
</p>

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🗡️ Custom Items | Runes, tools, armor, consumables |
| ⚡ Potion Effects | Auto-apply on equip |
| 📊 Attributes | Damage, speed, armor and more |
| 🛡️ Armor Sets | Bonuses for full sets |
| 🎯 Click Actions | Lightning, commands, effects, particles, sounds |
| 🔄 Triggers | Auto-actions on events |
| 📦 Per-Item Files | Each item in separate YAML |
| 🎨 Custom Models | Resource pack support with `item-model` |
| 🔐 Permissions | Per-item permissions |
| ✨ Particles & Sounds | Equip/unequip effects |

---

## 📦 Installation

### Quick Install

```bash
# Download latest release
cd ~/server/plugins
curl -L -o dcustomitems.jar https://github.com/animesao/dcustomitems/releases/download/v1.320.253/DC-CustomItems-1.320.253.jar

# Restart server
./restart.sh
```

### Manual Install

1. Download `DC-CustomItems-1.320.253.jar` from [Releases](https://github.com/animesao/dcustomitems/releases/tag/v1.320.253)
2. Place in `plugins/` folder
3. Restart server

---

## 🚀 Quick Start

### 1. Give yourself an item

```bash
/ci give vampire-blade
```

### 2. Create your own item

Create a new file `plugins/DC-CustomItems/items/my-item.yml`:

```yaml
my-sword:
  type: TOOL
  activation-slot: HAND
  
  item:
    type: DIAMOND_SWORD
    title: '&bMy Sword'
    item-model: "smoke"  # Use resource pack model
  
  effects:
    - 'SPEED:1'
  
  triggers:
    on_click_right:
      - 'lightning_forward:100'
      - 'title:&6⚡ LIGHTNING!::&7Beware!'
      - 'particles_custom:FLAME:50:0:1:0:1:1:1'
```

### 3. Reload and use

```bash
/ci reload
/ci give my-sword
```

---

## 🎮 Commands

| Command | Description |
|---------|-------------|
| `/ci list` | List all items |
| `/ci give <id>` | Give item to yourself |
| `/ci give <id> <player>` | Give item to player |
| `/ci reload` | Reload plugin |
| `/ci update` | Check for updates |

---

## 🎨 Custom Model Support (1.21.11+)

### YAML Format:
```yaml
my-item:
  item:
    type: NETHERITE_SWORD
    # All formats work:
    item-model: "smoke"
    item-model: "minecraft:item/smoke"
    item-model: "myplugin:custom_sword"
```

### Java API:
```java
public class MySword extends AbstractCustomItem {
    // Simple name
    @Override
    public String getItemModel() { return "dark_sword"; }
    
    // With namespace
    @Override
    public String getItemModel() { return "minecraft:item/smoke"; }
    
    // Custom namespace
    @Override
    public String getItemModel() { return "myplugin:item/weapon"; }
}
```

---

## 🚀 Advanced Triggers System

```yaml
my-item:
  triggers:
    on_equip:
      - 'particles_custom:FLAME:100:0:1:0:2:2:2'
      - 'sound_sequence:ENTITY_BLAZE_AMBIENT:1:1;ENTITY_PLAYER_LEVELUP:0.5:2'
    on_click_right:
      - 'title_sequence:🔥 READY!:::10:20:10;🔥 FIRE!:::10:30:10'
      - 'damage_custom:30:6:4:6'
      - 'effect_sequence:STRENGTH:15:5:0;SPEED:15:3:500'
    on_kill:
      - 'heal:50'
      - 'heal_custom:15:8:5:8:HEART'
```

**All Available Actions:**
- `effect`, `heal`, `damage`, `damage_nearby`, `damage_mobs`, `damage_players`
- `lightning`, `lightning_forward`, `knockback`, `launch`, `stun`
- `particles`, `particles_custom`, `sound`, `sound_sequence`
- `title`, `title_sequence`, `message`, `actionbar`, `broadcast`
- `teleport`, `teleport_relative`, `teleport_sequence`
- `give`, `remove`, `exp`, `sethealth`, `setfood`
- `command`, `console_command`, `command_sequence`
- `vanish`, `glow`, `speed`, `flight`, `fireworks`
- `effect_sequence`, `damage_custom`, `heal_custom`, `heal_nearby`

---

## 📚 Documentation

| Language | Link |
|----------|------|
| 🇬🇧 English | [Full Documentation](docs/README_EN.md) |
| 🇷🇺 Русский | [Полная документация](docs/README_RU.md) |
| ⚡ Actions & Triggers | [Actions Guide](src/main/resources/items/README-ACTIONS.md) |
| 🎨 Models Guide | [Custom Models](src/main/resources/examples/README-MODELS.md) |
| ☕ Java API | [Java API Examples](src/main/resources/items/EXAMPLE-dark-sword.java) |

---

## 📦 Included Items

### ⚔️ Weapons & Armor

| ID | Name | Description |
|----|------|-------------|
| `vampire-blade` | Vampire Blade | Life steal, AoE damage |
| `shadow-blade` | Shadow Blade | Critical strikes |
| `thunder-axe` | Thunder Axe | Lightning attacks |
| `frost-wand` | Frost Wand | Ice spells |
| `elemental-staff` | Elemental Staff | Multiple elements |
| `berserker-axe` | Berserker Axe | Rage mode |
| `healer-amulet` | Healer Amulet | Healing abilities |
| `nature-totem` | Nature Totem | Nature powers |
| `archer-bow` | Archer Bow | Special arrows |
| `shadow-helmet` | Shadow Helmet | Stealth mode |
| `air-wand` | Air Wand | Lightning forward |
| `breaker-pickaxe` | Breaker Pickaxe | Instant block break |
| `artifact-blade-of-destiny` | Blade of Destiny | Ultimate weapon |
| `artifact-chaos-orb` | Chaos Orb | Random effects |

### 🧪 Consumable Items

| ID | Name | Uses | Effect |
|----|------|------|--------|
| `health-potion` | Health Potion | 3 | Heal 10 + Regeneration |
| `speed-potion` | Speed Potion | 5 | Speed II 30s |
| `golden-apple` | Golden Apple | 2 | Heal + 3 effects |
| `xp-scroll` | XP Scroll | 1 | +500 XP + 5 levels |
| `shield-totem` | Shield Totem | 4 | Absorption + Resistance |
| `fire-resistance-potion` | Fire Resistance | 3 | Fire Resist 60s |
| `nature-talisman` | Nature Talisman | 1 | Regeneration 30s |
| `teleport-scroll` | Teleport Scroll | 3 | Teleport to random player |

### 📝 Example Items

| ID | Name | Description |
|----|------|-------------|
| `EXAMPLE-ultimate-fire-sword` | Ultimate Fire Sword | Full demo of all actions |
| `EXAMPLE-teleport-mage` | Teleport Mage Staff | Teleport sequences |
| `EXAMPLE-ultimate-potion` | Ultimate Potion | Effect sequences |

---

## 🛠️ Requirements

- Minecraft 1.21.11+
- Paper/Spigot
- Java 17+

---

## 🔗 Links

- [GitHub Repository](https://github.com/animesao/dcustomitems)
- [Releases](https://github.com/animesao/dcustomitems/releases)
- [Issues](https://github.com/animesao/dcustomitems/issues)

---

## 📄 License

MIT License - see [LICENSE](LICENSE) for details.

---

**Made with ❤️ by animesao**
