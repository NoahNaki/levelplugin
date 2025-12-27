package me.nakilex.levelplugin.waypoints;

import me.nakilex.levelplugin.quests.managers.BeaconManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Thin wrapper around {@link BeaconManager} for waypoint beacons.
 */
public class WaypointBeaconRenderer {
    private final BeaconManager beaconManager;

    public WaypointBeaconRenderer(BeaconManager beaconManager) {
        this.beaconManager = beaconManager;
    }

    public void show(Player player, Location target) {
        beaconManager.showBeam(player, target);
    }

    public void clear(Player player) {
        beaconManager.removeBeam(player);
    }

    public void clearAll() {
        beaconManager.removeAll();
    }
}
