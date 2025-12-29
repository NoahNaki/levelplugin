package me.nakilex.levelplugin.waypoints.bukkit;

import me.nakilex.levelplugin.waypoints.api.pathing.result.Path;
import me.nakilex.levelplugin.waypoints.api.wrapper.PathPosition;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared conversion helpers for pathing output.
 */
public final class PathLocationUtils {
    private PathLocationUtils() {
    }

    public static List<Location> toLocations(World world,
                                             Path path,
                                             double yOffset,
                                             boolean centered,
                                             int skipPoints) {
        List<Location> points = new ArrayList<>();
        if (world == null || path == null) {
            return points;
        }
        int index = 0;
        for (PathPosition position : path) {
            if (index++ < skipPoints) {
                continue;
            }
            double x = centered ? position.getCenteredX() : position.getX();
            double z = centered ? position.getCenteredZ() : position.getZ();
            points.add(new Location(world, x, position.getY() + yOffset, z));
        }
        return points;
    }
}
