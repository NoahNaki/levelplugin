package me.nakilex.levelplugin.cooking.runtime;

import me.nakilex.levelplugin.cooking.display.CookingDisplayAnimator;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Runtime references and metadata for floating cooking display entities. */
public record CookingDisplayState(
        ItemDisplay itemDisplay,
        TextDisplay textDisplay,
        Map<String, ManagedItemDisplay> ingredientDisplays,
        ManagedItemDisplay rewardPreviewDisplay,
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
        this(itemDisplay, textDisplay, new HashMap<>(), null, Instant.now());
    }

    public CookingDisplayState withTextDisplay(TextDisplay textDisplay) {
        return new CookingDisplayState(itemDisplay, textDisplay, ingredientDisplays, rewardPreviewDisplay, createdAt);
    }

    public CookingDisplayState withRewardPreviewDisplay(ManagedItemDisplay rewardPreviewDisplay) {
        return new CookingDisplayState(itemDisplay, textDisplay, ingredientDisplays, rewardPreviewDisplay, createdAt);
    }

    /** One managed display-group entry, including its animation handle. */
    public record ManagedItemDisplay(ItemDisplay display, CookingDisplayAnimator.AnimatedDisplay animation) {}
}
