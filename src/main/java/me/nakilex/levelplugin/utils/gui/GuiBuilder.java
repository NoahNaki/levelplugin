package me.nakilex.levelplugin.utils.gui;

import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Simple fluent builder for inventories that automatically applies filler items
 * and border decoration.  The builder delegates creation of filler panes and
 * border filling to {@link GuiUtil} so behaviour remains consistent across
 * different GUIs.
 */
public final class GuiBuilder {

    private final Inventory inventory;
    private ItemStack filler;
    private boolean fillEmpty = true;

    private GuiBuilder(int size, String title) {
        this.inventory = Bukkit.createInventory(null, size, title);
    }

    /** Create a builder for the given size and title. */
    public static GuiBuilder create(int size, String title) {
        return new GuiBuilder(size, title);
    }

    /** Specify the material used for filler panes. */
    public GuiBuilder filler(Material material) {
        this.filler = GuiUtil.createFiller(material);
        return this;
    }

    /** Fill the outer border of the inventory with the configured filler. */
    public GuiBuilder border() {
        if (filler == null) {
            filler(Material.GRAY_STAINED_GLASS_PANE);
        }
        GuiUtil.fillBorder(inventory, filler);
        return this;
    }

    /**
     * Control whether {@link #build()} should fill remaining empty slots with
     * the filler item. Defaults to {@code true}.
     */
    public GuiBuilder fillEmptySlots(boolean fill) {
        this.fillEmpty = fill;
        return this;
    }

    /** Place an item at the given slot. */
    public GuiBuilder setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
        return this;
    }

    /**
     * Finalize the inventory. Any remaining empty slots will be filled with
     * the configured filler item if present.
     */
    public Inventory build() {
        if (fillEmpty && filler != null) {
            for (int i = 0; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) == null) {
                    inventory.setItem(i, filler);
                }
            }
        }
        return inventory;
    }
}
