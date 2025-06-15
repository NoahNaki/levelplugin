package me.nakilex.levelplugin.spells.effect.rogue;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Dash forward in a burst of smoke.
 */
public class ShadowDashEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        World world = player.getWorld();
        double distance = 5.0;
        Object bonus = ctx.getExtraParam("distanceBonus");
        if (bonus instanceof Number n) distance += n.doubleValue();

        Vector dir = player.getLocation().getDirection().setY(0).normalize();
        Location start = player.getLocation();
        Location end = start.clone();

        for (double d = 0; d <= distance; d += 0.3) {
            Location step = start.clone().add(dir.clone().multiply(d));
            Block b = step.getBlock();
            if (b.getType() != Material.AIR && !b.isPassable()) break;
            end = step;
            world.spawnParticle(Particle.SMOKE_LARGE, step, 2, 0.1,0.1,0.1,0);
        }

        world.playSound(start, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        player.teleport(end);
    }
}
