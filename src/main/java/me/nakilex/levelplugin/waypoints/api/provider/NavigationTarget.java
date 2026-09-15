package me.nakilex.levelplugin.waypoints.api.provider;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Resolves a navigation destination for a particular player.
 *
 * <p>The target may be fixed or dynamic, such as an NPC whose location changes.</p>
 */
@FunctionalInterface
public interface NavigationTarget {
    Location resolve(Player viewer);
}
