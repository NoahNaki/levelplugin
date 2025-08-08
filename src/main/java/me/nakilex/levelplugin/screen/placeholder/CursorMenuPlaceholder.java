package me.nakilex.levelplugin.screen.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.screen.CursorMenuSystem;
import me.nakilex.levelplugin.screen.menu.MenuLayout;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion exposing information about the cursor menus.
 */
public class CursorMenuPlaceholder extends PlaceholderExpansion {
    private final Main plugin;
    private final CursorMenuSystem system;

    public CursorMenuPlaceholder(Main plugin, CursorMenuSystem system) {
        this.plugin = plugin;
        this.system = system;
    }

    @Override public @NotNull String getIdentifier() { return "cursormenu"; }
    @Override public @NotNull String getAuthor() { return plugin.getDescription().getAuthors().toString(); }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        return switch (params.toLowerCase()) {
            case "current" -> {
                var sec = system.getSectionManager().get("default");
                yield sec != null ? sec.getKey() : "";
            }
            case "selected" -> {
                MenuLayout layout = null; // system could expose selected option
                yield layout != null ? String.valueOf(layout.getX()) : "";
            }
            default -> "";
        };
    }
}
