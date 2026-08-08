
package me.dcplugin.dcustomitems.utils;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemBuilder {

    private ItemStack itemStack;
    private ItemMeta itemMeta;

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = itemStack.getItemMeta();
    }

    public ItemBuilder setDisplayName(String displayName) {
        if (itemMeta != null) {
            itemMeta.setDisplayName(ColorUtils.colorize(displayName));
        }
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        if (itemMeta != null) {
            List<String> colorizedLore = new ArrayList<>();
            for (String line : lore) {
                colorizedLore.add(ColorUtils.colorize(line));
            }
            itemMeta.setLore(colorizedLore);
        }
        return this;
    }

    public ItemBuilder addLoreLine(String line) {
        if (itemMeta != null) {
            List<String> lore = itemMeta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.add(ColorUtils.colorize(line));
            itemMeta.setLore(lore);
        }
        return this;
    }

    public ItemBuilder setAmount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        if (itemMeta != null) {
            itemMeta.addEnchant(enchantment, level, true);
        }
        return this;
    }

    public ItemBuilder addItemFlags(ItemFlag... flags) {
        if (itemMeta != null) {
            itemMeta.addItemFlags(flags);
        }
        return this;
    }

    public ItemBuilder setUnbreakable(boolean unbreakable) {
        if (itemMeta != null) {
            itemMeta.setUnbreakable(unbreakable);
        }
        return this;
    }

    public ItemBuilder setGlowing(boolean glowing) {
        if (glowing && itemMeta != null) {
            itemMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemBuilder setSkullTexture(String texture) {
        if (itemStack.getType() == Material.PLAYER_HEAD && itemMeta instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta) itemMeta;
            try {
                // For 1.20.2+ we should use the new PlayerProfile API if possible
                UUID id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + texture).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                PlayerProfile profile = org.bukkit.Bukkit.createPlayerProfile(id, "CustomHead");
                
                // texture can be either a URL or a Base64 string
                String textureUrl = texture;
                if (!texture.startsWith("http")) {
                    // If it's not a URL, assume it's a Base64 encoded JSON or just the texture hash
                    if (texture.length() > 64) {
                        // Likely Base64
                        byte[] decoded = java.util.Base64.getDecoder().decode(texture);
                        String json = new String(decoded);
                        // Simple extraction if it's the standard Minecraft texture JSON
                        if (json.contains("\"url\":\"")) {
                            textureUrl = json.split("\"url\":\"")[1].split("\"")[0];
                        }
                    } else {
                        // Likely just the hash
                        textureUrl = "http://textures.minecraft.net/texture/" + texture;
                    }
                }
                
                profile.getTextures().setSkin(new java.net.URL(textureUrl));
                skullMeta.setOwnerProfile(profile);
            } catch (Exception e) {
                try {
                    // Fallback to direct reflection if the above fails (some environments/versions)
                    Class<?> profileClass = Class.forName("com.mojang.authlib.GameProfile");
                    Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
                    UUID id = UUID.randomUUID();
                    Object profile = profileClass.getConstructor(UUID.class, String.class).newInstance(id, "CustomHead");
                    
                    String value = texture;
                    if (texture.length() <= 64 && !texture.startsWith("http")) {
                        // If it's a hash, we need the Base64 encoded JSON for GameProfile property
                        String json = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + texture + "\"}}}";
                        value = java.util.Base64.getEncoder().encodeToString(json.getBytes());
                    }
                    
                    Object property = propertyClass.getConstructor(String.class, String.class).newInstance("textures", value);
                    Object properties = profileClass.getMethod("getProperties").invoke(profile);
                    properties.getClass().getMethod("put", Object.class, Object.class).invoke(properties, "textures", property);
                    
                    Field field = skullMeta.getClass().getDeclaredField("profile");
                    field.setAccessible(true);
                    field.set(skullMeta, profile);
                } catch (Exception ex) {
                    skullMeta.setOwner("CustomHead");
                }
            }
        }
        return this;
    }

    private Object createGameProfile(String texture) {
        try {
            // Используем reflection для создания GameProfile
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Object gameProfile = gameProfileClass.getConstructor(UUID.class, String.class)
                    .newInstance(UUID.randomUUID(), "CustomHead");
            
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Object property = propertyClass.getConstructor(String.class, String.class)
                    .newInstance("textures", texture);
            
            Object propertyMap = gameProfile.getClass().getMethod("getProperties").invoke(gameProfile);
            propertyMap.getClass().getMethod("put", Object.class, Object.class)
                    .invoke(propertyMap, "textures", property);
            
            return gameProfile;
        } catch (Exception e) {
            return null;
        }
    }

    public ItemBuilder addAttribute(String attributeName, double value) {
        return addAttribute(attributeName, value, AttributeModifier.Operation.ADD_NUMBER);
    }

    public ItemBuilder setCustomModelData(int customModelData) {
        if (itemMeta != null) {
            itemMeta.setCustomModelData(customModelData);
        }
        return this;
    }

    public ItemBuilder addAttribute(String attributeName, double value, AttributeModifier.Operation operation) {
        if (itemMeta != null) {
            try {
                Attribute attribute = Attribute.valueOf(attributeName.toUpperCase());
                AttributeModifier modifier = new AttributeModifier(
                        org.bukkit.NamespacedKey.fromString("custom_" + attributeName.toLowerCase()),
                        value,
                        operation,
                        org.bukkit.inventory.EquipmentSlotGroup.ANY
                );
                itemMeta.addAttributeModifier(attribute, modifier);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return this;
    }

    public ItemStack build() {
        if (itemMeta != null) {
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }
}
