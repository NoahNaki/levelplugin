package me.nakilex.levelplugin.spells.effect;

import me.nakilex.levelplugin.spells.context.SpellCastContext;

public interface SpellEffect {
    /**
     * Perform the effect using all data in ctx (damage, caster, extraParams, etc.).
     */
    void apply(SpellCastContext ctx);
}
