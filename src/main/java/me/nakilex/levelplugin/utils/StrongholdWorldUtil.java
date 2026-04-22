package me.nakilex.levelplugin.utils;

import org.bukkit.World;

import java.util.Locale;

/**
 * Shared world checks for Stronghold-specific systems.
 */
public final class StrongholdWorldUtil {

    private static final String GENERATED_STRONGHOLD_PREFIX = "stronghold_debug_";
    private static final String STRONGHOLD_TOKEN = "stronghold";

    private StrongholdWorldUtil() {
    }

    public static boolean isStrongholdWorld(World world) {
        if (world == null) {
            return false;
        }
        return isStrongholdWorld(world.getName());
    }

    public static boolean isStrongholdWorld(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return false;
        }
        String lowered = worldName.toLowerCase(Locale.ROOT);
        return lowered.startsWith(GENERATED_STRONGHOLD_PREFIX) || lowered.contains(STRONGHOLD_TOKEN);
    }
}
