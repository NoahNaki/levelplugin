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
     * offsets. The forward distance is applied along the player's viewing
     * direction. The forward distance is applied opposite the player's view
     * vector so positive values consistently place the location in front of the
     * player.
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
        // Bukkit's direction vector points toward where the player is looking.
        // Subtracting this vector (scaled by the forward distance) moves the
        // location in front of the player's eyes.
        Vector dir = player.getEyeLocation().getDirection()
                .normalize().multiply(forward);
        base.subtract(dir);
        base.add(x, y, z);
        return base;
    }
}
