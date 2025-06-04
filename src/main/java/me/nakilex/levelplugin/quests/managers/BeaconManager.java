package me.nakilex.levelplugin.quests.managers;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

/**
 * Manages temporary particle "beacon" beams for navigation.
 * The effects are client side only and do not modify the world.
 */
public class BeaconManager implements Listener {

    /**
     * Show a colored beacon beam to a single player without modifying the world.
     *
     * @param player   the viewer
     * @param location the beacon base location
     * @param color    the beam color
     */
    public void showBeam(Player player, Location location, DyeColor color) {
        if (player == null || location == null) return;

        Color rgb = color.getColor();
        // Slightly larger size for a thicker beam
        Particle.DustOptions dust = new Particle.DustOptions(rgb, 2.5f);

        Location temp = location.clone().add(0.5, 0, 0.5);
        int max = location.getWorld().getMaxHeight();
        for (double y = location.getY(); y <= max; y += 0.5) {
            temp.setY(y);
            // Spawn a single DUST particle at each step to create a solid beam
            player.spawnParticle(Particle.DUST, temp, 0, 0, 0, 0, dust, true);
        }
    }

    public void removeBeam(Player player) {
        // no persistent state; nothing to clean up
    }
}

