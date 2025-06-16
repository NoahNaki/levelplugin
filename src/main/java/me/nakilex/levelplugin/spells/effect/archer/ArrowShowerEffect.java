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

        final int MAX_RANGE = 20;
        var targetBlock = player.getTargetBlockExact(MAX_RANGE);
        var targetLocation = targetBlock != null
                ? targetBlock.getLocation()
                : player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(MAX_RANGE));
        targetLocation.add(0, 20, 0);

        ArrowStorm spell = new ArrowStorm();
        spell.init(new SpellManager(), targetLocation, player, 0, 0, "Arrow Storm");
        spell.setAlive(true);
        new SpellRunner(spell).start();
    }
}
