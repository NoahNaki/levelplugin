package me.nakilex.levelplugin.quests.tasks;

import me.nakilex.levelplugin.waypoints.api.pathing.result.Path;
import me.nakilex.levelplugin.waypoints.api.wrapper.PathPosition;
import me.nakilex.levelplugin.waypoints.engine.result.PathUtils;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.quests.util.QuestNavigationUtil;
import me.nakilex.levelplugin.waypoints.bukkit.BukkitPathfindingService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Spawns a particle trail along the quest path so players can follow it.
 */
public class QuestPathTask extends BukkitRunnable {
    private static final Particle PATH_PARTICLE = Particle.DUST;
    private static final double CLOSE_DISTANCE = 8.0;
    private static final double REPATH_PLAYER_DISTANCE = 8.0;
    private static final double REPATH_TARGET_DISTANCE = 4.0;
    private static final long REPATH_INTERVAL_MS = 4000L;
    private static final double INTERPOLATION_STEP = 0.45;
    private static final int MAX_PARTICLE_POINTS = 1000;
    private static final int SKIP_POINTS = 0;
    private static final int SMOOTH_SAMPLES_PER_SEGMENT = 6;
    private static final int PARTICLE_COUNT = 1;
    private static final double PARTICLE_SPREAD = 0.0;
    private static final double PARTICLE_HEIGHT_OFFSET = 1.0;
    private static final int PARTICLE_STRIDE = 2;
    private static final Particle.DustOptions PATH_DUST = new Particle.DustOptions(
            Color.fromRGB(255, 165, 0), 1.2f);

    private final QuestManager questManager;
    private final BukkitPathfindingService pathfindingService;
    private final Map<UUID, QuestPathCache> cachedPaths = new HashMap<>();

