// ParticleEffectUtil.java
package me.nakilex.levelplugin.effects.utils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

public class ParticleEffectUtil {

    public static void spawnCircleParticles(Location center, double radius, int points, Particle particle) {
        World world = center.getWorld();
        if (world == null) return;

        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location particleLocation = new Location(world, x, center.getY(), z);
            world.spawnParticle(particle, particleLocation, 1);
        }
    }
}
