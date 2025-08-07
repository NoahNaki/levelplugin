package me.nakilex.levelplugin.cursormenu.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nakilex.levelplugin.cursormenu.CursorMenuPlugin;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI expansion exposing simple state of the cursor menu system.
 */
public class CursorMenuPlaceholder extends PlaceholderExpansion {
    private final CursorMenuPlugin plugin;

    public CursorMenuPlaceholder(CursorMenuPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "cursormenu";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params) {
        Player player = offlinePlayer.getPlayer();
        if (player == null) return "";
        return switch (params.toLowerCase()) {
            case "current_menu" -> plugin.getCurrentMenu(player);
            case "displayed_item" -> {
                String id = plugin.getItemDisplayManager().getPlayerActiveItemId(player);
                yield id != null ? id : "";
            }
            default -> "";
        };
    }
}
