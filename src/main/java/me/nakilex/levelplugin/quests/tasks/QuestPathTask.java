package me.nakilex.levelplugin.quests.tasks;

import me.nakilex.levelplugin.waypoints.api.pathing.result.Path;
import me.nakilex.levelplugin.waypoints.api.wrapper.PathPosition;
import me.nakilex.levelplugin.waypoints.engine.result.PathUtils;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.quests.util.QuestNavigationUtil;
import me.nakilex.levelplugin.waypoints.bukkit.BukkitPathfindingService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
    private static final Particle PATH_PARTICLE = Particle.END_ROD;
    private static final double CLOSE_DISTANCE = 8.0;
    private static final double REPATH_PLAYER_DISTANCE = 4.0;
    private static final double REPATH_TARGET_DISTANCE = 2.0;
    private static final long REPATH_INTERVAL_MS = 2500L;
    private static final double INTERPOLATION_STEP = 0.5;
    private static final int MAX_PARTICLE_POINTS = 400;
    private static final int SKIP_POINTS = 0;
    private static final int SMOOTH_ITERATIONS = 3;
    private static final int PARTICLE_COUNT = 4;
    private static final double PARTICLE_SPREAD = 0.08;

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
        points = smoothChaikin(points, SMOOTH_ITERATIONS);
        if (points.size() > MAX_PARTICLE_POINTS) {
            points = new ArrayList<>(points.subList(0, MAX_PARTICLE_POINTS));
        }
        if (points.isEmpty() && path.length() > 0 && target.getWorld() != null) {
            points.add(new Location(
                    target.getWorld(),
                    target.getBlockX() + 0.5,
                    target.getY() + 0.15,
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
                    position.getY() + 0.15,
                    position.getCenteredZ());
            points.add(point);
        }
        return points;
    }

    private void renderParticles(Player player, List<Location> points) {
        for (Location point : points) {
            player.spawnParticle(PATH_PARTICLE, point, PARTICLE_COUNT, PARTICLE_SPREAD, PARTICLE_SPREAD, PARTICLE_SPREAD, 0);
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

    private List<Location> smoothChaikin(List<Location> points, int iterations) {
        if (points == null || points.size() < 3) {
            return points == null ? List.of() : points;
        }
        List<Location> current = new ArrayList<>(points);
        for (int iter = 0; iter < iterations; iter++) {
            List<Location> next = new ArrayList<>(current.size() * 2);
            next.add(current.get(0));
            for (int i = 0; i < current.size() - 1; i++) {
                Location p0 = current.get(i);
                Location p1 = current.get(i + 1);
                double qx = 0.75 * p0.getX() + 0.25 * p1.getX();
                double qy = 0.75 * p0.getY() + 0.25 * p1.getY();
                double qz = 0.75 * p0.getZ() + 0.25 * p1.getZ();
                double rx = 0.25 * p0.getX() + 0.75 * p1.getX();
                double ry = 0.25 * p0.getY() + 0.75 * p1.getY();
                double rz = 0.25 * p0.getZ() + 0.75 * p1.getZ();
                next.add(new Location(p0.getWorld(), qx, qy, qz));
                next.add(new Location(p0.getWorld(), rx, ry, rz));
            }
            next.add(current.get(current.size() - 1));
            current = next;
        }
        return current;
    }

    private record QuestPathCache(Location start, Location target, long computedAt, List<Location> points) {
    }
}
