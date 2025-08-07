package me.nakilex.levelplugin.cursormenu.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Utility helpers for positioning displays relative to a player's view.
 */
public final class DisplayUtils {
    private DisplayUtils() {}

    /**
     * Computes a location in front of the player's eyes with the provided
     * offsets. The forward distance is applied opposite to the player's
     * viewing direction so a positive value will always place the location
     * in front of the player.
     *
     * @param player  player whose view is used
     * @param forward distance in blocks in front of the player
     * @param x       X offset in world coordinates
     * @param y       Y offset in world coordinates
     * @param z       Z offset in world coordinates
     * @return location offset from the player's eye position
     */
    public static Location getRelativeLocation(Player player,
                                               double forward,
                                               double x,
                                               double y,
                                               double z) {
        Location base = player.getEyeLocation().clone();
        Vector dir = player.getEyeLocation().getDirection()
                .normalize().multiply(-forward);
        base.add(dir);
        base.add(x, y, z);
        return base;
    }
}
