package me.nakilex.levelplugin.world;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nakilex.levelplugin.Main;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/** Keeps the Multiverse placeholders used by the existing chat configuration working. */
public final class MultiverseCorePlaceholderExpansion extends PlaceholderExpansion {
    private final Main plugin;
    private final WorldManager worldManager;

    public MultiverseCorePlaceholderExpansion(Main plugin, WorldManager worldManager) {
        this.plugin = plugin;
        this.worldManager = worldManager;
    }

    @Override public @NotNull String getIdentifier() { return "multiverse-core"; }
    @Override public @NotNull String getAuthor() { return String.join(", ", plugin.getDescription().getAuthors()); }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        return switch (params.toLowerCase(Locale.ROOT)) {
            case "alias" -> worldManager.getAlias(player.getWorld());
            case "world", "world_name", "worldname" -> player.getWorld().getName();
            case "environment" -> player.getWorld().getEnvironment().name().toLowerCase(Locale.ROOT);
            default -> null;
        };
    }
}
