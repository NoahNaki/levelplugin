package me.nakilex.levelplugin.spells.effect;

import me.nakilex.levelplugin.spells.context.SpellCastContext;

public interface SpellEffect {
    /**
     * Perform the effect using all data in ctx (damage, caster, extraParams, etc.).
     */
    /**
     * Apply the effect. Return true if the underlying MythicMobs skill or
     * animation actually triggered. Returning false indicates the effect did
     * nothing, e.g. because a Mythic skill was still on cooldown.
     */
    boolean apply(SpellCastContext ctx);
}
