package me.nakilex.levelplugin.spells.effect.mage;

import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;

/**
 * ObsidianMeteorEffect: spawns obsidian meteors by injecting the projectile material.
 * Delegates all behavior to MeteorEffect.
 */
public class ObsidianMeteorEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        // Ensure obsidian meteor heads
        ctx.putExtraParam("projectileMaterial", "GOLD_BLOCK");
        // Delegate to the shared MeteorEffect
        new MeteorEffect().apply(ctx);
    }
}
