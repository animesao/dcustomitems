<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.8-green?style=for-the-badge&logo=minecraft" alt="Minecraft">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/Version-1.320.211-blue?style=for-the-badge" alt="Version">
  <img src="https://img.shields.io/github/license/animesao/dcustomitems-purple?style=for-the-badge" alt="License">
</p>

<h1 align="center">⚔️ DC-CustomItems</h1>

<p align="center">
  <b>Powerful custom items plugin for Minecraft</b><br>
  Effects, attributes, armor sets, triggers and more!
</p>

<p align="center">
  <a href="#installation">Installation</a> •
  <a href="#commands">Commands</a> •
  <a href="#configuration">Configuration</a> •
  <a href="#features">Features</a> •
  <a href="#documentation">Documentation</a>
</p>

---

## Features

| Feature | Description |
|---------|-------------|
| 🗡️ Custom Items | Runes, tools, armor, consumables |
| ⚡ Potion Effects | Auto-apply on equip |
| 📊 Attributes | Damage, speed, armor and more |
| 🛡️ Armor Sets | Bonuses for full sets |
| 🎯 Click Actions | Lightning, commands, effects, particles, sounds |
| 🔄 Triggers | Auto-actions on events |
| 📦 Per-Item Files | Each item in separate YAML |
| 🎨 CustomModelData | Resource pack support |
| 🔐 Permissions | Per-item permissions |
| ✨ Particles & Sounds | Equip/unequip effects |

---

## Installation

1. Download the latest release from [GitHub Releases](https://github.com/animesao/dcustomitems/releases/latest)
2. Place `DC-CustomItems.jar` in your `plugins/` folder
3. Restart the server
4. Configure items in `plugins/DC-CustomItems/items/`

---

## Commands

| Command | Description |
|---------|-------------|
| `/customitems list` | List all items |
| `/customitems give <id> [player]` | Give item |
| `/customitems reload` | Reload config |
| `/customitems update` | Check updates |

---

## Configuration

### File Structure

```
plugins/DC-CustomItems/
├── config.yml              # Main settings
├── messages.yml            # Plugin messages
└── items/                  # Items folder
    ├── shadow-blade.yml
    ├── vampire-blade.yml
    ├── thunder-axe.yml
    └── ... (10 items)
```

### Item Example

```yaml
my-sword:
  type: TOOL
  activation-slot: HAND
  placeable: false
  permission: my.sword

  item:
    type: NETHERITE_SWORD
    title: '&6My Sword'
    glowing: true
    unbreakable: true
    enchantments:
      SHARPNESS: 5

  effects:
    - 'INCREASE_DAMAGE:3'
    - 'SPEED:2'

  trigger-actions:
    # On equip
    - 'on_equip:particle:FLAME:20'
    - 'on_equip:message:&aSword equipped!'
    
    # On unequip
    - 'on_unequip:message:&cSword unequipped.'
    
    # On right click
    - 'on_click_right:lightning:3'
    - 'on_click_right:particle:FLAME:30'
    - 'on_click_right:message:&6Lightning called!'
    
    # On left click (4x damage like Evoker!)
    - 'on_click_left:effect:INCREASE_DAMAGE:8:4'
    - 'on_click_left:lightning:2'
    - 'on_click_left:knockback:3'
    
    # Auto triggers
    - 'on_kill:heal:20'
    - 'on_jump:effect:SPEED:3:2'
```

---

## Triggers

| Trigger | Description |
|---------|-------------|
| `on_equip` | On equip item |
| `on_unequip` | On unequip item |
| `on_click_right` | On right click |
| `on_click_left` | On left click |
| `on_kill` | On kill mob/player |
| `on_death` | On player death |
| `on_damage_taken` | On damage taken |
| `on_damage_dealt` | On damage dealt |
| `on_jump` | On jump |
| `on_pickup` | On pickup item |
| `on_drop` | On drop item |

---

## Actions

### 📢 Communication
- `message` - Send message to player
- `announce` - Broadcast message
- `title` - Title on screen
- `actionbar` - Text above hotbar

### ⚡ Effects
- `effect:TYPE:SECONDS:LEVEL` - Potion effect

### 💥 Combat
- `lightning:COUNT` - Lightning strike
- `damage:AMOUNT` - Deal damage
- `heal:AMOUNT` - Heal player
- `stun:SECONDS` - Stun enemy
- `knockback:POWER` - Knockback enemy
- `launch:POWER` - Launch enemy up

### 🎁 Items
- `give:MATERIAL:COUNT` - Give item
- `remove:MATERIAL:COUNT` - Remove item
- `exp:XP:LEVELS` - Give experience

### 🌍 World
- `teleport:X:Y:Z` - Teleport
- `sethealth:VALUE` - Set health
- `setfood:VALUE` - Set food

### 🎆 Effects
- `particle:TYPE:COUNT` - Particles
- `sound:TYPE:VOLUME:PITCH` - Sound
- `fireworks:1` - Spawn firework
- `vanish:SECONDS` - Invisibility
- `glow:SECONDS` - Glowing

### 🔧 Commands
- `command:COMMAND` - Execute command

### ⏱️ Cooldown
- `cooldown:MS` - Set cooldown in milliseconds

---

## Documentation

📄 **[Full Documentation (Russian)](docs.md)**

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for information on how to contribute.

---

## License

This project is licensed under the MIT License - see [LICENSE](LICENSE) for details.

---

## Authors

- **DC-CustomItems** - [GitHub](https://github.com/animesao/dcustomitems)
