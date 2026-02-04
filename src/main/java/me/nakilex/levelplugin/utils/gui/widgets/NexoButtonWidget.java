package me.nakilex.levelplugin.utils.gui.widgets;

import java.util.List;
import java.util.function.Function;

import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

/** Widget for rendering a Nexo item button with optional lore and click handling. */
public class NexoButtonWidget extends SlotWidget {
    private final String itemId;
    private final String name;
    private final Function<GuiContext, List<String>> loreBuilder;
    private final ActionWidget.ClickHandler clickHandler;

    public NexoButtonWidget(int slot, String itemId, String name,
                            Function<GuiContext, List<String>> loreBuilder,
                            ActionWidget.ClickHandler clickHandler) {
        super(slot);
        this.itemId = itemId;
        this.name = name;
        this.loreBuilder = loreBuilder;
        this.clickHandler = clickHandler;
    }

    @Override
    protected ItemStack render(GuiContext context) {
        List<String> lore = loreBuilder != null ? loreBuilder.apply(context) : null;
        return GuiUtil.getNexoItem(itemId, name, lore);
    }

    @Override
    protected void handleClick(ClickType click, GuiContext context) {
        if (clickHandler != null) {
            clickHandler.handle(click, context);
        }
    }
}
