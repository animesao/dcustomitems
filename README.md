<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.11-green?style=for-the-badge&logo=minecraft" alt="Minecraft">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/Version-1.320.223-blue?style=for-the-badge" alt="Version">
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
curl -L -o dcustomitems.jar https://github.com/animesao/dcustomitems/releases/download/v1.320.223/DC-CustomItems-1.320.223.jar

# Restart server
./restart.sh
```

### Manual Install

1. Download `DC-CustomItems-1.320.223.jar` from [Releases](https://github.com/animesao/dcustomitems/releases/tag/v1.320.223)
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
  
  trigger-actions:
    - 'on_click_right:lightning:1'
    - 'on_click_right:message:&6⚡ Lightning!'
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

```yaml
my-item:
  item:
    type: NETHERITE_SWORD
    # All formats work:
    item-model: "smoke"
    # item-model: "minecraft:item/smoke"
    # item-model: "myplugin:custom_sword"
```

---

## 📚 Documentation

| Language | Link |
|----------|------|
| 🇬🇧 English | [Full Documentation](docs/README_EN.md) |
| 🇷🇺 Русский | [Полная документация](docs/README_RU.md) |
| ⚡ Actions & Triggers | [Actions Guide](src/main/resources/items/README-ACTIONS.md) |

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
