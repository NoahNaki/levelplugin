package me.nakilex.levelplugin.waypoints.bukkit;

import me.nakilex.levelplugin.waypoints.api.pathing.context.EnvironmentContext;
import me.nakilex.levelplugin.waypoints.api.provider.NavigationPoint;
import me.nakilex.levelplugin.waypoints.api.provider.NavigationPointProvider;
import me.nakilex.levelplugin.waypoints.api.wrapper.PathPosition;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Provides traversable node checks backed by Bukkit world data.
 */
public class BukkitNavigationPointProvider implements NavigationPointProvider {

    @Override
    public NavigationPoint getNavigationPoint(PathPosition position, EnvironmentContext environmentContext) {
        if (!(environmentContext instanceof BukkitPathfindingContext context)) {
            return () -> false;
        }
        World world = context.world();
        if (world == null || position == null) {
            return () -> false;
        }
        int y = position.getFlooredY();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;
        if (y <= minY || y >= maxY) {
            return () -> false;
        }

        int x = position.getFlooredX();
        int z = position.getFlooredZ();

        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        Block ground = world.getBlockAt(x, y - 1, z);

        boolean standable = ground.getType().isSolid();
        if (!context.allowLiquids() && ground.isLiquid()) {
            standable = false;
        }

        boolean passable = feet.isPassable() && head.isPassable();
        return () -> standable && passable;
    }
}
