package me.nakilex.levelplugin.pathfinding;

import de.bsommerfeld.pathetic.api.pathing.Pathfinder;
import de.bsommerfeld.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.bsommerfeld.pathetic.api.pathing.result.PathfinderResult;
import de.bsommerfeld.pathetic.api.provider.NavigationPointProvider;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import de.bsommerfeld.pathetic.engine.factory.AStarPathfinderFactory;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import me.nakilex.levelplugin.lootchests.utils.LocationUtils;

import java.util.concurrent.CompletionStage;

/**
 * Wrapper around Pathetic's Pathfinder to offer Bukkit-aware pathfinding requests.
 */
public class PatheticPathfinderService {

    private final Pathfinder pathfinder;

    public PatheticPathfinderService() {
        NavigationPointProvider provider = new BukkitNavigationPointProvider();
        PathfinderConfiguration config = PathfinderConfiguration.builder()
                .provider(provider)
                .async(false)
                .maxIterations(8000)
                .maxLength(1024)
                .build();
        this.pathfinder = new AStarPathfinderFactory().createPathfinder(config);
    }

    public CompletionStage<PathfinderResult> findPath(World world, Location start, Location target) {
        if (world == null || start == null || target == null) {
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }
        Location normalizedStart = normalizeLocation(start);
        Location normalizedTarget = normalizeLocation(target);
        if (normalizedStart == null || normalizedTarget == null) {
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }
        PathPosition from = toPathPosition(normalizedStart);
        PathPosition to = toPathPosition(normalizedTarget);
        return pathfinder.findPath(from, to, new WorldContext(world));
    }

    public Location normalizeLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return location;
        }
        World world = location.getWorld();
        Block block = world.getBlockAt(location);
        Block above = block.getRelative(BlockFace.UP);
        Block below = block.getRelative(BlockFace.DOWN);
        boolean passable = block.isPassable() && above.isPassable();
        boolean grounded = below.getType().isSolid() || below.isLiquid();
        if (passable && grounded) {
            return location;
        }
        Location surface = LocationUtils.surfaceBelow(location, false);
        return LocationUtils.firstAirAbove(surface, 6);
    }

    private PathPosition toPathPosition(Location location) {
        return new PathPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private record WorldContext(World world) implements de.bsommerfeld.pathetic.api.pathing.context.EnvironmentContext {
    }

    private static class BukkitNavigationPointProvider implements NavigationPointProvider {
        @Override
        public de.bsommerfeld.pathetic.api.provider.NavigationPoint getNavigationPoint(
                PathPosition position,
                de.bsommerfeld.pathetic.api.pathing.context.EnvironmentContext environmentContext) {
            if (!(environmentContext instanceof WorldContext ctx)) {
                return () -> false;
            }
            World world = ctx.world();
            int x = position.getFlooredX();
            int y = position.getFlooredY();
            int z = position.getFlooredZ();
            if (y <= world.getMinHeight() || y >= world.getMaxHeight() - 1) {
                return () -> false;
            }
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                return () -> false;
            }
            Block block = world.getBlockAt(x, y, z);
            Block above = block.getRelative(BlockFace.UP);
            Block below = block.getRelative(BlockFace.DOWN);
            boolean passable = block.isPassable() && above.isPassable();
            boolean grounded = below.getType().isSolid() || below.isLiquid();
            return () -> passable && grounded;
        }
    }
}
