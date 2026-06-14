package me.nakilex.levelplugin.cooking.registry;

import me.nakilex.levelplugin.cooking.model.CookingRecipe;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory registry for config-backed cooking recipe definitions. */
public class CookingRecipeRegistry {
    private final Map<String, CookingRecipe> recipes = new LinkedHashMap<>();

    public void replaceAll(Collection<CookingRecipe> definitions) {
        recipes.clear();
        if (definitions == null) return;
        for (CookingRecipe recipe : definitions) {
            if (recipe == null || recipe.id() == null || recipe.id().isBlank()) continue;
            recipes.put(recipe.id(), recipe);
        }
    }

    public Optional<CookingRecipe> get(String id) {
        return Optional.ofNullable(recipes.get(id));
    }

    public boolean contains(String id) {
        return recipes.containsKey(id);
    }

    public Collection<CookingRecipe> all() {
        return List.copyOf(recipes.values());
    }

    public int size() {
        return recipes.size();
    }
}
