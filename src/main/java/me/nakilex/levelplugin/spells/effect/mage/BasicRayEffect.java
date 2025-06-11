package me.nakilex.levelplugin.spells.effect.mage;

import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import org.bukkit.*;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Fires one or more magical beams out of the caster’s eyes.
 * Supports “extraProjectiles” and “damageMultiplier” from runes.
 */
public class BasicRayEffect implements SpellEffect {
    private static final double MAX_DISTANCE = 20.0;
    private static final double STEP = 2.0;
    private static final double SPREAD_ANGLE_DEG = 10.0;

    public BasicRayEffect() {
        super();
    }

    @Override
    public void apply(SpellCastContext ctx) {
        Player caster   = ctx.getPlayer();
        Vector baseDir  = caster.getEyeLocation().getDirection().normalize();
        Location eyeLoc = caster.getEyeLocation().clone();

        // ---- Damage scaling like Meteor ----
        StatsManager.PlayerStats stats =
            StatsManager.getInstance().getPlayerStats(caster.getUniqueId());
        int playerInt = stats.baseIntelligence + stats.bonusIntelligence;
        CustomItem cItem = ItemManager.getInstance()
            .getCustomItemFromItemStack(caster.getInventory().getItemInMainHand());
        int weaponInt = cItem != null ? cItem.getIntel() : 0;

        double rawDamage = ctx.getBaseSpell().getBaseDamage() + playerInt + weaponInt;
        double dmgMultiplier = ctx.getFinalDamage() / ctx.getBaseSpell().getBaseDamage();
        double scaledBaseDamage = rawDamage * dmgMultiplier;

        // Debug: what extraProjectiles and damageMultiplier do we have?
        Object rawExtra = ctx.getExtraParam("extraProjectiles");
        Object rawMult  = ctx.getExtraParam("damageMultiplier");

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

        eyeLoc.getWorld().playSound(eyeLoc, Sound.ENTITY_ENDER_EYE_LAUNCH, 1.0f, 1.0f);

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
            castBeam(caster, eyeLoc.clone(), dir, scaledBaseDamage, dmgMult);
        }
    }


    private void castBeam(Player caster,
                          Location start,
                          Vector dir,
                          double baseDamage,
                          double dmgMult) {
        World world = start.getWorld();
        double damage = baseDamage * dmgMult;

        // 1) Draw the beam in one go:
        //    count = 0 → use the offset vector as the “direction and length” of the effect
        //    offsets = dir * MAX_DISTANCE (so the particle travels that far)
        double step = 2.0;            // finer resolution
        for (double d = 0; d < MAX_DISTANCE; d += step) {
            Location point = start.clone().add(dir.clone().multiply(d));
            world.spawnParticle(
                Particle.WITCH,
                point,
                1,                     // one particle
                0, 0, 0,               // no random spread
                0                      // normal speed
            );
        }


        // 2) Then do your hit‐detection exactly as before (looping along the ray):
        Location loc = start.clone();
        for (double traveled = 0; traveled < MAX_DISTANCE; traveled += STEP) {
            loc.add(dir.clone().multiply(STEP));
            for (var ent : world.getNearbyEntities(loc, 0.5, 0.5, 0.5)) {
                if (ent instanceof LivingEntity target && !target.equals(caster)) {
                    SpellUtils.dealWithChat(caster, target, damage, "Basic Attack");
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
