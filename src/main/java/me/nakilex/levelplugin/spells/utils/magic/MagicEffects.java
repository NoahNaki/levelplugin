package me.nakilex.levelplugin.spells.utils.magic;

import me.nakilex.levelplugin.spells.utils.animation.SpellAnimation;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 * Minimal particle helper inspired by MagicSpells' effects.
 */
public final class MagicEffects {
    private MagicEffects() {}

    /**
     * Draws a circle of particles at the given location.
     * @param world the world
     * @param center the center location
     * @param particle the particle to spawn
     * @param radius radius of the circle
     * @param points number of points around the circle
     */
    public static void circle(World world, Location center, Particle particle, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            Vector offset = new Vector(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            world.spawnParticle(particle, center.clone().add(offset), 0, 0, 0, 0, 0);
        }
    }

    /**
     * Plays a rising helix animation for the specified duration.
     * @param center the base location
     * @param particle particle type
     * @param radius helix radius
     * @param height total height
     * @param loops number of helix loops
     * @param duration total ticks to play
     */
    public static void helix(Location center, Particle particle, double radius, double height, int loops, int duration) {
        World world = center.getWorld();
        new SpellAnimation(1, duration) {
            @Override
            protected void onTick(int tick) {
                double progress = (double) tick / duration;
                double angle = progress * loops * 2 * Math.PI;
                double y = progress * height;
                Vector offset = new Vector(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
                world.spawnParticle(particle, center.clone().add(offset), 0, 0, 0, 0, 0);
            }
        };
    }
}
