package me.nakilex.levelplugin.waypoints.bukkit;

import me.nakilex.levelplugin.waypoints.api.pathing.Pathfinder;
import me.nakilex.levelplugin.waypoints.api.pathing.configuration.PathfinderConfiguration;
import me.nakilex.levelplugin.waypoints.api.pathing.result.Path;
import me.nakilex.levelplugin.waypoints.api.pathing.result.PathfinderResult;
import me.nakilex.levelplugin.waypoints.api.wrapper.PathPosition;
import me.nakilex.levelplugin.waypoints.engine.factory.AStarPathfinderFactory;
import org.bukkit.Location;
import org.bukkit.World;

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
        World world = start.getWorld();
        if (world == null || !world.equals(target.getWorld())) {
            return Optional.empty();
        }
        PathPosition startPos = new PathPosition(start.getX(), start.getY(), start.getZ());
        PathPosition targetPos = new PathPosition(target.getX(), target.getY(), target.getZ());
        BukkitPathfindingContext context = new BukkitPathfindingContext(world, false);
        PathfinderResult result = pathfinder.findPath(startPos, targetPos, context).toCompletableFuture().join();
        if (result == null || result.getPath() == null || result.getPath().length() == 0) {
            return Optional.empty();
        }
        return Optional.of(result.getPath());
    }
}
