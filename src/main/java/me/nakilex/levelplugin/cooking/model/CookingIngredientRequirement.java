package me.nakilex.levelplugin.cooking.model;

import org.bukkit.Material;

import java.util.Locale;
import java.util.Optional;

/** Immutable ingredient requirement for an INSERT_ITEM cooking stage. */
public record CookingIngredientRequirement(
        Material material,
        String nexoItemId,
        int amount
) {
    public CookingIngredientRequirement {
        if (nexoItemId != null && nexoItemId.isBlank()) {
            nexoItemId = null;
        }
        amount = Math.max(1, amount);
    }

    public Optional<String> nexoItemIdOptional() {
        return Optional.ofNullable(nexoItemId);
    }

    public String progressKey() {
        if (nexoItemId != null) {
            return "nexo:" + nexoItemId.toLowerCase(Locale.ROOT);
        }
        return "material:" + (material == null ? "unknown" : material.name().toLowerCase(Locale.ROOT));
    }
}
