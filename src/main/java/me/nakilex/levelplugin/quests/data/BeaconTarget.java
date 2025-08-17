package me.nakilex.levelplugin.quests.data;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Resolves the location for a navigation beacon.
 */
public interface BeaconTarget {
    /**
     * Determine the location where the beacon should appear for the given viewer.
     *
     * @param viewer player requesting the location
     * @return resolved location or {@code null} if not available
     */
    Location resolve(Player viewer);
}

