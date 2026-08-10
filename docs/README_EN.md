# DC-CustomItems Documentation

## 📚 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Java Items](#java-items)
- [Java Commands](#java-commands)
- [Java Placeholders](#java-placeholders)
- [Messages Config](#messages-config)
- [Commands](#commands)
- [Support](#support)

---

## 📖 Overview

DC-CustomItems is a plugin for Minecraft servers (Spigot/Paper 1.20+) that allows you to create custom items, commands, and placeholders using pure Java code.

**Everything is hot-reloadable!** Just drop a .java file in the `items/` folder and run `/ci reload`.

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| **Java Items** | Create custom items with abilities |
| **Java Commands** | Create custom commands |
| **Java Placeholders** | Create custom placeholders |
| **Hot Reload** | Reload without server restart |
| **Database** | SQLite support for data storage |
| **Custom Messages** | Edit all plugin messages |

---

## 🚀 Installation

```bash
cd ~/plugins
curl -L -o dcustomitems.jar https://github.com/animesao/dcustomitems/releases/latest/download/DC-CustomItems.jar
```

Restart server or run `/ci reload`.

---

## 🎮 Quick Start

### Step 1: Create an Item

Create `my-sword.java` in `plugins/DC-CustomItems/items/`:

```java
import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class MySword extends AbstractCustomItem {
    
    @Override
    public String getId() { return "my_sword"; }
    
    @Override
    public String getDisplayName() { return "&6My Sword"; }
    
    @Override
    public Material getMaterial() { return Material.DIAMOND_SWORD; }
    
    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        player.setHealth(20);
        player.sendMessage("Healed!");
    }
}
```

### Step 2: Reload

```
/ci reload
```

### Step 3: Get Item

```
/ci give my_sword
```

---

## ☕ Java Items

### Base Class

Extend `AbstractCustomItem`:

```java
public class MyItem extends AbstractCustomItem {
    @Override
    public String getId() { return "my_item"; }
    
    @Override
    public String getDisplayName() { return "&6My Item"; }
    
    @Override
    public Material getMaterial() { return Material.DIAMOND_SWORD; }
}
```

### Available Methods

| Method | When Called |
|--------|------------|
| `onLeftClick(event, player)` | Left click |
| `onRightClick(event, player)` | Right click |
| `onEquip(player)` | Equipped |
| `onUnequip(player)` | Unequipped |
| `onDamageDealt(event, player)` | Deal damage |
| `onDamageTaken(event, player)` | Take damage |
| `onKill(killer, victim)` | Kill entity |
| `onBlockBreak(player, event)` | Break block |

### ItemAPI Utilities

```java
ItemAPI.heal(player, 10);
ItemAPI.effect(player, PotionEffectType.SPEED, 30, 2);
ItemAPI.particles(player, Particle.FLAME, 50);
ItemAPI.sound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
ItemAPI.teleport(player, x, y, z);
ItemAPI.title(player, "Title", "Subtitle");
```

---

## 📝 Java Commands

### Base Class

Extend `CustomCommand`:

```java
import me.dcplugin.dcustomitems.api.commands.CustomCommand;

public class HealCommand extends CustomCommand {
    
    public HealCommand() {
        super("heal", "Heal player", "/heal [player]", "dci.heal");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player target = getTarget(sender, args, 0);
        if (target == null) return false;
        
        target.setHealth(20);
        msg(sender, "&aHealed " + target.getName());
        return true;
    }
}
```

### Available Methods

| Method | Description |
|--------|-------------|
| `msg(sender, text)` | Send colored message |
| `title(player, title, sub)` | Send title |
| `getTarget(sender, args, index)` | Get player from args |
| `getInt(args, index, def)` | Get int from args |
| `colorize(text)` | Convert & to § |

---

## 🏷️ Java Placeholders

### Base Class

Extend `CustomPlaceholder`:

```java
import me.dcplugin.dcustomitems.api.placeholders.CustomPlaceholder;

public class MoneyPlaceholder extends CustomPlaceholder {
    
    public MoneyPlaceholder() {
        super("money"); // %money%
    }
    
    @Override
    public String getValue(Player player) {
        return "1000"; // Your logic here
    }
}
```

### Usage

```
Balance: %money%
```

---

## 🔧 Messages Config

Edit `EXAMPLE-messages.java` in `items/` to customize all messages:

```java
MessagesConfig.PREFIX = "&8[&6MyPlugin&8] &r";
MessagesConfig.ITEM_GIVEN = PREFIX + "&aYou got &e{item}&a!";
```

---

## 📋 Commands

| Command | Description |
|---------|-------------|
| `/ci give <id> [player]` | Give custom item |
| `/ci list` | List all items |
| `/ci reload` | Reload plugin |
| `/api-item give <id> [player]` | Give API item |
| `/api-item list` | List API items |

---

## 📁 File Structure

```
plugins/DC-CustomItems/
├── items/
│   ├── my-sword.java         <- Item
│   ├── heal-command.java     <- Command
│   ├── money-placeholder.java <- Placeholder
│   └── messages.java         <- Messages config
├── compiled/                 <- Auto-generated
└── data.db                  <- Database
```

---

## 📞 Support

- **GitHub:** https://github.com/animesao/dcustomitems
- **Issues:** https://github.com/animesao/dcustomitems/issues

---

**Version:** 1.320.263
