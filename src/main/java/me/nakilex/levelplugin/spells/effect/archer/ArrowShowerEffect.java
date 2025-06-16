package me.nakilex.levelplugin.spells.effect.archer;

import me.nakilex.levelplugin.epicspells.SpellManager;
import me.nakilex.levelplugin.epicspells.SpellRunner;
import me.nakilex.levelplugin.epicspells.spells.ArrowStorm;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.Main;
import org.bukkit.entity.Player;

/**
 * Wraps the legacy ArrowStorm spell from the epicspells package so it can
 * be triggered through the normal spell system (via a rune transform).
 */
public class ArrowShowerEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        Main.getInstance().getLogger().info("[DBG] ArrowShowerEffect triggered for " + player.getName());
        ArrowStorm spell = new ArrowStorm();
        spell.init(new SpellManager(), player, 0, 0, "Arrow Storm");
        spell.setAlive(true);
        new SpellRunner(spell).start();
    }
}
