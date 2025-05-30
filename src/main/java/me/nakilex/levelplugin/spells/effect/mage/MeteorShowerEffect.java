// src/main/java/me/nakilex/levelplugin/spells/effect/mage/MeteorShowerEffect.java
package me.nakilex.levelplugin.spells.effect.mage;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * A “shower” of meteors: schedules multiple MeteorEffect casts
 * spaced out in time and spread horizontally.
 */
public class MeteorShowerEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();

        // Base + extra projectiles
        int extra = 0;
        Object extraProjParam = ctx.getExtraParam("extraProjectiles");
        if (extraProjParam instanceof Number) {
            extra = ((Number) extraProjParam).intValue();
        } else if (extraProjParam instanceof java.util.List<?>) {
            for (Object n : (java.util.List<?>) extraProjParam) {
                if (n instanceof Number) extra += ((Number) n).intValue();
            }
        }
        int total = 1 + extra;

        // Delay between meteors (in ticks); default 40 (~2s)
        long delayBetween = 40L;
        Object intervalParam = ctx.getExtraParam("showerInterval");
        if (intervalParam instanceof Number) {
            delayBetween = ((Number) intervalParam).longValue();
        }

        // Spread radius (horizontal) for shower; default 5 blocks
        double spreadRadius = 5.0;
        Object spreadParam = ctx.getExtraParam("showerSpread");
        if (spreadParam instanceof Number) {
            spreadRadius = ((Number) spreadParam).doubleValue();
        }

        // Precompute base impact point
        Location baseImpact = getImpactLocation(player);

        for (int i = 0; i < total; i++) {
            // Compute random horizontal offset per meteor
            double angle = Math.random() * 2 * Math.PI;
            double dx = Math.cos(angle) * spreadRadius;
            double dz = Math.sin(angle) * spreadRadius;
            Location impactLoc = baseImpact.clone().add(dx, 0, dz);

            new BukkitRunnable() {
                @Override
                public void run() {
                    // Spawn this meteor at impactLoc
                    new MeteorEffect() {
                        protected Location getImpactLocation(Player player) {
                            return impactLoc;
                        }
                    }.apply(ctx);
                }
            }.runTaskLater(Main.getInstance(), i * delayBetween);
        }
    }

    /**
     * Compute default impact location (same as in MeteorEffect).
     */
    protected Location getImpactLocation(Player player) {
        if (player.getTargetBlockExact(20) != null) {
            return player.getTargetBlockExact(20).getLocation().add(0.5, 1, 0.5);
        } else {
            return player.getEyeLocation().add(
                player.getEyeLocation().getDirection().multiply(20)
            );
        }
    }
}
