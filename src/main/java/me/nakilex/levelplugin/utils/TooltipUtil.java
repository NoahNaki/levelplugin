package me.nakilex.levelplugin.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper methods for formatting item tooltips.  Provides common operations
 * such as generating progress bars and centering lore lines.  This utility is
 * intentionally lightweight so it can be reused across many GUI classes.
 */
public final class TooltipUtil {

    private TooltipUtil() {}

    /**
     * Create a textual progress bar for the given values using the standard
     * colours defined in {@link GuiUtil}.
     *
     * @param current the current value
     * @param max     the maximum value represented by a full bar
     * @param length  number of characters in the bar
     * @return coloured progress bar string
     */
    public static String progressBar(double current, double max, int length) {
        double progress = max <= 0 ? 0.0 : current / max;
        return GuiUtil.createProgressBar(progress, length);
    }

    /**
     * Return a new list where each line has been centered relative to the
     * widest line using {@link ChatFormatter} pixel measurements.
     */
    public static List<String> centerLore(List<String> lore) {
        if (lore == null || lore.isEmpty()) return lore;
        int max = lore.stream().mapToInt(ChatFormatter::pixelLength).max().orElse(0) / 2;
        List<String> centered = new ArrayList<>(lore.size());
        for (String line : lore) {
            centered.add(ChatFormatter.getCenteredText(line, max));
        }
        return centered;
    }
}
