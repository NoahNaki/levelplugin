package me.nakilex.levelplugin.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for centering text in item tooltips and inventory GUI titles.
 */
public final class TextUtil {

    private TextUtil() {}

    /** Half the pixel width of a standard chest GUI (176px wide). */
    private static final int INVENTORY_TITLE_PX = 88;

    /**
     * Center an inventory title string using space padding so that it appears
     * centered in the GUI window.
     *
     * @param title the title to center
     * @return the centered title
     */
    public static String centerInventoryTitle(String title) {
        return ChatFormatter.getCenteredText(title, INVENTORY_TITLE_PX);
    }

    /**
     * Center the display name and lore of an item so that each line is centered
     * relative to the widest line. The item meta is modified in-place.
     *
     * @param item the item whose tooltip should be centered
     */
    public static void centerItemTooltip(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> lines = new ArrayList<>();
        if (meta.hasDisplayName()) lines.add(meta.getDisplayName());
        List<String> lore = meta.getLore();
        if (lore != null) lines.addAll(lore);

        if (lines.isEmpty()) return;

        int max = lines.stream().mapToInt(ChatFormatter::pixelLength).max().orElse(0);
        int center = max / 2;

        if (meta.hasDisplayName()) {
            meta.setDisplayName(ChatFormatter.getCenteredText(meta.getDisplayName(), center));
        }
        if (lore != null) {
            List<String> centeredLore = new ArrayList<>();
            for (String line : lore) {
                centeredLore.add(ChatFormatter.getCenteredText(line, center));
            }
            meta.setLore(centeredLore);
        }
        item.setItemMeta(meta);
    }

    /** Convert a lowercase, underscore- or space-separated name into capitalized words. */
    public static String beautifyWords(String name) {
        if (name == null) return "";
        String[] parts = name.toLowerCase().replace('_', ' ').split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            sb.append(Character.toUpperCase(parts[i].charAt(0)))
              .append(parts[i].substring(1));
            if (i < parts.length - 1) sb.append(' ');
        }
        return sb.toString();
    }
}
