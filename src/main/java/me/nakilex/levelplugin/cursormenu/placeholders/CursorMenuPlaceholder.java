package me.nakilex.levelplugin.cursormenu.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nakilex.levelplugin.cursormenu.CursorMenuManager;
import me.nakilex.levelplugin.cursormenu.layout.Section;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion exposing a few basic values from the cursor menu
 * system. This is intentionally minimal but provides a foundation for future
 * placeholders.
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
        return "levelplugin";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        Section section = manager.getSectionManager().get(params);
        return section != null ? section.getKey() : "";
    }
}
