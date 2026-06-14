package me.nakilex.levelplugin.cooking.runtime;

import me.nakilex.levelplugin.cooking.util.CookingLocationKey;

import java.time.Instant;
import java.util.UUID;

/** Runtime-only lock for a selected cooking recipe. Stage execution is added later. */
public record ActiveCookingSession(
        UUID playerId,
        CookingLocationKey workstationKey,
        String recipeId,
        Instant startedAt
) {
    public ActiveCookingSession(UUID playerId, CookingLocationKey workstationKey, String recipeId) {
        this(playerId, workstationKey, recipeId, Instant.now());
    }
}
