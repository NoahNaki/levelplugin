package me.nakilex.levelplugin.waypoints.bukkit;

import me.nakilex.levelplugin.waypoints.api.pathing.result.Path;
import me.nakilex.levelplugin.waypoints.api.wrapper.PathPosition;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

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

    public static List<Location> downsampleByDistance(List<Location> points, double minDistance) {
        if (points == null || points.size() <= 1 || minDistance <= 0) {
            return points == null ? List.of() : points;
        }
        List<Location> sampled = new ArrayList<>();
        Location last = null;
        double minDistanceSq = minDistance * minDistance;
        for (Location point : points) {
            if (point == null) {
                continue;
            }
            if (last == null || last.distanceSquared(point) >= minDistanceSq) {
                sampled.add(point);
                last = point;
            }
        }
        if (!points.isEmpty()) {
            Location tail = points.get(points.size() - 1);
            if (!sampled.isEmpty() && !sampled.get(sampled.size() - 1).equals(tail)) {
                sampled.add(tail);
            }
        }
        return sampled;
    }

    public static void renderDustTrailToPlayer(Player player,
                                                List<Location> points,
                                                int stride,
                                                int count,
                                                double spread,
                                                Particle.DustOptions dustOptions) {
        if (player == null || points == null || points.isEmpty() || dustOptions == null) {
            return;
        }
        int safeStride = Math.max(1, stride);
        int safeCount = Math.max(1, count);
        double safeSpread = Math.max(0.0, spread);
        for (int i = 0; i < points.size(); i += safeStride) {
            Location point = points.get(i);
            if (point == null) {
                continue;
            }
            player.spawnParticle(Particle.DUST, point, safeCount, safeSpread, safeSpread, safeSpread, 0, dustOptions);
        }
    }
}
