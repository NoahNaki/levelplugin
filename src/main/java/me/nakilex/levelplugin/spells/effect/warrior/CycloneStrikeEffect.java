package me.nakilex.levelplugin.spells.effect.warrior;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import me.nakilex.levelplugin.spells.utils.animation.SpellAnimation;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Spin in a circle, slashing nearby enemies with swirling particles.
 */
public class CycloneStrikeEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double damage = ctx.getFinalDamage();
        double radius = 3.0;

        new SpellAnimation(2, 20) {
            @Override
            protected void onTick(int tick) {
                double angleOffset = tick * Math.PI / 5;
                Location base = player.getLocation();
                for (double a = 0; a < Math.PI * 2; a += Math.PI / 4) {
                    double rad = a + angleOffset;
                    Vector offset = new Vector(Math.cos(rad) * radius, 1.0, Math.sin(rad) * radius);
                    Location loc = base.clone().add(offset);
                    player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 0);
                    for (LivingEntity le : loc.getNearbyLivingEntities(0.6)) {
                        if (le.equals(player)) continue;
                        if (le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                            continue;
                        SpellUtils.dealWithChat(player, le, damage, "Cyclone Strike");
                    }
                }
            }
        };
    }
}
