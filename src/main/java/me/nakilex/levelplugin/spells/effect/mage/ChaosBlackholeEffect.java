package me.nakilex.levelplugin.spells.effect.mage;

import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;

public class ChaosBlackholeEffect implements SpellEffect {
    private final BlackholeEffect base = new BlackholeEffect();

    @Override
    public void apply(SpellCastContext ctx) {
        base.apply(ctx);
    }
}
