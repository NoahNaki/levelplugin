package me.nakilex.levelplugin.cooking.runtime;

import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;

import java.time.Instant;

/** Runtime references and metadata for floating cooking display entities. */
public record CookingDisplayState(
        ItemDisplay itemDisplay,
        TextDisplay textDisplay,
        Instant createdAt
) {
    public CookingDisplayState(ItemDisplay itemDisplay, TextDisplay textDisplay) {
        this(itemDisplay, textDisplay, Instant.now());
    }
}
