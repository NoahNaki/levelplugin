package me.nakilex.levelplugin.stronghold;

import me.nakilex.levelplugin.Main;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Optional;

/** Shared config access for Stronghold generated-world template settings. */
public final class StrongholdTemplateConfig {
    public static final String TEMPLATE_WORLD_KEY = "stronghold.generated-world-template";
    public static final String TEMPLATE_ORIGIN_KEY = "stronghold.generated-world-template-origin";

    private static final String ORIGIN_WORLD_KEY = TEMPLATE_ORIGIN_KEY + ".world";
    private static final String ORIGIN_X_KEY = TEMPLATE_ORIGIN_KEY + ".x";
    private static final String ORIGIN_Y_KEY = TEMPLATE_ORIGIN_KEY + ".y";
    private static final String ORIGIN_Z_KEY = TEMPLATE_ORIGIN_KEY + ".z";

    private StrongholdTemplateConfig() {
    }

    public static String templateWorld(Main plugin) {
        if (plugin == null || plugin.getCustomConfig() == null) {
            return "";
        }
        return plugin.getCustomConfig().getString(TEMPLATE_WORLD_KEY, "").trim();
    }

    public static Optional<TemplateOrigin> templateOrigin(Main plugin) {
        if (plugin == null || plugin.getCustomConfig() == null || !plugin.getCustomConfig().contains(ORIGIN_X_KEY)) {
            return Optional.empty();
        }
        String worldName = plugin.getCustomConfig().getString(ORIGIN_WORLD_KEY, templateWorld(plugin)).trim();
        int x = plugin.getCustomConfig().getInt(ORIGIN_X_KEY);
        int y = plugin.getCustomConfig().getInt(ORIGIN_Y_KEY);
        int z = plugin.getCustomConfig().getInt(ORIGIN_Z_KEY);
        return Optional.of(new TemplateOrigin(worldName, x, y, z));
    }

    public static void setTemplate(Main plugin, World templateWorld, Location origin) {
        if (plugin == null || plugin.getCustomConfig() == null || templateWorld == null || origin == null) {
            return;
        }
        plugin.getCustomConfig().set(TEMPLATE_WORLD_KEY, templateWorld.getName());
        plugin.getCustomConfig().set(ORIGIN_WORLD_KEY, templateWorld.getName());
        plugin.getCustomConfig().set(ORIGIN_X_KEY, origin.getBlockX());
        plugin.getCustomConfig().set(ORIGIN_Y_KEY, origin.getBlockY());
        plugin.getCustomConfig().set(ORIGIN_Z_KEY, origin.getBlockZ());
        plugin.saveCustomConfig();
    }

    public static void clearTemplate(Main plugin) {
        if (plugin == null || plugin.getCustomConfig() == null) {
            return;
        }
        plugin.getCustomConfig().set(TEMPLATE_WORLD_KEY, "");
        plugin.getCustomConfig().set(TEMPLATE_ORIGIN_KEY, null);
        plugin.saveCustomConfig();
    }

    public static String formatOrigin(TemplateOrigin origin) {
        if (origin == null) {
            return ChatColor.GRAY + "unset";
        }
        return ChatColor.WHITE + "(" + origin.x() + ", " + origin.y() + ", " + origin.z() + ")"
                + ChatColor.GRAY + " in " + ChatColor.WHITE + origin.worldName();
    }

    public record TemplateOrigin(String worldName, int x, int y, int z) {
    }
}
