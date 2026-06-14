package me.nakilex.levelplugin.cooking.model;

import org.bukkit.Material;

/** Immutable config-backed stage definition for a cooking recipe. */
public record CookingStage(
        CookingStageType type,
        Material itemMaterial,
        int amount,
        long durationTicks,
        String miniGameId
) {
    public CookingStage {
        amount = Math.max(1, amount);
        durationTicks = Math.max(0L, durationTicks);
    }
}
