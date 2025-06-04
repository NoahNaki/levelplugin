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
        Particle.DustOptions dust = new Particle.DustOptions(rgb, 1.5f);
        Location temp = location.clone();
        temp.add(0.5, 0, 0.5);
        for (double y = 0; y <= 10; y += 0.5) {
            temp.setY(location.getY() + y);
            // Use the generic DUST particle which accepts DustOptions for color
            player.spawnParticle(Particle.DUST, temp, 0, 0, 0, 0, 0, dust, true);
        }
    }

    public void removeBeam(Player player) {
        // no persistent state; nothing to clean up
    }
}

