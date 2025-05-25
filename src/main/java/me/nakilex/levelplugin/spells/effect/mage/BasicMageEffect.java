package me.nakilex.levelplugin.spells.effect.mage;

import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class BasicMageEffect implements SpellEffect {
    private final Random random = new Random();

    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        World world = player.getWorld();
        double damage = ctx.getFinalDamage();

        int range = parseInt(ctx.getExtraParam("basicRange"), 20);
        int rays = parseInt(ctx.getExtraParam("basicRays"), 3);
        double coneAngle = parseDouble(ctx.getExtraParam("basicConeAngle"), 30.0);
        double explosionRadius = parseDouble(ctx.getExtraParam("explosionRadius"), 2.0);
        double explosionPower = parseDouble(ctx.getExtraParam("explosionPower"), 2.0);
        double chainChance = parseDouble(ctx.getExtraParam("chainChance"), 0.3);
        int chainMaxTargets = parseInt(ctx.getExtraParam("chainMaxTargets"), 2);
        double chainRadius = parseDouble(ctx.getExtraParam("chainRadius"), 5.0);
        double chainDamageMultiplier = parseDouble(ctx.getExtraParam("chainDamageMultiplier"), 0.7);

        Location origin = player.getEyeLocation();
        Vector direction = origin.getDirection().normalize();

        // Fire multiple rays in a cone
        for (int ray = 0; ray < rays; ray++) {
            double offset = (ray - (rays - 1) / 2.0) * (coneAngle / (rays - 1));
            Vector dir = rotateYaw(direction, offset);
            for (int i = 1; i <= range; i++) {
                Location loc = origin.clone().add(dir.clone().multiply(i));
                world.spawnParticle(Particle.CRIT, loc, 1, 0, 0, 0, 0);

                // Check for entity collisions
                for (Entity e : world.getNearbyEntities(loc, 0.5, 0.5, 0.5)) {
                    if (!(e instanceof LivingEntity le) || le == player) continue;
                    boolean died = le.getHealth() - damage <= 0;
                    SpellUtils.dealWithChat(player, le, damage, "Basic Mage Attack");
                    world.spawnParticle(Particle.DAMAGE_INDICATOR, le.getLocation(), 10, 0.2, 0.2, 0.2, 0.02);
                    world.playSound(le.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 1.5f);

                    // Explode on death
                    if (died) {
                        world.createExplosion(le.getLocation(), (float) explosionPower, false, false);
                    }

                    // Chain lightning effect
                    if (random.nextDouble() < chainChance) {
                        chainLightning(le, world, player, damage * chainDamageMultiplier,
                            chainRadius, chainMaxTargets, explosionPower, chainDamageMultiplier);
                    }
                    return;
                }

                // If hits a block, spawn smoke and stop
                if (loc.getBlock().getType().isSolid()) {
                    world.spawnParticle(Particle.SMOKE, loc, 5, 0.2, 0.2, 0.2, 0.05);
                    world.playSound(loc, Sound.BLOCK_STONE_HIT, 1f, 0.8f);
                    break;
                }
            }
        }
    }

    /** Recursive chain lightning bounce */
    private void chainLightning(LivingEntity origin, World world, Player player,
                                double damage, double radius, int remainingTargets,
                                double explosionPower, double chainDamageMultiplier) {
        if (remainingTargets <= 0) return;
        List<LivingEntity> candidates = world.getNearbyEntities(origin.getLocation(), radius, radius, radius).stream()
            .filter(e -> e instanceof LivingEntity && e != player && e != origin)
            .map(e -> (LivingEntity) e)
            .collect(Collectors.toList());
        if (candidates.isEmpty()) return;

        LivingEntity target = candidates.get(random.nextInt(candidates.size()));
        world.strikeLightningEffect(target.getLocation());
        boolean died = target.getHealth() - damage <= 0;
        SpellUtils.dealWithChat(player, target, damage, "Chain Lightning");

        if (died) {
            world.createExplosion(target.getLocation(), (float) explosionPower, false, false);
        }
        // Continue bouncing
        chainLightning(target, world, player, damage * chainDamageMultiplier,
            radius, remainingTargets - 1, explosionPower, chainDamageMultiplier);
    }

    /** Rotate a vector around the Y-axis by degrees */
    private Vector rotateYaw(Vector v, double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double x = v.getX() * cos - v.getZ() * sin;
        double z = v.getX() * sin + v.getZ() * cos;
        return new Vector(x, v.getY(), z).normalize();
    }

    private double parseDouble(Object param, double defaultVal) {
        if (param instanceof Number) return ((Number) param).doubleValue();
        if (param instanceof String) {
            try { return Double.parseDouble((String) param); } catch (NumberFormatException ignored) {}
        }
        return defaultVal;
    }

    private int parseInt(Object param, int defaultVal) {
        if (param instanceof Number) return ((Number) param).intValue();
        if (param instanceof String) {
            try { return Integer.parseInt((String) param); } catch (NumberFormatException ignored) {}
        }
        return defaultVal;
    }
}
