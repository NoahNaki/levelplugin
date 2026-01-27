package me.nakilex.levelplugin.utils.gui.widgets;

import java.util.function.Function;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class ActionWidget extends SlotWidget {
    @FunctionalInterface
    public interface ClickHandler {
        void handle(ClickType click, GuiContext context);
    }

    private final Function<GuiContext, ItemStack> renderer;
    private final ClickHandler clickHandler;

    public ActionWidget(int slot, Function<GuiContext, ItemStack> renderer, ClickHandler clickHandler) {
        super(slot);
        this.renderer = renderer;
        this.clickHandler = clickHandler;
    }

    @Override
    protected ItemStack render(GuiContext context) {
        return renderer == null ? null : renderer.apply(context);
    }

    @Override
    protected void handleClick(ClickType click, GuiContext context) {
        if (clickHandler != null) {
            clickHandler.handle(click, context);
        }
    }
}
