# 🎨 DeluxeMenuX - Advanced Menu System

A complete menu system for a Minecraft server.

> 📦 This folder is a module sample (`EXAMPLE-*`). To enable the module:
> copy it into `items/` and remove the `EXAMPLE-` prefix from the folder name
> (`EXAMPLE-deluxemenux/` → `deluxemenux/`), then `/ci reload`.
> As long as the folder is named `EXAMPLE-*` it is not loaded.

## 📁 Module structure

```
items/deluxemenux/   (after enabling EXAMPLE-deluxemenux/ -> deluxemenux/)
├── config.yml          # Module settings
├── deluxemenux.java    # Main class
├── menu-command.java   # The /menu command
├── kits-command.java   # The /kits command
├── shop-command.java   # The /shop command
├── README.md           # Documentation
└── menus/              # The folder with the menus
    ├── shop.yml        # The shop menu, opened via /shop
    ├── kits.yml        # The kits menu, opened via /kits
    ├── main.yml        # The main menu
    └── ...
```

## 🚀 Quick start

### 1. Create a menu

Create the file `menus/my-menu.yml`:

```yaml
title: "&6My Menu"
size: 27
open-sound: "UI_BUTTON_CLICK"
close-sound: "UI_BUTTON_CLICK"

# Background (fills the empty slots)
fill:
  material: GRAY_STAINED_GLASS_PANE
  name: " "

# Items
items:
  # Slot 13 (the center)
  13:
    material: DIAMOND
    name: "&bDiamond"
    lore:
      - ""
      - "&7Click to receive it"
      - ""
    command: "give %player% diamond 1"
    message: "&aYou received a diamond!"
    sound: "ENTITY_PLAYER_LEVELUP"
    close: false
```

### 2. Open the menu

```
/menu my-menu
```

## 📋 YAML format

### Main settings

```yaml
title: "&6Header"    # The menu title (with colors)
size: 27                # Size (9, 18, 27, 36, 45, 54)
open-sound: "UI_BUTTON_CLICK"  # The open sound
close-sound: "UI_BUTTON_CLICK" # The close sound
```

### Background (fill)

```yaml
fill:
  material: GRAY_STAINED_GLASS_PANE
  name: " "
  lore:
    - "&7Description"
```

### Items (items)

```yaml
items:
  # The slot number
  13:
    material: DIAMOND           # Material
    name: "&bName"         # Name (with colors)
    lore:                       # Description
      - "&7Line 1"
      - "&7Line 2"
    amount: 1                   # Amount
    data: 0                     # Data value (for older versions)
    command: "give %player% diamond 1"  # Command
    message: "&aMessage"     # Message to the player
    permission: "my.permission" # Permissions
    sound: "ENTITY_PLAYER_LEVELUP"  # Sound
    close: false                # Close the menu after the click
```

## 🎯 Menu examples

### Shop

```yaml
title: "&6🛒 Shop"
size: 45
open-sound: "UI_BUTTON_CLICK"

fill:
  material: GRAY_STAINED_GLASS_PANE
  name: " "

items:
  # Header
  4:
    material: PLAYER_HEAD
    name: "&6&lShop"
    lore:
      - ""
      - "&7Choose a product"
      - ""

  # Products
  19:
    material: DIAMOND
    name: "&bDiamond"
    lore:
      - ""
      - "&7Price: &a100$"
      - ""
    command: "give %player% diamond 1"
    message: "&aYou bought a diamond!"
    sound: "ENTITY_PLAYER_LEVELUP"

  22:
    material: EMERALD
    name: "&aEmerald"
    lore:
      - ""
      - "&7Price: &a150$"
      - ""
    command: "give %player% emerald 1"
    message: "&aYou bought an emerald!"
    sound: "ENTITY_PLAYER_LEVELUP"

  25:
    material: GOLD_INGOT
    name: "&6Gold"
    lore:
      - ""
      - "&7Price: &a50$"
      - ""
    command: "give %player% gold_ingot 1"
    message: "&aYou bought gold!"
    sound: "ENTITY_PLAYER_LEVELUP"

  # Close
  40:
    material: BARRIER
    name: "&cClose"
    lore:
      - ""
      - "&7Close the menu"
      - ""
    close: true
    sound: "UI_BUTTON_CLICK"
```

### Navigation between menus

```yaml
title: "&6Main Menu"
size: 27

fill:
  material: BLACK_STAINED_GLASS_PANE
  name: " "

items:
  11:
    material: EMERALD_BLOCK
    name: "&a&lShop"
    lore:
      - ""
      - "&7Open the shop"
      - ""
    command: "menu shop"
    sound: "UI_BUTTON_CLICK"

  13:
    material: CHEST
    name: "&b&lInventory"
    lore:
      - ""
      - "&7Management"
      - ""
    command: "ci list"
    sound: "UI_BUTTON_CLICK"

  15:
    material: COMMAND_BLOCK
    name: "&e&lCommands"
    lore:
      - ""
      - "&7A list of the commands"
      - ""
    command: "help"
    sound: "UI_BUTTON_CLICK"
```

## 🎮 Commands

| Command | Description |
|---------|-------------|
| `/menu` | Open the main menu |
| `/menu <id>` | Open a menu by ID |
| `/menu list` | A list of all the menus |
| `/menu reload` | Reload the menus |
| `/kits` | Open the kits menu `menus/kits.yml` |
| `/kit` | An alias of the `/kits` command |
| `/shop` | Open the shop `menus/shop.yml` |

Permissions:

```text
deluxemenux.use       # /menu, /kits and /shop
deluxemenux.admin     # /menu reload
deluxemenux.kit.start # the starter kit
deluxemenux.kit.iron  # the iron kit
deluxemenux.kit.diamond
deluxemenux.shop.diamond
deluxemenux.shop.emerald
deluxemenux.shop.gold
```

The `kits.yml` and `shop.yml` files are created automatically only when they are missing. If you change the YAML, `/ci reload` will not overwrite it.

## 🔧 Placeholders

| Placeholder | Description |
|-------------|-------------|
| `%player%` | The player's name |
| `%player_name%` | The player's name |
| `%health%` | Health |
| `%food%` | Food |
| `%level%` | Level |

## 📝 Command examples

```yaml
# Give an item
command: "give %player% diamond 1"

# Teleportation
command: "tp %player% 100 64 100"

# Effect
command: "effect give %player% speed 30 1"

# Open another menu
command: "menu shop"

# Announcement
command: "bc %player% bought a diamond!"

# Toggle flight
command: "fly %player%"
```

## 🎨 Color codes

| Code | Color |
|-----|------|
| `&0` | Black |
| `&1` | Dark Blue |
| `&2` | Dark Green |
| `&3` | Dark Aqua |
| `&4` | Dark Red |
| `&5` | Purple |
| `&6` | Gold |
| `&7` | Gray |
| `&8` | Dark Gray |
| `&9` | Blue |
| `&a` | Green |
| `&b` | Aqua |
| `&c` | Red |
| `&d` | Pink |
| `&e` | Yellow |
| `&f` | White |
| `&l` | Bold |
| `&o` | Italic |
| `&n` | Underlined |
| `&k` | Obfuscated |
| `&r` | Reset |

## ⚡ Tips

1. **Menu size**: Use 9, 18, 27, 36, 45 or 54
2. **Slots**: They are numbered from 0 (top-left) to size-1 (bottom-right)
3. **Commands**: Use `%player%` to substitute the name
4. **Sounds**: The full list: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html
5. **Materials**: The full list: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html
