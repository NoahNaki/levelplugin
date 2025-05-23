package me.nakilex.levelplugin.spells.effect.mage;

import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;

/**
 * FrostCometEffect: spawns packed ice meteors and freezes ground on impact.
 * Delegates core meteor logic to MeteorEffect.
 */
public class FrostCometEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        // Ensure ice projectiles
        ctx.putExtraParam("projectileMaterial", "PACKED_ICE");
        // Default freeze duration (sec)
        Object fd = ctx.getExtraParam("freezeDuration");
        if (!(fd instanceof Number)) {
            ctx.putExtraParam("freezeDuration", 2.0);
        }
        // Default stun duration (sec)
        Object sd = ctx.getExtraParam("stunDuration");
        if (!(sd instanceof Number)) {
            ctx.putExtraParam("stunDuration", 2.0);
        }
        // Delegate to MeteorEffect for all remaining behavior
        new MeteorEffect().apply(ctx);
    }
}
