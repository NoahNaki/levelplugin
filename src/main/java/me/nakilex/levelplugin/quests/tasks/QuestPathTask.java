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
    private static final double INTERPOLATION_STEP = 1.4;
    private static final int MAX_PARTICLE_POINTS = 70;
    private static final int SKIP_POINTS = 2;

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
        if (path.length() > MAX_PARTICLE_POINTS) {
            path = PathUtils.trim(path, MAX_PARTICLE_POINTS);
        }

        List<Location> points = toParticleLocations(start.getWorld(), path);
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
                    position.getFlooredY() + 0.15,
                    position.getCenteredZ());
            points.add(point);
        }
        return points;
    }

    private void renderParticles(Player player, List<Location> points) {
        for (Location point : points) {
            player.spawnParticle(PATH_PARTICLE, point, 1, 0, 0, 0, 0);
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

    private record QuestPathCache(Location start, Location target, long computedAt, List<Location> points) {
    }
}
