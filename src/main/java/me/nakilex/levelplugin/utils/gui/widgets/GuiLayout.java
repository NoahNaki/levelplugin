package me.nakilex.levelplugin.utils.gui.widgets;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class GuiLayout {
    private final Inventory inventory;

    public GuiLayout(Inventory inventory) {
        this.inventory = inventory;
    }

    public void setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    public Inventory inventory() {
        return inventory;
    }
}
