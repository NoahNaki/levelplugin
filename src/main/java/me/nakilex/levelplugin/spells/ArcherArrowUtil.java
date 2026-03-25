package me.nakilex.levelplugin.spells;

import org.bukkit.Location;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/** Shared utility for spawning and configuring class-driven archer arrows. */
public final class ArcherArrowUtil {
    private static final String BASIC_ATTACK_META = "BasicAttack";

    private ArcherArrowUtil() {
    }

    public static Arrow launchClassArrow(Plugin plugin,
                                         Player caster,
                                         Vector direction,
                                         double speed,
                                         double damage) {
        if (plugin == null || caster == null || direction == null || direction.lengthSquared() <= 0.000001) {
            return null;
        }
        Vector velocity = direction.clone().normalize().multiply(Math.max(0.1, speed));
        return spawnConfiguredArrow(plugin, caster, caster.getEyeLocation(), velocity, damage);
    }

    public static Arrow spawnClassArrow(Plugin plugin,
                                        Player caster,
                                        Location spawn,
                                        Vector velocity,
                                        double damage) {
        if (plugin == null || caster == null || spawn == null || spawn.getWorld() == null || velocity == null
                || velocity.lengthSquared() <= 0.000001) {
            return null;
        }
        return spawnConfiguredArrow(plugin, caster, spawn, velocity, damage);
    }

    private static Arrow spawnConfiguredArrow(Plugin plugin,
                                              Player caster,
                                              Location spawn,
                                              Vector velocity,
                                              double damage) {
        Arrow arrow = spawn.getWorld().spawnArrow(spawn, velocity.clone().normalize(), (float) velocity.length(), 0.0f);
        arrow.setShooter(caster);
        arrow.setCritical(true);
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrow.setDamage(Math.max(0.1, damage));
        arrow.setMetadata(BASIC_ATTACK_META, new FixedMetadataValue(plugin, caster.getUniqueId()));
        return arrow;
    }


    public static List<Vector> buildHorizontalConeDirections(Vector forward,
                                                             int projectileCount,
                                                             double coneDegrees) {
        if (forward == null || forward.lengthSquared() <= 0.000001) {
            return List.of();
        }
        int safeCount = Math.max(1, projectileCount);
        Vector base = forward.clone().normalize();
        if (safeCount == 1 || coneDegrees <= 0.0) {
            return List.of(base);
        }

        List<Vector> directions = new ArrayList<>(safeCount);
        double step = coneDegrees / Math.max(1, safeCount - 1);
        for (int i = 0; i < safeCount; i++) {
            double yawOffset = (-coneDegrees / 2.0) + (step * i);
            directions.add(base.clone().rotateAroundY(Math.toRadians(yawOffset)));
        }
        return directions;
    }

    public static void attachHomingTask(Plugin plugin,
                                        Player caster,
                                        Arrow arrow,
                                        double homingStrength,
                                        int maxTicks,
                                        double targetRange,
                                        double targetRadius) {
        if (plugin == null || caster == null || arrow == null) {
            return;
        }
        double clampedStrength = Math.max(0.02, Math.min(0.55, homingStrength));
        int safeTicks = Math.max(1, maxTicks);
        double safeRange = Math.max(1.0, targetRange);
        double safeRadius = Math.max(0.1, targetRadius);

        new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                if (!caster.isOnline() || !arrow.isValid() || arrow.isOnGround() || arrow.isInBlock() || ticks++ > safeTicks) {
                    cancel();
                    return;
                }

                Location point = arrow.getLocation();
                LivingEntity target = SpellTargetingUtil.resolveTargetLivingEntity(caster, safeRange, safeRadius,
                        living -> !living.equals(caster)
                                && living.getWorld().equals(point.getWorld())
                                && living.getLocation().distanceSquared(point) <= safeRange * safeRange);
                if (target == null) {
                    return;
                }

                Location targetPoint = target.getLocation().clone().add(0.0, Math.min(1.2, target.getHeight() * 0.55), 0.0);
                Vector desired = targetPoint.toVector().subtract(point.toVector());
                if (desired.lengthSquared() <= 0.000001) {
                    return;
                }
                Vector current = arrow.getVelocity();
                Vector next = current.multiply(1.0 - clampedStrength)
                        .add(desired.normalize().multiply(current.length() * clampedStrength));
                if (next.lengthSquared() <= 0.000001) {
                    return;
                }
                arrow.setVelocity(next);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
