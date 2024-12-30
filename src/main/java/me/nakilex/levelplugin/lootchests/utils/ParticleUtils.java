package me.nakilex.levelplugin.lootchests.utils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

public class ParticleUtils {

    public static void displayTierParticles(Location location, int tier) {
        if (location == null) return;
        World world = location.getWorld();
        if (world == null) return;

        switch (tier) {
            case 1:
                // White sparkles
                world.spawnParticle(
                    Particle.HAPPY_VILLAGER,
                    location.clone().add(0.5, 1, 0.5),
                    50,
                    0.2, 0.5, 0.2,
                    0
                );
                break;
            case 2:
                // Yellow sparkles
                world.spawnParticle(
                    Particle.CRIT,
                    location.clone().add(0.5, 1, 0.5),
                    60,
                    0.3, 0.5, 0.3,
                    0.01
                );
                break;
            case 3:
                // Blue sparkles
                world.spawnParticle(
                    Particle.PORTAL,
                    location.clone().add(0.5, 1, 0.5),
                    75,
                    0.3, 0.5, 0.3,
                    0.01
                );
                break;
            case 4:
                // Green sparkles
                world.spawnParticle(
                    Particle.TOTEM_OF_UNDYING,
                    location.clone().add(0.5, 1, 0.5),
                    75,
                    0.3, 0.5, 0.3,
                    0.01
                );
                break;
            default:
                break;
        }
    }
}
