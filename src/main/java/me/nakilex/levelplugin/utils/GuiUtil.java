package me.nakilex.levelplugin.utils;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Utility helpers for basic GUI elements. */
public final class GuiUtil {
    private GuiUtil() {}

    /** Create a simple filler pane with a blank display name. */
    public static ItemStack createFiller(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Build a Nexo item with a custom name or a barrier if missing. */
    public static ItemStack getNexoItem(String id, String name) {
        ItemBuilder builder = NexoItems.itemFromId(id);
        ItemStack item = builder != null ? builder.build() : new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Create a standardized toggle item using check/cross Nexo icons and lore
     * indicating the current enabled status.
     *
     * @param enabled whether the feature is enabled
     * @param name    display name of the item
     * @param extraLore optional additional lore lines (e.g. instructions)
     * @return configured ItemStack representing the toggle state
     */
    public static ItemStack createToggleItem(boolean enabled, String name, String... extraLore) {
        ItemStack base = getNexoItem(enabled ? "check" : "cross", name);
        ItemMeta meta = base.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add("§7Status: " + (enabled ? "§aEnabled" : "§cDisabled"));
            if (extraLore != null && extraLore.length > 0) {
                lore.add(" ");
                for (String line : extraLore) {
                    lore.add(line);
                }
            }
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            base.setItemMeta(meta);
        }
        return base;
    }

    /**
     * Fill the outer border of an inventory with the given filler item.
     * This is reused by multiple GUI classes.
     */
    public static void fillBorder(org.bukkit.inventory.Inventory inv, ItemStack filler) {
        int size = inv.getSize();
        int cols = 9;
        int rows = size / cols;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int slot = row * cols + col;
                if (row == 0 || row == rows - 1 || col == 0 || col == cols - 1) {
                    inv.setItem(slot, filler);
                }
            }
        }
    }

    /**
     * Generate a star rating string using the provided characters.
     *
     * @param value number of filled stars
     * @param max   maximum number of stars to display
     * @param filled character(s) to use for a filled star
     * @param empty  character(s) to use for an empty star
     * @return star string of length {@code max}
     */
    public static String generateStars(int value, int max, String filled, String empty) {
        StringBuilder sb = new StringBuilder(max * Math.max(filled.length(), empty.length()));
        for (int i = 0; i < value && i < max; i++) sb.append(filled);
        for (int i = value; i < max; i++) sb.append(empty);
        return sb.toString();
    }

    /** Convenience wrapper using "✦" and "✧" characters. */
    public static String generateStars(int value, int max) {
        return generateStars(value, max, "✦", "✧");
    }

    /** Generate star glyphs repeated the given number of times. */
    public static String glyphStars(int count) {
        if (count <= 0) return "";
        return "<glyph:star>".repeat(count);
    }
}
