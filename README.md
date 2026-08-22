<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.11-green?style=for-the-badge&logo=minecraft" alt="Minecraft">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/Version-1.320.282-blue?style=for-the-badge" alt="Version">
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
| 🔗 PlaceholderAPI | 25+ exported placeholders |

---

## 📦 Installation

### Quick Install

```bash
# Download latest release
cd ~/server/plugins
curl -L -o dcustomitems.jar https://github.com/animesao/dcustomitems/releases/download/v1.320.282/DC-CustomItems-1.320.282.jar

# Restart server
./restart.sh
```

### Manual Install

1. Download `DC-CustomItems-1.320.282.jar` from [Releases](https://github.com/animesao/dcustomitems/releases/tag/v1.320.282)
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

## 🔗 PlaceholderAPI Integration

DC-CustomItems supports [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) for exporting placeholders to other plugins.

### Setup

1. Install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) on your server
2. Place `DC-CustomItems.jar` in `plugins/`
3. Restart server — placeholders are registered automatically

### Available Placeholders

All placeholders use the prefix `%dci_` (or `%dcustomitems_`).

#### Player Info

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%dci_player%` | Player name | `Steve` |
| `%dci_player_uuid%` | Player UUID | `8667ba71-b85a-4004-af54-457a9734eed7` |
| `%dci_display_name%` | Display name | `§6Steve` |
| `%dci_health%` | Current health | `20` |
| `%dci_max_health%` | Max health | `20` |
| `%dci_health_percent%` | Health percentage | `100` |
| `%dci_food%` | Food level | `20` |
| `%dci_saturation%` | Saturation | `5` |
| `%dci_level%` | XP level | `30` |
| `%dci_exp%` | XP percentage | `75` |
| `%dci_x%`, `%dci_y%`, `%dci_z%` | Coordinates | `100`, `64`, `-200` |
| `%dci_world%` | World name | `world` |
| `%dci_gamemode%` | Game mode | `SURVIVAL` |
| `%dci_fly_status%` | Fly status | `on` / `off` |
| `%dci_item_in_hand%` | Item in main hand | `DIAMOND_SWORD` |
| `%dci_online%` | Online players | `15` |
| `%dci_max_players%` | Max players | `50` |

#### Plugin Info

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%dci_version%` | Plugin version | `1.320.282` |
| `%dci_item_count%` | Total YAML items loaded | `45` |
| `%dci_java_item_count%` | Java API items loaded | `6` |
| `%dci_command_count%` | Custom commands registered | `3` |
| `%dci_placeholder_count%` | Custom placeholders registered | `12` |
| `%dci_module_count%` | Modules loaded | `2` |
| `%dci_database_type%` | Database type | `sqlite` / `mysql` |
| `%dci_active_effects%` | Active effects from custom items | `SPEED:II, STRENGTH:I` |
| `%dci_active_effects_count%` | Number of active effects | `3` |

#### Item-Specific Placeholders (Dynamic)

These placeholders work with **any** custom item ID. Replace `<item_id>` with the actual item ID (e.g., `vampire-blade`, `rune-1`, `netherite-helmet`).

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%dci_has_<item_id>%` | `true` if player has item in inventory | `%dci_has_vampire-blade%` → `true` |
| `%dci_holding_<item_id>%` | `true` if player is holding item (main/off hand) | `%dci_holding_rune-1%` → `false` |
| `%dci_equipped_<item_id>%` | `true` if item is in correct activation slot | `%dci_equipped_netherite-helmet%` → `true` |
| `%dci_item_amount_<item_id>%` | Count of item in inventory | `%dci_item_amount_health-potion%` → `3` |

**Examples:**
```
# Check if player has Vampire Blade
%dci_has_vampire-blade%

# Check if player is holding the Thunder Axe
%dci_holding_thunder-axe%

# Check if Dark Lord helmet is worn
%dci_equipped_netherite-helmet%

# How many Health Potions does player have?
%dci_item_amount_health-potion%
```

**Conditional messages:**
```yaml
# Show message only if player has the item
message: "%if_%dci_has_vampire-blade%_=_true_&cYou have the Vampire Blade!%"

# Show equipped item name
tablist:
  footer: "&7Weapon: %dci_holding_vampire-blade%"
```

### Usage Examples

**Scoreboard:**
```yaml
objective:
  display:
    slot: sidebar
  entries:
    - "&6&lMy Server"
    - "&7Health: &c%dci_health%/%dci_max_health%"
    - "&7Level: &a%dci_level%"
    - "&7World: &b%dci_world%"
    - "&7Online: &e%dci_online%/%dci_max_players%"
```

**Tab list:**
```yaml
tablist:
  header: "&6Welcome, %dci_player%!"
  footer: "&7Version: %dci_version% | Online: %dci_online%"
