package me.nakilex.levelplugin.waypoints.bukkit;

import me.nakilex.levelplugin.waypoints.api.pathing.Pathfinder;
import me.nakilex.levelplugin.waypoints.api.pathing.configuration.PathfinderConfiguration;
import me.nakilex.levelplugin.waypoints.api.pathing.result.Path;
import me.nakilex.levelplugin.waypoints.api.pathing.result.PathfinderResult;
import me.nakilex.levelplugin.waypoints.api.wrapper.PathPosition;
import me.nakilex.levelplugin.waypoints.engine.factory.AStarPathfinderFactory;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import me.nakilex.levelplugin.lootchests.utils.LocationUtils;

import java.util.Optional;

/**
 * Shared pathfinding adapter for Bukkit worlds using the waypoints engine.
 */
public class BukkitPathfindingService {
    private static final int DEFAULT_MAX_ITERATIONS = 1800;
    private static final int DEFAULT_MAX_LENGTH = 320;

    private final Pathfinder pathfinder;
    private final BukkitNavigationPointProvider provider;

    public BukkitPathfindingService() {
        this(DEFAULT_MAX_ITERATIONS, DEFAULT_MAX_LENGTH);
    }

    public BukkitPathfindingService(int maxIterations, int maxLength) {
        this.provider = new BukkitNavigationPointProvider();
        PathfinderConfiguration configuration = PathfinderConfiguration.builder()
                .provider(provider)
                .maxIterations(maxIterations)
                .maxLength(maxLength)
                .async(false)
                .build();
        this.pathfinder = new AStarPathfinderFactory().createPathfinder(configuration);
    }

    public Optional<Path> findPath(Location start, Location target) {
        if (start == null || target == null) {
            return Optional.empty();
        }
        Location resolvedStart = resolveWalkableLocation(start);
        Location resolvedTarget = resolveWalkableLocation(target);
        if (resolvedStart == null || resolvedTarget == null) {
            return Optional.empty();
        }
        World world = resolvedStart.getWorld();
        if (world == null || !world.equals(resolvedTarget.getWorld())) {
            return Optional.empty();
        }
        PathPosition startPos = new PathPosition(resolvedStart.getX(), resolvedStart.getY(), resolvedStart.getZ());
        PathPosition targetPos = new PathPosition(resolvedTarget.getX(), resolvedTarget.getY(), resolvedTarget.getZ());
        BukkitPathfindingContext context = new BukkitPathfindingContext(world, false);
        PathfinderResult result = pathfinder.findPath(startPos, targetPos, context).toCompletableFuture().join();
        if (result == null || !result.successful() || result.getPath() == null || result.getPath().length() == 0) {
            return Optional.empty();
        }
        return Optional.of(result.getPath());
    }

    private Location resolveWalkableLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        Location surface = LocationUtils.surfaceBelow(location, true);
        Location candidate = LocationUtils.firstAirAbove(surface, 3);
        if (candidate == null || candidate.getWorld() == null) {
            return null;
        }
        Block feet = candidate.getBlock();
        Block head = candidate.clone().add(0, 1, 0).getBlock();
        if (!feet.isPassable() || !head.isPassable()) {
            return null;
        }
        return candidate;
    }
}
