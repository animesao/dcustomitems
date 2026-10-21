
package me.dcplugin.dcustomitems.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ItemBuilder {

    /**
     * Fallback-namespace, если билдер создан без ссылки на плагин.
     * Значение по умолчанию совпадает с namespace текущего plugin.yml
     * ("DC-CustomItems" -> "dc-customitems").
     */
    private static final String FALLBACK_NAMESPACE = "dc-customitems";

    private final ItemStack itemStack;
    private final org.bukkit.plugin.Plugin plugin;
    private ItemMeta itemMeta;

    public ItemBuilder(Material material) {
        this(material, null);
    }

    public ItemBuilder(Material material, org.bukkit.plugin.Plugin plugin) {
        this.plugin = plugin;
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
            List<String> colorizedLore = new ArrayList<>(lore.size());
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
        itemStack.setAmount(Math.max(1, Math.min(amount, 64)));
        return this;
    }

    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        if (itemMeta != null && enchantment != null && level > 0) {
            itemMeta.addEnchant(enchantment, level, true);
        }
        return this;
    }

    public ItemBuilder addItemFlags(ItemFlag... flags) {
        if (itemMeta != null && flags != null) {
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
            // UNBREAKING не даёт бонуса — в отличие от SHARPNESS, который
            // добавлял бы +1 к урону.
            itemMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemBuilder setSkullTexture(String texture) {
        if (itemStack.getType() != Material.PLAYER_HEAD || !(itemMeta instanceof SkullMeta)) {
            return this;
        }
        if (texture == null || texture.isBlank()) {
            return this;
        }

        SkullMeta skullMeta = (SkullMeta) itemMeta;
        String textureUrl = resolveTextureUrl(texture);
        if (textureUrl == null) {
            skullMeta.setOwner("CustomHead");
            return this;
        }

        try {
            UUID id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + texture).getBytes(StandardCharsets.UTF_8));
            PlayerProfile profile = Bukkit.createPlayerProfile(id, "CustomHead");
            profile.getTextures().setSkin(new URL(textureUrl));
            skullMeta.setOwnerProfile(profile);
        } catch (Exception e) {
            // Ставим владельца-заглушку: предмет остаётся валидным голова,
            // просто без текстуры.
            try {
                skullMeta.setOwner("CustomHead");
            } catch (Exception ignored) {
                // Совсем крайний случай — оставляем мета без владельца
            }
        }
        return this;
    }

    /**
     * Приводит описание текстуры к URL.
     *
     * Поддерживаются: прямой http(s)-URL, base64 от JSON-текстуры
     * и «чистый» хеш (превращается в textures.minecraft.net URL).
     */
    private static String resolveTextureUrl(String texture) {
        String value = texture.trim();
        if (value.regionMatches(true, 0, "http://", 0, 7)
                || value.regionMatches(true, 0, "https://", 0, 8)) {
            return value;
        }
        if (value.length() > 64) {
            try {
                byte[] decoded = java.util.Base64.getDecoder().decode(value);
                String json = new String(decoded, StandardCharsets.UTF_8);
                int index = json.indexOf("\"url\":\"");
                if (index >= 0) {
                    // Значение начинается сразу после закрывающей кавычки:
                    // "\"url\":\"" — это 7 символов.
                    String url = json.substring(index + 7);
                    int end = url.indexOf('"');
                    if (end > 0) return url.substring(0, end);
                }
            } catch (Exception ignored) {
                // Не base64 — трактуем как хеш
            }
        }
        return "http://textures.minecraft.net/texture/" + value;
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
        if (itemMeta == null || attributeName == null) {
            return this;
        }
        Attribute attribute = EnumCache.getAttribute(attributeName);
        if (attribute == null) {
            return this;
        }
        try {
            AttributeModifier modifier = new AttributeModifier(
                    attributeKey(attribute),
                    value,
                    operation,
                    org.bukkit.inventory.EquipmentSlotGroup.ANY
            );
            itemMeta.addAttributeModifier(attribute, modifier);
        } catch (IllegalArgumentException ignored) {
            // Некорректное имя атрибута — пропускаем
        }
        return this;
    }

    public ItemStack build() {
        if (itemMeta != null) {
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    /**
     * Ключ модификатора атрибута в namespace плагина.
     *
     * {@code NamespacedKey.fromString} без namespace вернул бы ключ из
     * namespace {@code minecraft} — поэтому строим ключ вручную,
     * нормализуя символы до {@code [a-z0-9._-]}.
     */
    private NamespacedKey attributeKey(Attribute attribute) {
        NamespacedKey key = attribute.getKey();
        String raw = key != null ? key.getKey() : "attribute";
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            boolean valid = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '-' || c == '.';
            sb.append(valid ? c : '_');
        }
        String fullKey = "custom_" + sb;
        return plugin != null
                ? new NamespacedKey(plugin, fullKey)
                : new NamespacedKey(FALLBACK_NAMESPACE, fullKey);
    }
}