```

**Chat format:**
```yaml
chat-format: "&7[&e%dci_level%&7] %dci_display_name%&7: %message%"
```

**Conditional messages (with PlaceholderAPI):**
```yaml
# Show different messages based on health
message: "%if_%dci_health%_>_10_&aYou are healthy!_&cYou are hurt!%"
```

### PAPI in Plugin Messages

PlaceholderAPI placeholders work **automatically** in all plugin messages:
- Equip/unequip messages
- Cooldown messages
- Click action messages (message, title, actionbar, broadcast)
- Trigger actions
- Command responses

**Example — item with PAPI placeholders:**
```yaml
legendary-sword:
  item:
    type: DIAMOND_SWORD
    title: '&6[⚔] &fLegendary Sword'
  type: TOOL
  activation-slot: HAND
  equip-message: '&a Equipped! Health: &c%dci_health%/%dci_max_health%'
  unequip-message: '&cUnequipped! Level: &a%dci_level%'
  cooldown-message: '&cCooldown: &e{seconds}s &7| &bHP: &c%dci_health%'
  trigger-actions:
    - 'on_kill:message:&6 Killed! Your kills: %dci_has_vampire-blade%'
    - 'on_kill:announce:&e %dci_player% got a kill!'
```

**Example — scoreboard with live data:**
```yaml
scoreboard:
  title: '&6&lMy Server'
  lines:
    - '&7Health: &c%dci_health%/%dci_max_health%'
    - '&7Level: &a%dci_level%'
    - '&7World: &b%dci_world%'
    - '&7Online: &e%dci_online%/%dci_max_players%'
    - '&7Version: &f%dci_version%'
```

### Custom Placeholders via Java API

You can create your own placeholders with the Java API:

```java
import me.dcplugin.dcustomitems.api.placeholders.CustomPlaceholder;

public class KillsPlaceholder extends CustomPlaceholder {
    
    public KillsPlaceholder() {
        super("kills"); // %dci_kills%
    }
    
    @Override
    public String getValue(Player player) {
        // Query your database
        return String.valueOf(getPlugin().getDatabaseManager().queryInt(
            "SELECT kills FROM stats WHERE uuid=?",
            player.getUniqueId().toString()
        ));
    }
}
```

Place in `plugins/DC-CustomItems/items/` and restart. Available as `%dci_kills%`.

### Included Placeholder Examples

The plugin ships with several ready-to-use placeholder examples in `items/`:

| File | Placeholder | Description |
|------|-------------|-------------|
| `EXAMPLE-kills-placeholder.java` | `%kills%` | Kill counter (in-memory) |
| `EXAMPLE-playtime-placeholder.java` | `%playtime%` | Playtime with formatted output |
| `EXAMPLE-streak-placeholder.java` | `%streak%` | Daily login streak counter |
| `EXAMPLE-balance-placeholder.java` | `%balance%` | Virtual currency balance |
| `EXAMPLE-combo-placeholder.java` | `%combo%` | Kill combo counter with multipliers |
| `EXAMPLE-status-placeholder.java` | `%rank%` | Player rank system |

**Quick start:**
```bash
# Remove EXAMPLE- prefix to activate
mv items/EXAMPLE-kills-placeholder.java items/kills-placeholder.java
mv items/EXAMPLE-playtime-placeholder.java items/playtime-placeholder.java

# Reload
/ci reload

# Use in scoreboard, chat, etc.
# %kills% → "42"
# %playtime% → "2ч 15м 30с"
# %streak% → "5"
# %balance% → "1,250"
# %combo% → "x5"
# %rank% → "VIP"
```

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

## 💰 Vault Economy Integration

DC-CustomItems supports [Vault](https://www.spigotmc.org/resources/vault.34315/) for buying and selling custom items.

### Setup

1. Install [Vault](https://www.spigotmc.org/resources/vault.34315/)
2. Install an economy plugin (EssentialsX, ChargeMe, etc.)
3. Add `buy-price` and/or `sell-price` to your item YAML

### Item YAML Example

```yaml
my-sword:
  item:
    type: DIAMOND_SWORD
    title: '&6Diamond Sword'
  type: TOOL
  activation-slot: HAND
  buy-price: 1000    # Can buy for $1,000
  sell-price: 500    # Can sell for $500
  effects:
    - 'SPEED:1'
```

### Commands

| Command | Description |
|---------|-------------|
| `/ci buy <id>` | Buy 1 item |
| `/ci buy <id> <amount>` | Buy multiple (1-64) |
| `/ci sell <id>` | Sell 1 item from inventory |
| `/ci sell <id> <amount>` | Sell multiple |

### Price Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `buy-price` | double | -1 (disabled) | Cost to buy one item |
| `sell-price` | double | -1 (disabled) | Money received for selling one item |

### Example Output

```
/dci buy vampire-blade
→ [DCI] You bought vampire-blade for $1,000!

/dci sell vampire-blade 3
→ [DCI] You sold vampire-blade x3 for $1,500!

/dci buy vampire-blade
→ [DCI] Not enough money! Need: $1,000 | You have: $500
```

### Vault Placeholders

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%vault_balance%` | Player balance | `$1,250` |
| `%vault_balance_formatted%` | Formatted balance | `$1,250.00` |

---

## 🛠️ Requirements

- Minecraft 1.21.11+
- Paper/Spigot
- Java 17+
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (optional — for placeholder export)
- [Vault](https://www.spigotmc.org/resources/vault.34315/) + economy plugin (optional — for buy/sell)

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
