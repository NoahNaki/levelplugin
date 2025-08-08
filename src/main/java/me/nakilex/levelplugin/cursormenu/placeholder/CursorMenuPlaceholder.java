package me.nakilex.levelplugin.cursormenu.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nakilex.levelplugin.cursormenu.CursorMenuManager;
import me.nakilex.levelplugin.cursormenu.menu.MenuLayout;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Basic PlaceholderAPI expansion for cursor menu state.
 */
public class CursorMenuPlaceholder extends PlaceholderExpansion {
    private final CursorMenuManager manager;

    public CursorMenuPlaceholder(CursorMenuManager manager) {
        this.manager = manager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "cursormenu";
    }

    @Override
    public @NotNull String getAuthor() {
        return "LevelPlugin";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        switch (params) {
            case "current_menu":
                return manager.getCurrentMenu(player);
            case "selected_option":
                MenuLayout layout = manager.getSectionManager()
                        .getLayout(manager.getCurrentMenu(player), 0);
                return layout != null ? layout.getId() : "";
            default:
                return "";
        }
    }
}
