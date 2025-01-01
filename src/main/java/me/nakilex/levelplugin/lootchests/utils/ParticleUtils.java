package me.nakilex.levelplugin.lootchests.utils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

public class ParticleUtils {

    public static void displayTierParticles(Location location, int tier) {
        if (location == null) return;
        World world = location.getWorld();
        if (world == null) return;

        // All tiers use CRIT particles
        world.spawnParticle(
            Particle.CRIT,
            location.clone().add(0.5, 1, 0.5),
            60,
            0.3, 0.5, 0.3,
            0.01
        );
    }
}
