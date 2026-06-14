package me.nakilex.levelplugin.cooking.model;

import org.bukkit.Material;

import java.util.List;

/** Immutable config-backed stage definition for a cooking recipe. */
public record CookingStage(
        CookingStageType type,
        List<CookingIngredientRequirement> requirements,
        long durationTicks,
        String miniGameId
) {
    public CookingStage {
        requirements = List.copyOf(requirements == null ? List.of() : requirements);
        durationTicks = Math.max(0L, durationTicks);
    }

    /** Compatibility helper for older single-ingredient call sites. */
    public Material itemMaterial() {
        return requirements.isEmpty() ? null : requirements.get(0).material();
    }

    /** Compatibility helper for older single-ingredient call sites. */
    public int amount() {
        return requirements.isEmpty() ? 1 : requirements.get(0).amount();
    }
}
