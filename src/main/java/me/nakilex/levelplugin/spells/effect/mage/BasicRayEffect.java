package me.nakilex.levelplugin.spells.effect.mage;

import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.Location;

import java.util.List;

/**
 * Fires one or more magical beams out of the caster’s eyes.
 * Supports “extraProjectiles” and “damageMultiplier” from runes.
 */
public class BasicRayEffect implements SpellEffect {
    private static final double MAX_DISTANCE = 30.0;
    private static final double STEP = 0.5;
    private static final double SPREAD_ANGLE_DEG = 10.0;

    public BasicRayEffect() {
        super();
    }

    @Override
    public void apply(SpellCastContext ctx) {
        Player caster   = ctx.getPlayer();
        Vector baseDir  = caster.getEyeLocation().getDirection().normalize();
        Location eyeLoc = caster.getEyeLocation().clone();

        // Debug: what extraProjectiles and damageMultiplier do we have?
        Object rawExtra = ctx.getExtraParam("extraProjectiles");
        Bukkit.getLogger().info("[DBG] BasicRayEffect: extraProjectiles=" + rawExtra);
        Object rawMult  = ctx.getExtraParam("damageMultiplier");
        Bukkit.getLogger().info("[DBG] BasicRayEffect: damageMultiplier=" + rawMult);

        // Compute number of beams
        int extra = 0;
        if (rawExtra instanceof Number) {
            extra = ((Number) rawExtra).intValue();
        } else if (rawExtra instanceof List<?>) {
            extra = ((List<?>) rawExtra).stream()
                .filter(n -> n instanceof Number)
                .mapToInt(n -> ((Number) n).intValue())
                .sum();
        }
        int totalBeams = 1 + extra;

        // Compute damage multiplier
        double dmgMult = 1.0;
        if (rawMult instanceof Number) {
            dmgMult = ((Number) rawMult).doubleValue();
        }

        // Fire each beam with a yaw spread
        for (int i = 0; i < totalBeams; i++) {
            double offsetDeg = 0.0;
            if (i > 0) {
                int band = (i + 1) / 2;
                offsetDeg = SPREAD_ANGLE_DEG * band * (i % 2 == 1 ? 1 : -1);
            }
            Vector dir = rotateY(baseDir, Math.toRadians(offsetDeg));
            castBeam(ctx, caster, eyeLoc.clone(), dir, dmgMult);
        }
    }


    private void castBeam(SpellCastContext ctx,
                          Player caster,
                          Location loc,
                          Vector dir,
                          double dmgMult) {
        double baseDamage = ctx.getFinalDamage();
        double damage     = baseDamage * dmgMult;

        for (double traveled = 0; traveled < MAX_DISTANCE; traveled += STEP) {
            loc.add(dir.clone().multiply(STEP));
            loc.getWorld().spawnParticle(Particle.WITCH, loc, 1);

            // stop on first hit
            for (var ent : loc.getWorld().getNearbyEntities(loc, 0.5, 0.5, 0.5)) {
                if (ent instanceof LivingEntity target && !target.equals(caster)) {
                    target.damage(damage, caster);
                    return;
                }
            }
        }
    }

    /** Rotate vector around the Y axis by the given radians. */
    private Vector rotateY(Vector v, double rad) {
        double cos = Math.cos(rad), sin = Math.sin(rad);
        double x = v.getX() * cos - v.getZ() * sin;
        double z = v.getX() * sin + v.getZ() * cos;
        return new Vector(x, v.getY(), z).normalize();
    }
}
