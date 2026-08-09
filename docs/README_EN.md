# ⚔️ DC-CustomItems - Full Documentation

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.11-green?style=for-the-badge&logo=minecraft" alt="Minecraft">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/Version-1.320.228-blue?style=for-the-badge" alt="Version">
  <img src="https://img.shields.io/badge/Paper-1.21.11-red?style=for-the-badge" alt="Paper">
</p>

**DC-CustomItems** is a powerful Minecraft plugin for creating custom items with effects, attributes, triggers, and resource pack support.

---

## 📑 Table of Contents

- [Installation](#installation)
- [Configuration](#configuration)
- [Commands](#commands)
- [Permissions](#permissions)
- [Item Configuration](#item-configuration)
- [Item Model System](#item-model-system)
- [Triggers](#triggers)
- [Actions](#actions)
- [Effects](#effects)
- [Attributes](#attributes)
- [Examples](#examples)
- [Troubleshooting](#troubleshooting)

---

## 📦 Installation

### Step 1: Download

Download the latest release from [GitHub Releases](https://github.com/animesao/dcustomitems/releases/latest)

### Step 2: Install

```bash
# Copy to plugins folder
cp DC-CustomItems-1.320.223.jar /path/to/server/plugins/

# Or using curl on VDS
cd ~/server/plugins
curl -L -o dcustomitems.jar https://github.com/animesao/dcustomitems/releases/download/v1.320.223/DC-CustomItems-1.320.223.jar
```

### Step 3: Restart Server

```bash
# Restart your Minecraft server
./restart.sh
# or
systemctl restart minecraft
```

### Step 4: Configure Items

Items are stored in `plugins/DC-CustomItems/items/` folder.

---

## 📁 Configuration

### File Structure

```
plugins/DC-CustomItems/
├── config.yml              # Main plugin settings
├── messages.yml            # Plugin messages
└── items/                  # Items folder
    ├── vampire-blade.yml
    ├── shadow-blade.yml
    ├── thunder-axe.yml
    ├── frost-wand.yml
    └── ... (12 items included)
```

---

## 🎮 Commands

| Command | Aliases | Description |
|---------|---------|-------------|
| `/ci list` | `/customitems list` | List all custom items |
| `/ci give <id>` | `/customitems give <id>` | Give item to yourself |
| `/ci give <id> <player>` | `/customitems give <id> <player>` | Give item to player |
| `/ci reload` | `/customitems reload` | Reload all items |
| `/ci update` | `/customitems update` | Check for updates |

### Examples

```bash
# Give vampire-blade to yourself
/ci give vampire-blade

# Give shadow-blade to a player
/ci give shadow-blade Steve

# List all items
/ci list

# Reload plugin
/ci reload
```

---

## 🔐 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `customitems.use` | Basic usage | op |
| `customitems.give` | Give items | op |
| `customitems.reload` | Reload plugin | op |
| `customitems.update` | Check updates | op |
| `customitems.*` | All permissions | op |

---

## 📝 Item Configuration

### Basic Item Structure

```yaml
item-id:
  type: TOOL                    # RUNE, TOOL, ARMOR, CONSUMABLE
  activation-slot: HAND         # HAND, OFFHAND, HEAD, CHEST, LEGS, FEET
  placeable: false              # Can item be placed?
  permission: custom.permission # Required permission
  
  item:
    type: NETHERITE_SWORD       # Material
    title: '&6My Sword'         # Display name
    glowing: true               # Enchantment glow
    unbreakable: true           # Unbreakable
    item-model: "smoke"         # Resource pack model (1.21.11+)
    
    enchantments:
      SHARPNESS: 5
      UNBREAKING: 3
    
    item-flags:
      - HIDE_ENCHANTS
      - HIDE_UNBREAKABLE
  
  lore:
    - ''
    - ' &7Custom description'
    - ''
  
  effects:
    - 'INCREASE_DAMAGE:3'
    - 'SPEED:2'
  
  attributes:
    GENERIC_ATTACK_DAMAGE: 10.0
    GENERIC_ATTACK_SPEED: 1.6
  
  click-cooldown: 1000          # Cooldown in milliseconds
  
  triggers:
    on_equip:
      - 'particles:FLAME:20'
    on_kill:
      - 'heal:20'
```

---

## 🎨 Item Model System (Minecraft 1.21.11+)

### Overview

The plugin supports Minecraft's new `custom_model_data` component with strings (1.21.4+).

### Supported Formats

| Format | Example | Description |
|--------|---------|-------------|
| Short | `"smoke"` | Auto-converts to `minecraft:item/smoke` |
| Full Path | `"minecraft:item/smoke"` | Direct path |
| Custom NS | `"myplugin:weapons/sword"` | Custom namespace |

### Configuration

```yaml
my-item:
  item:
    type: NETHERITE_SWORD
    # All formats work:
    item-model: "smoke"
    # item-model: "minecraft:item/smoke"
    # item-model: "myplugin:custom_sword"
```

### Resource Pack Structure

```
rp/assets/minecraft/
├── items/
│   └── netherite_sword.json    # Model selection
├── models/item/
│   ├── smoke.json              # 3D model
│   ├── custom_sword.json       # Another model
│   └── ...
└── textures/item/
    ├── smoke.png               # Texture
    ├── custom_sword.png        # Another texture
    └── ...
```

### Items Definition (items/netherite_sword.json)

```json
{
  "model": {
    "type": "minecraft:select",
    "property": "minecraft:custom_model_data",
    "index": 0,
    "cases": [
      {
        "when": "smoke",
        "model": { "model": "minecraft:item/smoke" }
      },
      {
        "when": "minecraft:item/smoke",
        "model": { "model": "minecraft:item/smoke" }
      }
    ],
    "fallback": { "model": "minecraft:item/netherite_sword" }
  }
}
```

### Commands

```bash
# Using custom_model_data with strings
/give @s minecraft:netherite_sword[minecraft:custom_model_data={strings:["smoke"]}]

# Using item_model component (alternative)
/give @s minecraft:netherite_sword[item_model="minecraft:item/smoke"]
```

---

## 🔄 Triggers

| Trigger | Description |
|---------|-------------|
| `on_equip` | When item is equipped |
| `on_unequip` | When item is unequipped |
| `on_click_right` | Right click with item |
| `on_click_left` | Left click with item |
| `on_kill` | Kill mob/player |
| `on_death` | Player death |
| `on_damage_taken` | Take damage |
| `on_damage_dealt` | Deal damage |
| `on_jump` | Jump |
| `on_pickup` | Pick up item |
| `on_drop` | Drop item |

---

## ⚡ Actions

### 📢 Communication

| Action | Format | Description |
|--------|--------|-------------|
| `message` | `message:Text` | Send message to player |
| `title` | `title:Main:Sub:fadeIn:stay:fadeOut` | Title on screen |
| `actionbar` | `actionbar:Text` | Text above hotbar |
| `broadcast` | `broadcast:Text` | Broadcast to all players |

**Placeholders:** `%player%` - Player name

### ⚡ Effects

| Action | Format | Description |
|--------|--------|-------------|
| `effect` | `effect:TYPE:SECONDS:LEVEL` | Apply potion effect |
| `speed` | `speed:SECONDS:LEVEL` | Speed (short) |
| `vanish` | `vanish:SECONDS` | Invisibility |
| `glow` | `glow:SECONDS` | Glowing |
| `flight` | `flight:true/false` | Toggle flight |

### 💥 Combat

| Action | Format | Description |
|--------|--------|-------------|
| `heal` | `heal:AMOUNT` | Heal self |
| `heal_nearby` | `heal_nearby:AMOUNT:RADIUS` | Heal nearby players |
| `damage` | `damage:AMOUNT` | Damage self |
| `damage_nearby` | `damage_nearby:DMG:RADIUS` | AoE damage |
| `damage_mobs` | `damage_mobs:DMG:RADIUS` | Damage mobs only |
| `damage_players` | `damage_players:DMG:RADIUS` | Damage players only |
| `lightning` | `lightning:COUNT` | Lightning at player |
| `lightning_forward` | `lightning_forward:DISTANCE` | Lightning ahead |
| `knockback` | `knockback:RADIUS` | Knockback nearby |
| `launch` | `launch:POWER` | Launch nearby up |
| `stun` | `stun:SECONDS:RADIUS` | Stun nearby |

### 🎁 Items

| Action | Format | Description |
|--------|--------|-------------|
| `give` | `give:MATERIAL:COUNT` | Give item |
| `remove` | `remove:MATERIAL:COUNT` | Remove item |
| `exp` | `exp:XP:LEVELS` | Give experience |

### 🌍 World

| Action | Format | Description |
|--------|--------|-------------|
| `teleport` | `teleport:X:Y:Z` | Teleport |
| `teleport_relative` | `teleport_relative:X:Y:Z` | Relative teleport |
| `sethealth` | `sethealth:VALUE` | Set health |
| `setfood` | `setfood:VALUE` | Set food |

### 🎆 Effects

| Action | Format | Description |
|--------|--------|-------------|
| `particles` | `particles:TYPE:COUNT` | Spawn particles |
| `sound` | `sound:TYPE:VOLUME:PITCH` | Play sound |
| `fireworks` | `fireworks:1` | Spawn firework |

### 🔧 Commands

| Action | Format | Description |
|--------|--------|-------------|
| `command` | `command:COMMAND` | Execute command as player |
| `console_command` | `console_command:COMMAND` | Execute command as console |

---

## 💊 Effects

### Available Effects

| Effect | Name |
|--------|------|
| SPEED | Speed |
| SLOW | Slowness |
| INCREASE_DAMAGE | Strength |
| HEAL | Instant Health |
| HASTE | Haste |
| JUMP | Jump Boost |
| REGENERATION | Regeneration |
| RESISTANCE | Resistance |
| FIRE_RESISTANCE | Fire Resistance |
| WATER_BREATHING | Water Breathing |
| INVISIBILITY | Invisibility |
| NIGHT_VISION | Night Vision |
| HEALTH_BOOST | Health Boost |
| ABSORPTION | Absorption |
| SATURATION | Saturation |

### Format

```
effect:TYPE:SECONDS:LEVEL
```

**Examples:**
```yaml
- 'effect:SPEED:10:2'          # Speed II for 10 seconds
- 'effect:INCREASE_DAMAGE:5:3' # Strength III for 5 seconds
- 'effect:REGENERATION:15:1'   # Regeneration I for 15 seconds
```

---

## 📊 Attributes

### Available Attributes

| Attribute | Description |
|-----------|-------------|
| GENERIC_ATTACK_DAMAGE | Attack damage |
| GENERIC_ATTACK_SPEED | Attack speed |
| GENERIC_MAX_HEALTH | Max health |
| GENERIC_MOVEMENT_SPEED | Movement speed |
| GENERIC_ARMOR | Armor |
| GENERIC_ARMOR_TOUGHNESS | Armor toughness |
| GENERIC_KNOCKBACK_RESISTANCE | Knockback resistance |
| GENERIC_LUCK | Luck |

### Format

```yaml
attributes:
  GENERIC_ATTACK_DAMAGE: 10.0
  GENERIC_ATTACK_SPEED: 1.6
```

---

## 📦 Included Items

### ⚔️ Weapons & Armor

| ID | Name | Type | Description |
|----|------|------|-------------|
| `vampire-blade` | Vampire Blade | Sword | Life steal, AoE damage |
| `shadow-blade` | Shadow Blade | Sword | Critical strikes |
| `thunder-axe` | Thunder Axe | Axe | Lightning attacks |
| `frost-wand` | Frost Wand | Wand | Ice spells |
| `elemental-staff` | Elemental Staff | Staff | Multiple elements |
| `berserker-axe` | Berserker Axe | Axe | Rage mode |
| `healer-amulet` | Healer Amulet | Amulet | Healing abilities |
| `nature-totem` | Nature Totem | Totem | Nature powers |
| `archer-bow` | Archer Bow | Bow | Special arrows |
| `shadow-helmet` | Shadow Helmet | Armor | Stealth mode |
| `artifact-blade-of-destiny` | Blade of Destiny | Sword | Ultimate weapon |
| `artifact-chaos-orb` | Chaos Orb | Orb | Random effects |

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

## 🚀 Advanced Actions (No Limits!)

### 🎨 Custom Particles
```yaml
# Format: particles_custom:TYPE:COUNT:X:Y:Z:OFF_X:OFF_Y:OFF_Z
- 'particles_custom:FLAME:100:0:1:0:2:2:2'
```

### 🔊 Sound Sequence
```yaml
# Format: sound_sequence:SOUND1:VOL1:PITCH1;SOUND2:...
- 'sound_sequence:ENTITY_PLAYER_LEVELUP:1:1;ENTITY_ORB_PICKUP:0.5:1.5'
```

### 🎬 Title Sequence (Animation!)
```yaml
# Format: title_sequence:TITLE:SUB:IN:STAY:OUT;...
- 'title_sequence:🔥 READY!:::10:20:10;🔥 FIRE!:::10:30:10;🔥 STORM!:::10:40:10'
```

### 📜 Command Sequence with Delays
```yaml
# Format: command_sequence:CMD1:DELAY1;CMD2:DELAY2;...
- 'command_sequence:give %player% diamond 1:20;say Hi!:40'
```

### 💥 Custom Damage (Precise Area)
```yaml
# Format: damage_custom:DAMAGE:WIDTH:HEIGHT:DEPTH
- 'damage_custom:30:6:4:6'  # 30 damage in 6x4x6 area
```

### ❤️ Custom Heal with Particles
```yaml
# Format: heal_custom:HEAL:W:H:D:PARTICLE
- 'heal_custom:15:8:5:8:HEART'  # Heal allies in 8x5x8
```

---

## 💡 Examples

### Example 1: Simple Sword

```yaml
my-sword:
  type: TOOL
  activation-slot: HAND
  placeable: false
  
  item:
    type: DIAMOND_SWORD
    title: '&bDiamond Sword'
    glowing: true
    enchantments:
      SHARPNESS: 3
  
  effects:
    - 'SPEED:1'
```

### Example 2: Full Featured Item

```yaml
legendary-sword:
  type: TOOL
  activation-slot: HAND
  placeable: false
  permission: legendary.sword
  
  item:
    type: NETHERITE_SWORD
    title: '&6[⚔] &fLegendary Sword'
    glowing: true
    unbreakable: true
    item-model: "smoke"
    enchantments:
      SHARPNESS: 5
      UNBREAKING: 3
  
  lore:
    - ''
    - ' &7Legendary weapon of heroes'
    - ''
    - ' &eRight Click: Lightning Strike'
    - ' &eLeft Click: AoE Damage'
    - ''
  
  effects:
    - 'INCREASE_DAMAGE:3'
    - 'SPEED:2'
  
  attributes:
    GENERIC_ATTACK_DAMAGE: 12.0
    GENERIC_ATTACK_SPEED: 1.8
  
  click-cooldown: 2000
  
  triggers:
    on_equip:
      - 'particles:FLAME:30'
      - 'sound:ENTITY_PLAYER_LEVELUP:1:1'
      - 'message:&6⚔ Legendary Sword equipped!'
    
    on_click_right:
      - 'lightning_forward:100'
      - 'particles:EXPLOSION_LARGE:20'
      - 'title:&6⚡ LIGHTNING!::&7Beware!'
    
    on_click_left:
      - 'damage_nearby:10:4'
      - 'knockback:3'
    
    on_kill:
      - 'heal:20'
      - 'effect:REGENERATION:10:2'
      - 'particles:HEART:20'
```

---

## 🔧 Troubleshooting

### Item Model Not Working

1. **Check resource pack is installed:**
   - Place `SmokeSword_ResourcePack.zip` in `%appdata%\.minecraft\resourcepacks\`
   - Activate in Options → Resource Packs

2. **Reload resources:**
   - Press `F3 + T` in game

3. **Check format:**
   - Use `item-model: "smoke"` in config
   - Ensure `items/netherite_sword.json` has matching `when` value

### Plugin Not Loading

1. **Check Java version:** Requires Java 17+
2. **Check Paper version:** Requires Paper 1.21.11+
3. **Check logs:** Look for errors in `logs/latest.log`

### Commands Not Working

1. **Check permissions:** Ensure you have `customitems.use` permission
2. **Check item ID:** Use `/ci list` to see available items

---

## 📚 Additional Resources

- [GitHub Repository](https://github.com/animesao/dcustomitems)
- [Issue Tracker](https://github.com/animesao/dcustomitems/issues)
- [Releases](https://github.com/animesao/dcustomitems/releases)

---

## 📄 License

This project is licensed under the MIT License.

---

**Made with ❤️ by animesao**
