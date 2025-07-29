package me.nakilex.levelplugin.utils;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
}
