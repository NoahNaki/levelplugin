package me.nakilex.levelplugin.utils.gui.widgets;

import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.function.BooleanSupplier;

/** Generic GUI toggle widget for filter controls. */
public class ToggleFilterWidget extends SlotWidget {
    private final BooleanSupplier enabledSupplier;
    private final String name;
    private final String[] lore;
    private final ActionWidget.ClickHandler clickHandler;

    public ToggleFilterWidget(int slot, BooleanSupplier enabledSupplier, String name,
                              ActionWidget.ClickHandler clickHandler, String... lore) {
        super(slot);
        this.enabledSupplier = enabledSupplier;
        this.name = name;
        this.clickHandler = clickHandler;
        this.lore = lore == null ? new String[0] : lore.clone();
    }

    @Override
    protected ItemStack render(GuiContext context) {
        boolean enabled = enabledSupplier != null && enabledSupplier.getAsBoolean();
        return GuiUtil.createToggleItem(enabled, name, lore);
    }

    @Override
    protected void handleClick(ClickType click, GuiContext context) {
        if (clickHandler != null) {
            clickHandler.handle(click, context);
        }
    }
}
