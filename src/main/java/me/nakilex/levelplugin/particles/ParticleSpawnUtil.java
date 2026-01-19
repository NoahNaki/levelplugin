package me.nakilex.levelplugin.particles;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

public final class ParticleSpawnUtil {
    private ParticleSpawnUtil() {}

    public static void spawn(World world, Location location, Particle particle, int count, Object data) {
        if (world == null || particle == null || location == null) {
            return;
        }
        if (data != null) {
            world.spawnParticle(particle, location, count, 0, 0, 0, 0, data);
        } else {
            world.spawnParticle(particle, location, count, 0, 0, 0, 0);
        }
    }
}
