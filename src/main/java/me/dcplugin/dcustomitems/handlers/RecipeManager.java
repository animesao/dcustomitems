package me.dcplugin.dcustomitems.handlers;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.api.RecipeDef;
import me.dcplugin.dcustomitems.models.CustomItem;
import me.dcplugin.dcustomitems.utils.EnumCache;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.FurnaceRecipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Менеджер крафт-рецептов кастомных предметов.
 *
 * Рецепты задаются в YAML-секции предмета:
 * <pre>
 * my-item:
 *   item: ...
 *   recipes:
 *     shaped:
 *       - pattern:
 *           - " A "
 *           - "ABA"
 *           - " A "
 *         keys:
 *           A: DIAMOND            # материал
 *           B: my-other-item      # или ID другого кастомного предмета
 *         amount: 1
 *     shapeless:
 *       - ingredients: [ DIAMOND, DIAMOND, STICK ]
 *     furnace:
 *       - ingredient: IRON_INGOT
 *         experience: 0.7
 *         cooking-time: 200       # тики
 * </pre>
 *
 * Рецепты автоматически удаляются и пересоздаются при /ci reload.
 */
public class RecipeManager {

    private final Main plugin;
    private final Set<NamespacedKey> registered = new HashSet<>();
    // Ключи рецептов Java API-предметов (отдельно: регистрируются ItemRegistry)
    private final Set<NamespacedKey> javaKeys = new HashSet<>();

    public RecipeManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Удалить все рецепты, добавленные этим менеджером (вызывается перед reload).
     */
    public void clear() {
        for (NamespacedKey key : registered) {
            try {
                plugin.getServer().removeRecipe(key);
            } catch (Exception ignored) {
                // Сервер без removeRecipe (старый Paper) — рецепты исчезнут после рестарта
            }
        }
        registered.clear();
    }

    /**
     * Зарегистрировать рецепты из секции "recipes" предмета (если они есть).
     */
    public void register(CustomItem item) {
        if (item == null || !item.hasRecipes()) return;

        Map<String, Object> recipes = item.getRecipes();
        ItemStack result = item.getItemStack().clone();

        registerShaped(item, recipes, result);
        registerShapeless(item, recipes, result);
        registerFurnace(item, recipes, result);
    }

    // ===== Java API-предметы (AbstractCustomItem.getRecipes()) =====

    /**
     * Удалить рецепты Java-предметов (вызывается перед их перерегистрацией).
     */
    public void clearJava() {
        for (NamespacedKey key : javaKeys) {
            try {
                plugin.getServer().removeRecipe(key);
            } catch (Exception ignored) {}
        }
        javaKeys.clear();
    }

