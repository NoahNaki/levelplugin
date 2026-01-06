package me.nakilex.levelplugin.utils;

import me.nakilex.levelplugin.Main;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Utility for determining whether LevelPlugin features should be excluded in a world.
 */
public final class WorldExclusionUtil {

    private static final String CONFIG_KEY = "levelplugin.excluded-worlds";

    private WorldExclusionUtil() {
    }

    public static boolean isExcluded(Player player) {
        if (player == null) {
            return false;
        }
        return isExcluded(player.getWorld());
    }

    public static boolean isExcluded(World world) {
        if (world == null) {
            return false;
        }
        Set<String> excluded = getExcludedWorlds();
        if (excluded.isEmpty()) {
            return false;
        }
        return excluded.contains(world.getName().toLowerCase(Locale.ROOT));
    }

    public static boolean isEnabled(World world) {
        return !isExcluded(world);
    }

    private static Set<String> getExcludedWorlds() {
        Main plugin = Main.getInstance();
        if (plugin == null || plugin.getCustomConfig() == null) {
            return Set.of();
        }
        List<String> names = plugin.getCustomConfig().getStringList(CONFIG_KEY);
        if (names == null || names.isEmpty()) {
            return Set.of();
        }
        Set<String> lowered = new HashSet<>();
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            lowered.add(name.toLowerCase(Locale.ROOT));
        }
        return lowered;
    }
}
