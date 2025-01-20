package me.nakilex.levelplugin.effects.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.bukkit.Bukkit;

import static me.nakilex.levelplugin.effects.utils.ParticleEffectUtil.createBlockBreakingEffect;

public class ArmorStandEffectUtil {

    public static void createTrail(Location start, Location end, Particle particle, double density) {
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);

        for (double i = 0; i <= distance; i += density) {
            Location point = start.clone().add(direction.clone().multiply(i));
            point.getWorld().spawnParticle(particle, point, 1, 0.1, 0.1, 0.1, 0.1);
        }
    }

    public static void createShockwave(Location center, Particle particle, Material blockMaterial, double radius) {
        for (double angle = 0; angle < 360; angle += 10) {
            double radians = Math.toRadians(angle);
            double x = center.getX() + radius * Math.cos(radians);
            double z = center.getZ() + radius * Math.sin(radians);
            Location point = new Location(center.getWorld(), x, center.getY(), z);
            point.getWorld().spawnParticle(particle, point, 5, 0.2, 0.2, 0.2, blockMaterial.createBlockData());
        }
    }

    public static void spawnRotatingArmorStands(Location location, Material material, int count, int duration) {
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double x = location.getX() + 2 * Math.cos(angle);
            double z = location.getZ() + 2 * Math.sin(angle);

            Location spawnLoc = new Location(location.getWorld(), x, location.getY() + 1, z);
            ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
            armorStand.setVisible(false);
            armorStand.setGravity(false);
            armorStand.setItemInHand(new ItemStack(material));

            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (ticks++ >= duration) {
                        armorStand.remove();
                        cancel();
                    }
                    double newAngle = angle + ticks * 0.1;
                    double newX = location.getX() + 2 * Math.cos(newAngle);
                    double newZ = location.getZ() + 2 * Math.sin(newAngle);
                    armorStand.teleport(new Location(location.getWorld(), newX, location.getY() + 1, newZ));
                }
            }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 1L);
        }
    }

    public static void createTrailWithArmorStands(Location start, Location end, Material material, int count) {
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);

        for (int i = 0; i < count; i++) {
            double offset = i * distance / count;
            Location spawnLoc = start.clone().add(direction.clone().multiply(offset));
            ArmorStand armorStand = (ArmorStand) start.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
            armorStand.setVisible(false);
            armorStand.setGravity(false);
            armorStand.setItemInHand(new ItemStack(material));

            new BukkitRunnable() {
                @Override
                public void run() {
                    armorStand.remove();
                }
            }.runTaskLater(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 20L);
        }
    }

    public static void createLeadingArmorStand(Location start, Material material, int speed) {
        double distance = 10 * speed; // Calculate distance for 5 seconds

        ArmorStand armorStand = (ArmorStand) start.getWorld().spawnEntity(start, EntityType.ARMOR_STAND);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setItemInHand(new ItemStack(material));

        // Set the T-pose arm positions
        armorStand.setLeftArmPose(new EulerAngle(0, 0, Math.toRadians(-90))); // Left arm pointing sideways
        armorStand.setRightArmPose(new EulerAngle(0, 0, Math.toRadians(90))); // Right arm pointing sideways

        Vector direction = start.getDirection().normalize();

        new BukkitRunnable() {
            double traveledDistance = 0;
            float currentYaw = 0; // To keep track of the rotation angle

            @Override
            public void run() {
                if (traveledDistance >= distance) {
                    armorStand.remove();
                    cancel();
                    return;
                }

                // Calculate the new location
                Location currentLocation = armorStand.getLocation().add(direction.multiply(speed * 0.1));

                // Increment the yaw angle for rotation
                currentYaw += 10; // Rotate by 10 degrees each tick (adjust for smoother or faster rotation)
                if (currentYaw >= 360) {
                    currentYaw -= 360; // Reset yaw after a full rotation
                }

                // Apply the rotation to the armor stand
                currentLocation.setYaw(currentYaw);
                armorStand.teleport(currentLocation);

                // Optional: Create a trailing effect
                createBlockBreakingEffect(currentLocation, material, 5);

                traveledDistance += speed * 0.1;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 1L);
    }


    public static void createBoomerangArmorStand(Player player, Material material, int speed, double maxDistance) {
        Location startLocation = player.getLocation().clone();
        ArmorStand armorStand = (ArmorStand) startLocation.getWorld().spawnEntity(startLocation, EntityType.ARMOR_STAND);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setItemInHand(new ItemStack(material));

        // Set the T-pose arm positions
        armorStand.setLeftArmPose(new EulerAngle(0, 0, Math.toRadians(-90))); // Left arm pointing sideways
        armorStand.setRightArmPose(new EulerAngle(0, 0, Math.toRadians(90))); // Right arm pointing sideways

        Vector initialDirection = startLocation.getDirection().normalize(); // Outward direction
        boolean[] isReturning = {false}; // Track if it's returning

        new BukkitRunnable() {
            double traveledDistance = 0;
            float currentYaw = 0; // For rotating the armor stand

            @Override
            public void run() {
                if (!isReturning[0]) {
                    // Move outward
                    Location currentLocation = armorStand.getLocation().add(initialDirection.clone().multiply(speed * 0.1));
                    traveledDistance += speed * 0.1;

                    if (traveledDistance >= maxDistance) {
                        isReturning[0] = true; // Start returning
                    }

                    // Update armor stand's position and rotation
                    updateArmorStand(currentLocation);
                } else {
                    // Smoothly move toward the player's updated position
                    Location currentLocation = armorStand.getLocation();
                    Vector returnDirection = player.getLocation().toVector().subtract(currentLocation.toVector()).normalize();
                    currentLocation.add(returnDirection.multiply(speed * 0.1));

                    // Check if the armor stand has reached the player
                    if (currentLocation.distance(player.getLocation()) < 0.5) {
                        armorStand.remove();
                        cancel();
                        return;
                    }

                    // Update armor stand's position and rotation
                    updateArmorStand(currentLocation);
                }
            }

            private void updateArmorStand(Location currentLocation) {
                currentYaw += 10; // Rotate by 10 degrees each tick
                if (currentYaw >= 360) {
                    currentYaw -= 360;
                }
                currentLocation.setYaw(currentYaw);
                armorStand.teleport(currentLocation);
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, 1L);
    }


}
