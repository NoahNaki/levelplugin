package me.nakilex.levelplugin.utils;

import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Helper methods for formatting and manipulating item tooltips.
 */
public final class TooltipUtil {
    private TooltipUtil() {
    }

    /**
     * Add a textual progress bar to the provided lore list. The bar uses the
     * {@link GuiUtil#createProgressBar(double, int)} helper under the hood to
     * ensure a consistent style across the plugin.
     *
     * @param lore     lore list to append to
     * @param progress value between 0.0 and 1.0
     * @param length   number of segments in the bar
     */
    public static void addProgressBar(List<String> lore, double progress, int length) {
        lore.add(GuiUtil.createProgressBar(progress, length));
    }

    /**
     * Convenience overload that appends additional text after the progress
     * bar (for example, numeric values or labels).
     */
    public static void addProgressBar(List<String> lore, double progress, int length, String suffix) {
        lore.add(GuiUtil.createProgressBar(progress, length) + " " + suffix);
    }

    /**
     * Center the tooltip (display name and lore) of the given item using the
     * existing {@link TextUtil#centerItemTooltip(ItemStack)} implementation.
     */
    public static void centerLore(ItemStack item) {
        TextUtil.centerItemTooltip(item);
    }
}
