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
        int requiredClicks,
        int barSize,
        int targetScore,
        int health,
        long speedTicks,
        String hitTargetSymbol,
        String hitHookSymbol,
        String hitLineSymbol,
        String mixFilledSymbol,
        String mixEmptySymbol,
        String healthSymbol
) {
    public CookingStage {
        requirements = List.copyOf(requirements == null ? List.of() : requirements);
        durationTicks = Math.max(0L, durationTicks);
        if (tooltip != null && tooltip.isBlank()) {
            tooltip = null;
        }
        hitWindowTicks = Math.max(0L, hitWindowTicks);
        requiredClicks = Math.max(0, requiredClicks);
        barSize = Math.max(0, barSize);
        targetScore = Math.max(0, targetScore);
        health = Math.max(0, health);
        speedTicks = Math.max(0L, speedTicks);
        hitTargetSymbol = normalizeSymbol(hitTargetSymbol);
        hitHookSymbol = normalizeSymbol(hitHookSymbol);
        hitLineSymbol = normalizeSymbol(hitLineSymbol);
        mixFilledSymbol = normalizeSymbol(mixFilledSymbol);
        mixEmptySymbol = normalizeSymbol(mixEmptySymbol);
        healthSymbol = normalizeSymbol(healthSymbol);
    }

    private static String normalizeSymbol(String symbol) {
        return symbol == null || symbol.isBlank() ? null : symbol;
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
