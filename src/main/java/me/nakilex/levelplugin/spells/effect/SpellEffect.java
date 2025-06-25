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
    /**
     * Apply the effect using the provided context.
     * Effects should call {@link SpellCastContext#markSuccess(boolean)} to
     * indicate whether any action actually occurred.
     */
    void apply(SpellCastContext ctx);
}
