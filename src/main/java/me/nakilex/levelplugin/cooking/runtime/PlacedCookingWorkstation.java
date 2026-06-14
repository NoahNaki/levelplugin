package me.nakilex.levelplugin.cooking.runtime;

import me.nakilex.levelplugin.cooking.model.CookingWorkstationType;
import me.nakilex.levelplugin.cooking.util.CookingLocationKey;

import java.util.UUID;

/** In-memory runtime entry for a player-placed cooking workstation. */
public record PlacedCookingWorkstation(
        CookingLocationKey locationKey,
        CookingWorkstationType type,
        UUID placedBy
) {
}
