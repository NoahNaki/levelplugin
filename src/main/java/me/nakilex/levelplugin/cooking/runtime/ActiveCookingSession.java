package me.nakilex.levelplugin.cooking.runtime;

import me.nakilex.levelplugin.cooking.util.CookingLocationKey;

import java.time.Instant;
import java.util.UUID;

/** Runtime-only lock and stage progress for a selected cooking recipe. */
public record ActiveCookingSession(
        UUID playerId,
        CookingLocationKey workstationKey,
        String recipeId,
        CookingStageProgress progress,
        Instant startedAt
) {
    public ActiveCookingSession(UUID playerId, CookingLocationKey workstationKey, String recipeId) {
        this(playerId, workstationKey, recipeId, new CookingStageProgress(), Instant.now());
    }
}
