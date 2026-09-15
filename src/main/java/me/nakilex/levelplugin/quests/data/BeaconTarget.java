package me.nakilex.levelplugin.quests.data;

import me.nakilex.levelplugin.waypoints.api.provider.NavigationTarget;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Resolves the location for a navigation beacon.
 */
public interface BeaconTarget extends NavigationTarget {
    /**
     * Determine the location where the beacon should appear for the given viewer.
     *
     * @param viewer player requesting the location
     * @return resolved location or {@code null} if not available
     */
    @Override
    Location resolve(Player viewer);
}
