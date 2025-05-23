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
        new MeteorEffect().apply(ctx);
    }
}
