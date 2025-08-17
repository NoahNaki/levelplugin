package me.nakilex.levelplugin.quests.data;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Beacon target that always resolves to a fixed location.
 */
public class StaticBeaconTarget implements BeaconTarget {
    private final Location location;

    public StaticBeaconTarget(Location location) {
        this.location = location;
    }

    @Override
    public Location resolve(Player viewer) {
        return location;
    }
}

