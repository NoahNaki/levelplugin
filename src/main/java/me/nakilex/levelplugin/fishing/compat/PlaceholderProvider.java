package me.nakilex.levelplugin.fishing.compat;

import me.nakilex.levelplugin.fishing.api.FishingContext;

public interface PlaceholderProvider {
    String applyPlaceholders(FishingContext ctx, String input);
}
