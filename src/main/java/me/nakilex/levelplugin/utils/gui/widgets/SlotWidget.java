package me.nakilex.levelplugin.utils.gui.widgets;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public abstract class SlotWidget implements GuiWidget {
    private final int slot;

    protected SlotWidget(int slot) {
        this.slot = slot;
    }

    public int slot() {
        return slot;
    }

    @Override
    public void contribute(GuiLayout layout, GuiContext context) {
        layout.setItem(slot, render(context));
    }

    @Override
    public void onClick(int slot, ClickType click, GuiContext context) {
        if (this.slot == slot) {
            handleClick(click, context);
        }
    }

    @Override
    public boolean handlesSlot(int slot) {
        return this.slot == slot;
    }

    protected abstract ItemStack render(GuiContext context);

    protected void handleClick(ClickType click, GuiContext context) {}
}
