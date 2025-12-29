package me.nakilex.levelplugin.waypoints.bukkit;

import me.nakilex.levelplugin.waypoints.api.pathing.context.EnvironmentContext;
import org.bukkit.World;

/**
 * Environment context for Bukkit-based pathfinding.
 */
public record BukkitPathfindingContext(World world, boolean allowLiquids) implements EnvironmentContext {
}
