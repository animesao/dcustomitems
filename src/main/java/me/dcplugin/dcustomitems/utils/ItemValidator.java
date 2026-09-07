package me.dcplugin.dcustomitems.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ItemValidator {

    private ItemValidator() {}

    public record Issue(String itemId, String field, String message) {
        @Override public String toString() { return itemId + " [" + field + "]: " + message; }
    }

    public static List<Issue> validateShaped(String itemId, List<String> pattern, Map<?, ?> keys, Set<String> knownMaterials, Set<String> knownItems) {
        List<Issue> issues = new ArrayList<>();
        if (pattern == null || pattern.isEmpty()) {
            issues.add(new Issue(itemId, "recipes.shaped.pattern", "пустой pattern (нужно 1-3 строки)"));
            return issues;
        }
        if (pattern.size() > 3) issues.add(new Issue(itemId, "recipes.shaped.pattern", "больше 3 строк"));
        int width = -1;
        for (String row : pattern) {
            if (row == null || row.length() > 3) issues.add(new Issue(itemId, "recipes.shaped.pattern", "строка длиннее 3: '" + row + "'"));
            if (width < 0) width = row == null ? 0 : row.length();
            else if (row != null && row.length() != width) issues.add(new Issue(itemId, "recipes.shaped.pattern", "неровные строки"));
            if (row != null) for (char c : row.toCharArray()) {
                if (c == ' ') continue;
                if (keys == null || !keys.containsKey(String.valueOf(c)) && !keys.containsKey(c)) {
                    issues.add(new Issue(itemId, "recipes.shaped.keys", "нет ключа для символа '" + c + "'"));
                }
            }
        }
        if (keys != null) for (Map.Entry<?, ?> e : keys.entrySet()) {
            String ingredient = String.valueOf(e.getValue());
            if (!isKnownIngredient(ingredient, knownMaterials, knownItems)) {
                issues.add(new Issue(itemId, "recipes.shaped.keys." + e.getKey(), "неизвестный ингредиент '" + ingredient + "'"));
            }
        }
        return issues;
    }

    public static List<Issue> validateShapeless(String itemId, List<?> ingredients, Set<String> knownMaterials, Set<String> knownItems) {
        List<Issue> issues = new ArrayList<>();
        if (ingredients == null || ingredients.isEmpty()) {
            issues.add(new Issue(itemId, "recipes.shapeless", "пустой список ingredients"));
            return issues;
        }
        if (ingredients.size() > 9) issues.add(new Issue(itemId, "recipes.shapeless", "больше 9 ингредиентов"));
        for (Object o : ingredients) {
            String ingredient = String.valueOf(o);
            if (!isKnownIngredient(ingredient, knownMaterials, knownItems)) {
                issues.add(new Issue(itemId, "recipes.shapeless", "неизвестный ингредиент '" + ingredient + "'"));
            }
        }
        return issues;
    }

    public static List<Issue> validateSmithing(String itemId, String template, String base, String addition, Set<String> knownMaterials, Set<String> knownItems) {
        List<Issue> issues = new ArrayList<>();
        if (isBlank(template)) issues.add(new Issue(itemId, "recipes.smithing.template", "пустой template"));
        if (isBlank(base)) issues.add(new Issue(itemId, "recipes.smithing.base", "пустой base"));
        if (isBlank(addition)) issues.add(new Issue(itemId, "recipes.smithing.addition", "пустой addition"));
        for (String s : new String[]{template, base, addition}) {
            if (!isBlank(s) && !isKnownIngredient(s, knownMaterials, knownItems)) {
                issues.add(new Issue(itemId, "recipes.smithing", "неизвестный ингредиент '" + s + "'"));
            }
        }
        return issues;
    }

    public static List<Issue> validateStonecutting(String itemId, String ingredient, Set<String> knownMaterials, Set<String> knownItems) {
        List<Issue> issues = new ArrayList<>();
        if (isBlank(ingredient)) issues.add(new Issue(itemId, "recipes.stonecutting.ingredient", "пустой ingredient"));
        else if (!isKnownIngredient(ingredient, knownMaterials, knownItems)) {
            issues.add(new Issue(itemId, "recipes.stonecutting", "неизвестный ингредиент '" + ingredient + "'"));
        }
        return issues;
    }

    private static boolean isKnownIngredient(String raw, Set<String> mats, Set<String> items) {
        if (raw == null) return false;
        String s = raw.trim().toUpperCase();
        if (mats != null && mats.contains(s)) return true;
        if (items != null) {
            for (String id : items) if (id.equalsIgnoreCase(raw.trim())) return true;
        }
        return false;
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
