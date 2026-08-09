# DC-CustomItems Plugin Documentation

## 📚 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Commands](#commands)
- [Permissions](#permissions)
- [Resource Pack](#resource-pack)
- [Java API](#java-api)
- [Troubleshooting](#troubleshooting)
- [Support](#support)

---

## 📖 Overview

DC-CustomItems is a powerful plugin for Minecraft servers (Spigot/Paper 1.20+) that allows you to create custom items with unique abilities, effects, and mechanics.

### What You Can Create:

- ⚔️ **Custom Weapons** - Swords, axes, bows with special abilities
- 🛡️ **Custom Armor** - Helmets, chestplates with unique effects
- 🧪 **Potions & Consumables** - Custom potions, food, scrolls
- 🔮 **Special Items** - Totems, wands, tools with abilities
- 🎯 **Any Item** - Full customization with no limits

---

## ✨ Features

### Core Features:

| Feature | Description |
|---------|-------------|
| **YAML Configuration** | Create items using simple YAML files |
| **Java API** | Advanced items using Java code |
| **Hot Reload** | Reload items without server restart |
| **Custom Models** | Support for resource pack models |
| **Effects System** | Particles, sounds, potions, titles |
| **Cooldowns** | Configurable cooldowns for abilities |
| **Permissions** | Per-item permission system |
| **Multi-Trigger** | Multiple actions per event |

### Supported Events:

- Left Click (LKM)
- Right Click (PKM)
- Equip/Unequip
- Block Break
- Entity Damage
- Player Death
- And more...

---

## 📋 Requirements

| Requirement | Version |
|-------------|---------|
| Minecraft Server | 1.20+ (tested on 1.21.11) |
| Server Software | Spigot, Paper, or compatible |
| Java | 17 or higher |
| Plugin Loader | Bukkit/Spigot |

---

## 🚀 Installation

### Step 1: Download

Download the latest version from GitHub:
```bash
curl -L -o dcustomitems.jar https://github.com/animesao/dcustomitems/releases/latest/download/DC-CustomItems.jar
```

### Step 2: Install

Place the JAR file in your server's `plugins/` folder:
```
server/
└── plugins/
    └── dcustomitems.jar
```

### Step 3: Restart Server

Restart your Minecraft server or reload with `/ci reload`.

### Step 4: Verify

Check if the plugin is loaded:
```
/pl
```

You should see `DC-CustomItems` in the list.

---

## 🎮 Quick Start

### Creating Your First Item

1. **Navigate to the items folder:**
```
plugins/DC-CustomItems/items/
```

2. **Create a new file:** `my-sword.yml`

3. **Add this configuration:**
```yaml
my_sword:
  item:
    type: DIAMOND_SWORD
    title: '&6My First Sword'
    glowing: true
  lore:
    - ''
    - '&7A powerful custom sword'
    - ''
  type: TOOL
  activation-slot: HAND
  trigger-actions:
    - 'on_click_right:effect:SPEED:10:1'
    - 'on_click_right:particle:FLAME:50'
    - 'on_click_right:message:&6Sword activated!'
```

4. **Reload the plugin:**
```
/ci reload
```

5. **Get your item:**
```
/ci give my_sword
```

### That's it! You've created your first custom item!

---

## ⚙️ Configuration

### Item Configuration Structure

```yaml
item_id:                          # Unique identifier
  item:
    type: MATERIAL                # Minecraft material
    title: '&6Display Name'      # Item name (supports & colors)
    glowing: true                 # Enchantment glow
    unbreakable: true             # Cannot be broken
    custom-model-data: 1001       # Custom model data
    item-model: "model_name"     # Resource pack model (1.21+)
    enchantments:
      SHARPNESS: 5               # Enchantments
      UNBREAKING: 3
  lore:                           # Item description
    - ''
    - '&7Line 1'
    - '&eLine 2'
    - ''
  type: TOOL                      # Item type (TOOL, ARMOR, RUNE, etc.)
  activation-slot: HAND           # Where item activates
  placeable: false                # Can item be placed
  click-cooldown: 500             # Cooldown in milliseconds
  permission: "myplugin.use"      # Required permission
  trigger-actions:                # Actions to perform
    - 'on_event:action'
```

---

## 📝 Commands

### Basic Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/ci give <item> [player]` | Give custom item | `customitems.give` |
| `/ci list` | List all items | `customitems.list` |
| `/ci reload` | Reload all items | `customitems.reload` |
| `/api-item give <id> [player]` | Give Java API item | `customitems.give` |
| `/api-item list` | List Java API items | `customitems.list` |

### Command Examples

```bash
# Give item to yourself
/ci give my_sword

# Give item to another player
/ci give my_sword Steve

# List all items
/ci list

# Reload plugin
/ci reload
```

---

## 🔐 Permissions

### Default Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `customitems.give` | Give custom items | op |
| `customitems.list` | List items | true |
| `customitems.reload` | Reload plugin | op |
| `customitems.admin` | Admin permissions | op |

### Per-Item Permissions

Add custom permissions to restrict item usage:

```yaml
legendary_sword:
  permission: "myplugin.legendary"
  # Only players with this permission can use
```

---

## 🎨 Resource Pack

### Basic Setup

1. **Create resource pack folder structure:**
```
resource-pack/
├── pack.mcmeta
└── assets/
    └── minecraft/
        ├── models/
        │   └── item/
        │       └── my_model.json
        └── textures/
            └── item/
                └── my_model.png
```

2. **pack.mcmeta:**
```json
{
  "pack": {
    "description": "Custom Items Resource Pack",
    "pack_format": 34
  }
}
```

3. **Model file (my_model.json):**
```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "0": "minecraft:item/my_model"
  }
}
```

4. **Use in item config:**
```yaml
my_item:
  item:
    item-model: "my_model"
```

---

## ☕ Java API

### Basic Structure

```java
import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.api.ItemAPI;

public class MySword extends AbstractCustomItem {

    @Override
    public String getId() { return "my_sword"; }

    @Override
    public String getDisplayName() { return "&6My Sword"; }

    @Override
    public Material getMaterial() { return Material.DIAMOND_SWORD; }

    @Override
    public void onRightClick(PlayerInteractEvent e, Player p) {
        ItemAPI.heal(p, 5);
        ItemAPI.effect(p, PotionEffectType.SPEED, 10, 1);
    }
}
```

### Available Methods

| Method | Description |
|--------|-------------|
| `onLeftClick()` | Left click action |
| `onRightClick()` | Right click action |
| `onEquip()` | When equipped |
| `onUnequip()` | When unequipped |
| `onDamageDealt()` | When dealing damage |
| `onDamageTaken()` | When taking damage |
| `onKill()` | When killing entity |
| `onDeath()` | On player death |
| `onBlockBreak()` | Breaking blocks |

### ItemAPI Utilities

| Method | Description |
|--------|-------------|
| `ItemAPI.heal(player, amount)` | Heal player |
| `ItemAPI.effect(player, type, sec, lvl)` | Apply potion effect |
| `ItemAPI.particles(player, particle, count)` | Spawn particles |
| `ItemAPI.sound(player, sound, vol, pitch)` | Play sound |
| `ItemAPI.teleport(player, x, y, z)` | Teleport player |
| `ItemAPI.title(player, title, sub)` | Show title |

---

## 🔧 Troubleshooting

### Common Issues

#### Item Not Appearing

**Problem:** Item doesn't show up after `/ci give`

**Solution:**
1. Check item ID matches: `/ci list`
2. Reload plugin: `/ci reload`
3. Check console for errors

#### Effects Not Working

**Problem:** Particles/effects don't show

**Solution:**
1. Check particle name is correct
2. Verify player has required permissions
3. Check console for error messages

#### Plugin Not Loading

**Problem:** Plugin doesn't appear in `/pl`

**Solution:**
1. Verify Java version (17+)
2. Check plugin.yml exists
3. Look for errors in console

---

## 📞 Support

### Getting Help

1. **Check Documentation** - Read relevant docs
2. **Search Issues** - Look for similar problems
3. **Create Issue** - Report bugs on GitHub
4. **Community** - Join our Discord

### Useful Links

- **GitHub:** https://github.com/animesao/dcustomitems
- **Releases:** https://github.com/animesao/dcustomitems/releases
- **Issues:** https://github.com/animesao/dcustomitems/issues

---

## 📄 License

This plugin is open source under the MIT License.

---

**Version:** 1.320.237  
**Last Updated:** August 2026
