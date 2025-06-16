package me.nakilex.levelplugin.spells.effect.archer;

import me.nakilex.levelplugin.epicspells.spells.ArrowStorm;
import me.nakilex.levelplugin.epicspells.SpellManager;
import me.nakilex.levelplugin.epicspells.SpellRunner;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import org.bukkit.entity.Player;

/**
 * Launches the imported EpicSpells ArrowStorm as a spell effect.
 */
public class EpicArrowStormEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        ArrowStorm spell = new ArrowStorm();
        spell.init(new SpellManager(), player, 0, 0, "Arrow Storm");
        spell.setAlive(true);
        new SpellRunner(spell).start();
    }
}