    public QuestPathTask(QuestManager questManager, BukkitPathfindingService pathfindingService) {
        this.questManager = questManager;
        this.pathfindingService = pathfindingService;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            QuestNavigationUtil.QuestTrackingInfo tracking = QuestNavigationUtil.resolveTracking(player, questManager);
            if (tracking == null || tracking.location() == null) {
                clearCache(player.getUniqueId());
                continue;
            }

            Location target = tracking.location();
            Location playerLoc = player.getLocation();
            if (!isSameWorld(playerLoc, target)) {
                clearCache(player.getUniqueId());
                continue;
            }

            if (playerLoc.distance(target) < CLOSE_DISTANCE) {
                clearCache(player.getUniqueId());
                continue;
            }

            UUID playerId = player.getUniqueId();
            QuestPathCache cache = cachedPaths.get(playerId);
            if (shouldRepath(cache, playerLoc, target)) {
                cache = buildPathCache(playerLoc, target);
                cachedPaths.put(playerId, cache);
            }

            if (cache != null && !cache.points().isEmpty()) {
                renderParticles(player, cache.points());
            }
        }
    }

    private QuestPathCache buildPathCache(Location start, Location target) {
        long now = System.currentTimeMillis();
        Optional<Path> pathResult = pathfindingService.findPath(start, target);
        if (pathResult.isEmpty()) {
            return new QuestPathCache(start.clone(), target.clone(), now, List.of());
        }

        Path path = PathUtils.interpolate(pathResult.get(), INTERPOLATION_STEP);
        List<Location> points = toParticleLocations(start.getWorld(), path);
        points = smoothCatmullRom(points, SMOOTH_SAMPLES_PER_SEGMENT);
        points = limitPointCount(points, MAX_PARTICLE_POINTS);
        if (points.isEmpty() && path.length() > 0 && target.getWorld() != null) {
            points.add(new Location(
                    target.getWorld(),
                    target.getBlockX() + 0.5,
                    target.getY() + PARTICLE_HEIGHT_OFFSET,
                    target.getBlockZ() + 0.5));
        }
        return new QuestPathCache(start.clone(), target.clone(), now, points);
    }

    private List<Location> toParticleLocations(World world, Path path) {
        List<Location> points = new ArrayList<>();
        if (world == null || path == null) {
            return points;
        }
        int index = 0;
        for (PathPosition position : path) {
            if (index++ < SKIP_POINTS) {
                continue;
            }
            Location point = new Location(
                    world,
                    position.getCenteredX(),
                    position.getY() + PARTICLE_HEIGHT_OFFSET,
                    position.getCenteredZ());
            points.add(point);
        }
        return points;
    }

    private void renderParticles(Player player, List<Location> points) {
        int index = 0;
        for (Location point : points) {
            if (index++ % PARTICLE_STRIDE != 0) {
                continue;
            }
            player.spawnParticle(
                    PATH_PARTICLE,
                    point,
                    PARTICLE_COUNT,
                    PARTICLE_SPREAD,
                    PARTICLE_SPREAD,
                    PARTICLE_SPREAD,
                    0,
                    PATH_DUST);
        }
    }

    private boolean shouldRepath(QuestPathCache cache, Location playerLoc, Location target) {
        if (cache == null) {
            return true;
        }
        if (!isSameWorld(cache.start(), playerLoc) || !isSameWorld(cache.target(), target)) {
            return true;
        }
        if (cache.computedAt() + REPATH_INTERVAL_MS < System.currentTimeMillis()) {
            return true;
        }
        if (cache.start().distanceSquared(playerLoc) > REPATH_PLAYER_DISTANCE * REPATH_PLAYER_DISTANCE) {
            return true;
        }
        return cache.target().distanceSquared(target) > REPATH_TARGET_DISTANCE * REPATH_TARGET_DISTANCE;
    }

    private boolean isSameWorld(Location first, Location second) {
        return first != null
                && second != null
                && first.getWorld() != null
                && first.getWorld().equals(second.getWorld());
    }

    private void clearCache(UUID playerId) {
        cachedPaths.remove(playerId);
    }

    private List<Location> smoothCatmullRom(List<Location> points, int samplesPerSegment) {
        if (points == null || points.size() < 3 || samplesPerSegment <= 0) {
            return points == null ? List.of() : points;
        }
        List<Location> smoothed = new ArrayList<>();
        smoothed.add(points.get(0));
        for (int i = 0; i < points.size() - 1; i++) {
            Location p0 = points.get(Math.max(0, i - 1));
            Location p1 = points.get(i);
            Location p2 = points.get(i + 1);
            Location p3 = points.get(Math.min(points.size() - 1, i + 2));
            for (int step = 1; step <= samplesPerSegment; step++) {
                double t = (double) step / (samplesPerSegment + 1);
                smoothed.add(catmullRomPoint(p0, p1, p2, p3, t));
            }
            smoothed.add(p2);
        }
        return smoothed;
    }

    private Location catmullRomPoint(Location p0, Location p1, Location p2, Location p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        double x = 0.5 * ((2 * p1.getX())
                + (-p0.getX() + p2.getX()) * t
                + (2 * p0.getX() - 5 * p1.getX() + 4 * p2.getX() - p3.getX()) * t2
                + (-p0.getX() + 3 * p1.getX() - 3 * p2.getX() + p3.getX()) * t3);
        double y = 0.5 * ((2 * p1.getY())
                + (-p0.getY() + p2.getY()) * t
                + (2 * p0.getY() - 5 * p1.getY() + 4 * p2.getY() - p3.getY()) * t2
                + (-p0.getY() + 3 * p1.getY() - 3 * p2.getY() + p3.getY()) * t3);
        double z = 0.5 * ((2 * p1.getZ())
                + (-p0.getZ() + p2.getZ()) * t
                + (2 * p0.getZ() - 5 * p1.getZ() + 4 * p2.getZ() - p3.getZ()) * t2
                + (-p0.getZ() + 3 * p1.getZ() - 3 * p2.getZ() + p3.getZ()) * t3);
        return new Location(p1.getWorld(), x, y, z);
    }

    private List<Location> limitPointCount(List<Location> points, int maxPoints) {
        if (points == null || points.size() <= maxPoints) {
            return points == null ? List.of() : points;
        }
        int stride = (int) Math.ceil(points.size() / (double) maxPoints);
        List<Location> trimmed = new ArrayList<>();
        for (int i = 0; i < points.size(); i += stride) {
            trimmed.add(points.get(i));
        }
        return trimmed;
    }

    private record QuestPathCache(Location start, Location target, long computedAt, List<Location> points) {
    }
}
