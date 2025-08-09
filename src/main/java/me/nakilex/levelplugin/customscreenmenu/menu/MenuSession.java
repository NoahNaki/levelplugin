package me.nakilex.levelplugin.customscreenmenu.menu;

import org.bukkit.entity.ItemDisplay;
import java.util.Optional;

public class MenuSession {
    private MenuDefinition menu;
    private ItemDisplay showcase;

    public MenuDefinition getMenu() {
        return menu;
    }

    public void setMenu(MenuDefinition menu) {
        this.menu = menu;
    }

    public Optional<ItemDisplay> getShowcase() {
        return Optional.ofNullable(showcase);
    }

    public void setShowcase(ItemDisplay showcase) {
        this.showcase = showcase;
    }
}
