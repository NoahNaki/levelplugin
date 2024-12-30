package me.nakilex.levelplugin.lootchests.utils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

public class ParticleUtils {

    /**
     * Spawns tier-based particle effects at a specified location.
     *
     * @param location Location to spawn the particles.
     * @param tier     Which tier's effect to display (1–4).
     */
    public static void displayTierParticles(Location location, int tier) {
        if (location == null) return;
        World world = location.getWorld();
        if (world == null) return;

        switch (tier) {
            case 1:
                // Example: Small white sparkles
                // VILLAGER_HAPPY is a small, subtle particle effect
                world.spawnParticle(
                    Particle.HAPPY_VILLAGER,  // Particle type
                    location.add(0.5, 1, 0.5), // Slight offset so it's above the chest center
                    10,                       // Number of particles
                    0.3, 0.5, 0.3,            // Spread in X, Y, Z
                    0                          // Extra speed or offset
                );
                break;

            case 2:
                // Example: Yellow sparkles
                // CRIT is a decent choice; set "extra" so it's visible
                world.spawnParticle(
                    Particle.CRIT,
                    location.add(0.5, 1, 0.5),
                    15,
                    0.3, 0.5, 0.3,
                    0.01
                );
                break;

            case 3:
                // Example: Blue sparkles
                // SPELL_WITCH is a purple swirl, but you could use PORTAL (purple) or CRIT_MAGIC (blueish)
                world.spawnParticle(
                    Particle.PORTAL,
                    location.add(0.5, 1, 0.5),
                    20,
                    0.3, 0.5, 0.3,
                    0.01
                );
                break;

            case 4:
                // Example: Green sparkles
                // TOTEM has a greenish swirl effect
                world.spawnParticle(
                    Particle.TOTEM_OF_UNDYING,
                    location.add(0.5, 1, 0.5),
                    20,
                    0.3, 0.5, 0.3,
                    0.01
                );
                break;

            default:
                // Fallback: no effect or a default effect
                break;
        }
    }
}
