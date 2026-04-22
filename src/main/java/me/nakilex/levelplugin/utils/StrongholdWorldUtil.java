package me.nakilex.levelplugin.utils;

import org.bukkit.World;

import java.util.Locale;

/**
 * Shared helpers for identifying stronghold-related worlds.
 */
public final class StrongholdWorldUtil {
    private static final String DEBUG_PREFIX = "stronghold_debug_";
    private static final String WORLD_KEYWORD = "stronghold";

    private StrongholdWorldUtil() {
    }

    public static boolean isStrongholdWorld(World world) {
        if (world == null) {
            return false;
        }
        return isStrongholdWorldName(world.getName());
    }

    public static boolean isStrongholdWorldName(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return false;
        }
        String lowered = worldName.toLowerCase(Locale.ROOT);
        return lowered.startsWith(DEBUG_PREFIX) || lowered.contains(WORLD_KEYWORD);
    }
}