    /**
     * Зарегистрировать рецепты, объявленные Java API-предметами через
     * {@code getRecipes()} (RecipeDef). Вызывается ItemRegistry после загрузки.
     */
    public void registerJavaItems(Collection<AbstractCustomItem> items) {
        clearJava();
        if (items == null || items.isEmpty()) return;

        for (AbstractCustomItem item : items) {
            if (item == null) continue;
            List<RecipeDef> defs;
            try {
                defs = item.getRecipes();
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "[Recipes] Ошибка getRecipes() у '" + item.getId() + "': " + e.getMessage(), e);
                continue;
            }
            if (defs == null || defs.isEmpty()) continue;

            ItemStack result = item.createItemStack();
            int index = 0;
            for (RecipeDef def : defs) {
                registerJavaRecipe(item, def, result, index++);
            }
        }
    }

    private void registerJavaRecipe(AbstractCustomItem item, RecipeDef def, ItemStack result, int index) {
        if (def == null) return;
        try {
            NamespacedKey key = javaKey(item.getId(), def.getType().name().toLowerCase(), index);
            ItemStack out = result.clone();
            out.setAmount(def.getAmount() <= 0 ? 1 : def.getAmount());

            switch (def.getType()) {
                case SHAPED: {
                    List<String> pattern = def.getPattern();
                    if (pattern.isEmpty() || pattern.size() > 3) {
                        plugin.getLogger().warning("[Recipes] '" + item.getId() + "': shaped-паттерн должен быть 1-3 строки");
                        return;
                    }
                    ShapedRecipe shaped = new ShapedRecipe(key, out);
                    shaped.shape(pattern.toArray(new String[0]));
                    for (String row : pattern) {
                        for (char c : row.toCharArray()) {
                            if (c == ' ') continue;
                            String choiceRaw = def.getKeys().get(c);
                            RecipeChoice choice = choiceRaw == null ? null : resolveChoice(choiceRaw);
                            if (choice == null) {
                                plugin.getLogger().warning("[Recipes] '" + item.getId()
                                        + "': неизвестный ингредиент '" + choiceRaw + "' для ключа '" + c + "'");
                                return;
                            }
                            shaped.setIngredient(c, choice);
                        }
                    }
                    addJava(key, shaped, item.getId());
                    break;
                }
                case SHAPELESS: {
                    if (def.getIngredients().isEmpty()) return;
                    ShapelessRecipe shapeless = new ShapelessRecipe(key, out);
                    for (String ingredient : def.getIngredients()) {
                        RecipeChoice choice = resolveChoice(ingredient);
                        if (choice == null) {
                            plugin.getLogger().warning("[Recipes] '" + item.getId()
                                    + "': неизвестный ингредиент '" + ingredient + "'");
                            return;
                        }
                        shapeless.addIngredient(choice);
                    }
                    addJava(key, shapeless, item.getId());
                    break;
                }
                case FURNACE: {
                    if (def.getIngredients().isEmpty()) return;
                    RecipeChoice choice = resolveChoice(def.getIngredients().get(0));
                    if (choice == null) return;
                    FurnaceRecipe furnace = new FurnaceRecipe(key, out, choice,
                            def.getExperience(), def.getCookingTime() <= 0 ? 200 : def.getCookingTime());
                    addJava(key, furnace, item.getId());
                    break;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "[Recipes] Ошибка java-рецепта '" + item.getId() + "' #" + index + ": " + e.getMessage(), e);
        }
    }

    private boolean addJava(NamespacedKey key, Recipe recipe, String itemId) {
        try {
            if (plugin.getServer().addRecipe(recipe)) {
                javaKeys.add(key);
                return true;
            }
            plugin.getLogger().warning("[Recipes] Не удалось добавить рецепт '" + key + "' (дубликат?)");
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "[Recipes] Ошибка регистрации java-рецепта '" + itemId + "': " + e.getMessage(), e);
        }
        return false;
    }

    private NamespacedKey javaKey(String itemId, String type, int index) {
        return new NamespacedKey(plugin, "java_" + itemId.toLowerCase() + "_" + type + "_" + index);
    }

    // ===== Shaped =====

    private void registerShaped(CustomItem item, Map<String, Object> recipes, ItemStack result) {
        Object raw = recipes.get("shaped");
        if (!(raw instanceof List)) return;

        int index = 0;
        for (Object recipeObj : (List<?>) raw) {
            Map<String, Object> recipe = asMap(recipeObj);
            if (recipe == null) {
                index++;
                continue;
            }

            List<String> pattern = asStringList(recipe.get("pattern"));
            Map<String, Object> keys = asMap(recipe.get("keys"));
            if (pattern.isEmpty() || keys == null) {
                index++;
                continue;
            }

            try {
                NamespacedKey key = recipeKey(item.getId(), "shaped", index);
                ItemStack out = result.clone();
                out.setAmount(asInt(recipe.get("amount"), 1));

                ShapedRecipe shaped = new ShapedRecipe(key, out);
                shaped.shape(pattern.toArray(new String[0]));

                boolean valid = true;
                for (String row : pattern) {
                    for (char c : row.toCharArray()) {
                        if (c == ' ') continue;
                        String choiceRaw = asString(keys.get(String.valueOf(c)));
                        if (choiceRaw == null) {
                            valid = false;
                            break;
                        }
                        RecipeChoice choice = resolveChoice(choiceRaw);
                        if (choice == null) {
                            plugin.getLogger().warning("[Recipes] '" + item.getId()
                                    + "': неизвестный ингредиент '" + choiceRaw + "' для ключа '" + c + "'");
                            valid = false;
                            break;
                        }
                        shaped.setIngredient(c, choice);
                    }
                    if (!valid) break;
                }

                if (valid && add(key, shaped, item)) {
                    plugin.getLogger().info("[Recipes] '" + item.getId() + "': shaped рецепт #" + index);
                }
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "[Recipes] Ошибка shaped-рецепта '" + item.getId() + "': " + e.getMessage(), e);
            }
            index++;
        }
    }

    // ===== Shapeless =====

    private void registerShapeless(CustomItem item, Map<String, Object> recipes, ItemStack result) {
        Object raw = recipes.get("shapeless");
        if (!(raw instanceof List)) return;

        int index = 0;
        for (Object recipeObj : (List<?>) raw) {
            Map<String, Object> recipe = asMap(recipeObj);
            if (recipe == null) {
                index++;
                continue;
            }

            List<String> ingredients = asStringList(recipe.get("ingredients"));
            if (ingredients.isEmpty()) {
                index++;
                continue;
            }

            try {
                NamespacedKey key = recipeKey(item.getId(), "shapeless", index);
                ItemStack out = result.clone();
                out.setAmount(asInt(recipe.get("amount"), 1));

                ShapelessRecipe shapeless = new ShapelessRecipe(key, out);
                boolean valid = true;
                for (String ingredient : ingredients) {
                    RecipeChoice choice = resolveChoice(ingredient);
                    if (choice == null) {
                        plugin.getLogger().warning("[Recipes] '" + item.getId()
                                + "': неизвестный ингредиент '" + ingredient + "'");
                        valid = false;
                        break;
                    }
                    shapeless.addIngredient(choice);
                }

                if (valid && add(key, shapeless, item)) {
                    plugin.getLogger().info("[Recipes] '" + item.getId() + "': shapeless рецепт #" + index);
                }
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "[Recipes] Ошибка shapeless-рецепта '" + item.getId() + "': " + e.getMessage(), e);
            }
            index++;
        }
    }

    // ===== Furnace =====

    private void registerFurnace(CustomItem item, Map<String, Object> recipes, ItemStack result) {
        Object raw = recipes.get("furnace");
        if (!(raw instanceof List)) return;

        int index = 0;
        for (Object recipeObj : (List<?>) raw) {
            Map<String, Object> recipe = asMap(recipeObj);
            if (recipe == null) {
                index++;
                continue;
            }

            String ingredient = asString(recipe.get("ingredient"));
            if (ingredient == null) {
                index++;
                continue;
            }

            RecipeChoice choice = resolveChoice(ingredient);
            if (choice == null) {
                plugin.getLogger().warning("[Recipes] '" + item.getId()
                        + "': неизвестный ингредиент печи '" + ingredient + "'");
                index++;
                continue;
            }

            try {
                NamespacedKey key = recipeKey(item.getId(), "furnace", index);
                ItemStack out = result.clone();
                float experience = (float) asDouble(recipe.get("experience"), 0.0);
                int cookingTime = asInt(recipe.get("cooking-time"), 200);

                FurnaceRecipe furnace = new FurnaceRecipe(key, out, choice, experience, cookingTime);
                if (add(key, furnace, item)) {
                    plugin.getLogger().info("[Recipes] '" + item.getId() + "': furnace рецепт #" + index);
                }
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "[Recipes] Ошибка furnace-рецепта '" + item.getId() + "': " + e.getMessage(), e);
            }
            index++;
        }
    }

    // ===== Helpers =====

    private boolean add(NamespacedKey key, Recipe recipe, CustomItem item) {
        try {
            if (plugin.getServer().addRecipe(recipe)) {
                registered.add(key);
                return true;
            }
            plugin.getLogger().warning("[Recipes] Не удалось добавить рецепт '" + key + "' (дубликат?)");
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "[Recipes] Ошибка регистрации рецепта '" + item.getId() + "': " + e.getMessage(), e);
        }
        return false;
    }

    private NamespacedKey recipeKey(String itemId, String type, int index) {
        return new NamespacedKey(plugin, itemId + "_" + type + "_" + index);
    }

    /**
     * Ингредиент: материал (DIAMOND) или ID кастомного предмета
     * (vampire-blade) — YAML или Java API, оба распознаются.
     */
    private RecipeChoice resolveChoice(String raw) {
        if (raw == null || raw.isEmpty()) return null;

        Material material = EnumCache.getMaterial(raw);
        if (material != null) {
            return new RecipeChoice.MaterialChoice(material);
        }

        // YAML-предмет
        CustomItem custom = plugin.getItemHandler().getCustomItem(raw.toLowerCase());
        if (custom != null) {
            return exactChoice(custom.getItemStack());
        }

        // Java API-предмет (AbstractCustomItem)
        AbstractCustomItem javaItem = plugin.getApiItemRegistry().getItem(raw);
        if (javaItem == null) javaItem = plugin.getApiItemRegistry().getItem(raw.toLowerCase());
        if (javaItem != null) {
            return exactChoice(javaItem.createItemStack());
        }
        return null;
    }

    private RecipeChoice exactChoice(ItemStack stack) {
        ItemStack copy = stack.clone();
        copy.setAmount(1);
        return new RecipeChoice.ExactChoice(copy);
    }

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            return map;
        }
        return null;
    }

    private String asString(Object value) {
        return value instanceof String ? (String) value : null;
    }

    private List<String> asStringList(Object value) {
        List<String> out = new ArrayList<>();
        if (value instanceof List) {
            for (Object o : (List<?>) value) {
                out.add(String.valueOf(o));
            }
        } else if (value instanceof String) {
            out.add((String) value);
        }
        return out;
    }

    private int asInt(Object value, int def) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private double asDouble(Object value, double def) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {}
        }
        return def;
    }
}
