package me.nakilex.levelplugin.effects.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;

public class ParticleEffectUtil {

    public static void createShieldEffect(Location location, double radius, Particle particle, Material material) {
        for (int i = 0; i < 360; i += 15) {
            double angle = Math.toRadians(i);
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            location.getWorld().spawnParticle(particle, location.clone().add(x, 0, z), 1, material.createBlockData());
        }
    }

    public static void createSpiralEffect(Location location, double radius, Particle particle, int height) {
        for (int y = 0; y < height; y++) {
            double angle = y * 10;
            double radians = Math.toRadians(angle);
            double x = radius * Math.cos(radians);
            double z = radius * Math.sin(radians);
            location.getWorld().spawnParticle(particle, location.clone().add(x, y * 0.2, z), 1);
        }
    }

    public static void createBlockBreakingEffect(Location location, Material material, int particleCount) {
        for (int i = 0; i < particleCount; i++) {
            location.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE,
                location.getX() + Math.random() - 0.5,
                location.getY() + Math.random() - 0.5,
                location.getZ() + Math.random() - 0.5,
                1,
                0, 0, 0,
                material.createBlockData());
        }
    }

    public static void createVortexEffect(Location location, Particle particle, double radius, int height) {
        new BukkitRunnable() {
            double angle = 0;
            int yLevel = 0;

            @Override
            public void run() {
                if (yLevel >= height) {
                    cancel();
                    return;
                }

                // Calculate the coordinates for the vortex
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);

                // Spawn the particle at the calculated position
                Location particleLocation = location.clone().add(x, yLevel * 0.2, z);
                location.getWorld().spawnParticle(particle, particleLocation, 1, 0.1, 0.1, 0.1, 0.1);

                // Increment the angle and height for the spiral
                angle += Math.PI / 8; // Adjust this value for tighter/looser spirals
                if (angle >= 2 * Math.PI) {
                    angle = 0;
                    yLevel++;
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 1L);
    }


}
