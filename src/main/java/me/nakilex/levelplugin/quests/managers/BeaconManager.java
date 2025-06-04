package me.nakilex.levelplugin.quests.managers;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

/**
 * Client-side, lime-green, rectangular beacon beam.
 */
public class BeaconManager implements Listener {

    /**
     * Draw a constant rectangular lime beam for {@code player}.
     *
     * @param player   viewer
     * @param location centre of the block that defines the X/Z of the column
     */
    public void showBeam(Player player, Location location) {
        if (player == null || location == null) return;

        World world = location.getWorld();
        if (world == null) return;

        Color rgb = DyeColor.LIME.getColor();
        Particle.DustOptions dust = new Particle.DustOptions(rgb, 1.8f); // slimmer line

        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;    // safety – never above build height

        double baseX = location.getX() + 0.5;   // block centre
        double baseZ = location.getZ() + 0.5;

        // Offsets so the four lines form a square “frame”
        double off = 0.25;

        for (double y = minY; y <= maxY; y += 1) {       // 1-block increments ⇒ fewer particles
            // NW, NE, SW, SE corners of the block
            player.spawnParticle(Particle.DUST, baseX - off, y, baseZ - off, 0, 0, 0, 0, 1, dust, true);
            player.spawnParticle(Particle.DUST, baseX + off, y, baseZ - off, 0, 0, 0, 0, 1, dust, true);
            player.spawnParticle(Particle.DUST, baseX - off, y, baseZ + off, 0, 0, 0, 0, 1, dust, true);
            player.spawnParticle(Particle.DUST, baseX + off, y, baseZ + off, 0, 0, 0, 0, 1, dust, true);
        }
    }

    // still stateless
    public void removeBeam(Player ignored) {}
}
