package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.function.Predicate;

public final class WarriorCombatUtil {
    private WarriorCombatUtil() {
    }

    public static int strikeCone(Main plugin,
                                 Player caster,
                                 Location origin,
                                 double range,
                                 double halfAngleDegrees,
                                 double damage,
                                 double knockback) {
        if (plugin == null || caster == null || origin == null || origin.getWorld() == null) {
            return 0;
        }
        Vector forward = caster.getLocation().getDirection().clone();
        if (forward.lengthSquared() <= 0.0001) {
            return 0;
        }
        forward.normalize();
        double minDot = Math.cos(Math.toRadians(Math.max(1.0, halfAngleDegrees)));

        List<LivingEntity> targets = SpellEffectUtil.getLivingTargets(origin, range,
                living -> !living.equals(caster));
        int hitCount = 0;
        for (LivingEntity target : targets) {
            Vector toTarget = target.getLocation().toVector().subtract(origin.toVector());
            if (toTarget.lengthSquared() <= 0.0001) {
                continue;
            }
            Vector directionToTarget = toTarget.normalize();
            if (forward.dot(directionToTarget) < minDot) {
                continue;
            }
            SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, damage, true);
            Vector shove = directionToTarget.clone().multiply(knockback).setY(0.08);
            target.setVelocity(target.getVelocity().multiply(0.75).add(shove));
            hitCount++;
        }
        return hitCount;
    }

    public static void runRadialPulse(Main plugin,
                                      Player caster,
                                      Location center,
                                      int pulseCount,
                                      long intervalTicks,
                                      double baseRadius,
                                      double radiusStep,
                                      double baseDamage,
                                      double damageStep) {
        if (plugin == null || caster == null || center == null || center.getWorld() == null) {
            return;
        }
        new BukkitRunnable() {
            private int pulse;

            @Override
            public void run() {
                if (!caster.isOnline() || pulse >= pulseCount) {
                    cancel();
                    return;
                }
                Location pulseCenter = center.clone();
                double radius = baseRadius + (pulse * radiusStep);
                double damage = baseDamage + (pulse * damageStep);

                pulseCenter.getWorld().spawnParticle(Particle.CRIT, pulseCenter.clone().add(0.0, 0.5, 0.0),
                        16 + pulse * 6, radius * 0.3, 0.25, radius * 0.3, 0.02);
                pulseCenter.getWorld().spawnParticle(Particle.CLOUD, pulseCenter.clone().add(0.0, 0.2, 0.0),
                        12 + pulse * 5, radius * 0.35, 0.08, radius * 0.35, 0.01);
                pulseCenter.getWorld().playSound(pulseCenter, Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                        0.85f, 0.9f + (pulse * 0.08f));

                for (LivingEntity target : SpellEffectUtil.getLivingTargets(pulseCenter, radius,
                        living -> !living.equals(caster))) {
                    SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, damage, true);
                }
                pulse++;
            }
        }.runTaskTimer(plugin, 0L, Math.max(1L, intervalTicks));
    }

    public static void runShockwaveRipple(Main plugin,
                                          Player caster,
                                          Location center,
                                          int pulseCount,
                                          long intervalTicks,
                                          double baseRadius,
                                          double radiusStep,
                                          double baseDamage,
                                          double damageStep,
                                          double knockback) {
        if (plugin == null || caster == null || center == null || center.getWorld() == null) {
            return;
        }
        World world = center.getWorld();
        new BukkitRunnable() {
            private int pulse;

            @Override
            public void run() {
                if (!caster.isOnline() || pulse >= pulseCount) {
                    cancel();
                    return;
                }
                double radius = baseRadius + (pulse * radiusStep);
                double damage = baseDamage + (pulse * damageStep);

                spawnGroundRipple(plugin, world, center, radius);
                world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.45f, 1.35f - (pulse * 0.08f));

                for (LivingEntity target : SpellEffectUtil.getLivingTargets(center, radius,
                        living -> !living.equals(caster))) {
                    SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, damage, true);
                    Vector away = target.getLocation().toVector().subtract(center.toVector());
                    away.setY(0.0);
                    if (away.lengthSquared() <= 0.0001) {
                        away = caster.getLocation().getDirection().clone().setY(0.0);
                    }
                    away.normalize().multiply(knockback).setY(0.12 + (pulse * 0.01));
                    target.setVelocity(target.getVelocity().multiply(0.72).add(away));
                }
                pulse++;
            }
        }.runTaskTimer(plugin, 0L, Math.max(1L, intervalTicks));
    }

    public static void leapAndSlam(Main plugin,
                                   Player caster,
                                   Vector launchVelocity,
                                   int maxAirTicks,
                                   double impactRadius,
                                   double impactDamage,
                                   Predicate<LivingEntity> extraFilter) {
        if (plugin == null || caster == null || launchVelocity == null) {
            return;
        }
        caster.setVelocity(launchVelocity);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.8f, 0.9f);

        new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                if (!caster.isOnline()) {
                    cancel();
                    return;
                }
                if (ticks >= maxAirTicks || (ticks > 2 && caster.isOnGround())) {
                    Location impact = caster.getLocation().clone();
                    impact.getWorld().spawnParticle(Particle.EXPLOSION, impact.clone().add(0.0, 0.1, 0.0), 1);
                    impact.getWorld().spawnParticle(Particle.CLOUD, impact.clone().add(0.0, 0.2, 0.0),
                            28, 0.7, 0.15, 0.7, 0.02);
                    impact.getWorld().playSound(impact, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.85f, 1.05f);
                    for (LivingEntity target : SpellEffectUtil.getLivingTargets(impact, impactRadius,
                            living -> !living.equals(caster) && (extraFilter == null || extraFilter.test(living)))) {
                        SpellEffectUtil.applyDirectSpellDamage(plugin, caster, target, impactDamage, true);
                    }
                    cancel();
                    return;
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private static void spawnGroundRipple(Main plugin, World world, Location center, double radius) {
        if (world == null || center == null) {
            return;
        }
        int segments = Math.max(10, (int) Math.round(radius * 16.0));
        for (int i = 0; i < segments; i++) {
            double angle = (Math.PI * 2.0 * i) / segments;
            double x = center.getX() + (Math.cos(angle) * radius);
            double z = center.getZ() + (Math.sin(angle) * radius);
            Location sample = new Location(world, x, center.getY() + 0.35, z);
            Block ground = findGroundBlock(sample);
            if (ground == null || !ground.getType().isSolid()) {
                continue;
            }
            Location fx = ground.getLocation().add(0.5, 1.02, 0.5);
            world.spawnParticle(Particle.BLOCK, fx, 3, 0.16, 0.06, 0.16, 0.01, ground.getBlockData());
            world.spawnParticle(Particle.CLOUD, fx, 1, 0.06, 0.01, 0.06, 0.001);
            spawnRippleFallingBlock(plugin, fx, ground.getBlockData(), center);
        }
    }

    private static void spawnRippleFallingBlock(Main plugin, Location spawn, org.bukkit.block.data.BlockData data, Location center) {
        if (plugin == null || spawn == null || data == null || spawn.getWorld() == null) {
            return;
        }
        FallingBlock fb = spawn.getWorld().spawnFallingBlock(spawn, data);
        fb.setDropItem(false);
        fb.setHurtEntities(false);
        fb.setGravity(true);
        Vector outward = spawn.toVector().subtract(center.toVector()).setY(0.0);
        if (outward.lengthSquared() < 0.0001) {
            outward = new Vector(0.0, 0.0, 0.0);
        } else {
            outward.normalize().multiply(0.06);
        }
        fb.setVelocity(outward.setY(0.14));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (fb.isValid()) {
                fb.remove();
            }
        }, 8L);
    }

    private static Block findGroundBlock(Location sample) {
        if (sample == null || sample.getWorld() == null) {
            return null;
        }
        Block block = sample.getBlock();
        World world = sample.getWorld();
        int minY = world.getMinHeight();
        int checks = 6;
        while (checks-- > 0 && block.getY() > minY) {
            if (block.getType().isSolid() && block.getType() != Material.BARRIER) {
                return block;
            }
            block = block.getRelative(0, -1, 0);
        }
        return null;
    }
}
