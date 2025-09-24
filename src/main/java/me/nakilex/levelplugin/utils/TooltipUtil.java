package me.nakilex.levelplugin.utils;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper methods for formatting item tooltips. Provides lightweight utilities
 * such as generating progress bars that can be reused across many GUI classes.
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
     * Generate standard left/right click instruction lines.
     *
     * @param leftAction  description following "Left-click" or {@code null}
     * @param rightAction description following "Right-click" or {@code null}
     * @return list of formatted instruction lines
     */
    public static List<String> clickInstructions(String leftAction, String rightAction) {
        List<String> lore = new ArrayList<>(2);
        if (leftAction != null) {
            lore.add(ChatColor.WHITE + "Left-click " + ChatColor.GRAY + leftAction);
        }
        if (rightAction != null) {
            lore.add(ChatColor.WHITE + "Right-click " + ChatColor.GRAY + rightAction);
        }
        return lore;
    }

    /**
     * Generate a single line describing a keybind instruction, e.g. "Press [F] to cast".
     *
     * @param key    the key to display (e.g. "F")
     * @param action the action description following the key
     * @return list containing the formatted instruction line (possibly empty)
     */
    public static List<String> keyInstructions(String key, String action) {
        List<String> lore = new ArrayList<>(1);
        if (key == null || key.isBlank()) {
            return lore;
        }
        StringBuilder line = new StringBuilder()
            .append(ChatColor.YELLOW)
            .append("Press [")
            .append(key.toUpperCase())
            .append("]");
        if (action != null && !action.isBlank()) {
            line.append(' ').append(ChatColor.GRAY).append(action);
        }
        lore.add(line.toString());
        return lore;
    }
}
