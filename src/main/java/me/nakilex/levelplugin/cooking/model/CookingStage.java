package me.nakilex.levelplugin.cooking.model;

import me.nakilex.levelplugin.cooking.minigame.CookingMiniGameType;
import org.bukkit.Material;

import java.util.List;

/** Immutable config-backed stage definition for a cooking recipe. */
public record CookingStage(
        CookingStageType type,
        List<CookingIngredientRequirement> requirements,
        long durationTicks,
        String miniGameId,
        String tooltip,
        CookingMiniGameType miniGameType,
        long hitWindowTicks,
        int requiredClicks
) {
    public CookingStage {
        requirements = List.copyOf(requirements == null ? List.of() : requirements);
        durationTicks = Math.max(0L, durationTicks);
        if (tooltip != null && tooltip.isBlank()) {
            tooltip = null;
        }
        hitWindowTicks = Math.max(0L, hitWindowTicks);
        requiredClicks = Math.max(0, requiredClicks);
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
