package me.nakilex.levelplugin.utils.gui.widgets;

import org.bukkit.event.inventory.ClickType;

public interface GuiWidget {
    void contribute(GuiLayout layout, GuiContext context);

    void onClick(int slot, ClickType click, GuiContext context);

    default boolean handlesSlot(int slot) {
        return false;
    }
}
