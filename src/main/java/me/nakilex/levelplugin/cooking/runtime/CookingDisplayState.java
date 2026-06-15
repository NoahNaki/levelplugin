package me.nakilex.levelplugin.cooking.runtime;

import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Runtime references and metadata for floating cooking display entities. */
public record CookingDisplayState(
        ItemDisplay itemDisplay,
        TextDisplay textDisplay,
        Map<String, ItemDisplay> ingredientDisplays,
        Instant createdAt
) {
    public CookingDisplayState {
        if (ingredientDisplays == null) {
            ingredientDisplays = new HashMap<>();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public CookingDisplayState(ItemDisplay itemDisplay, TextDisplay textDisplay) {
        this(itemDisplay, textDisplay, new HashMap<>(), Instant.now());
    }

    public CookingDisplayState withTextDisplay(TextDisplay textDisplay) {
        return new CookingDisplayState(itemDisplay, textDisplay, ingredientDisplays, createdAt);
    }
}
