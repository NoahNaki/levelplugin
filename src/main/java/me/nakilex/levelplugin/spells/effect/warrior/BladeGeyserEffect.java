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
 * Blades erupt from the ground dealing damage in a small area.
 */
public class BladeGeyserEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double damage = ctx.getFinalDamage();
        Location start = player.getLocation().add(player.getLocation().getDirection().normalize().multiply(2));

        new SpellAnimation(1, 10) {
            Location loc = start.clone();
            @Override
            protected void onTick(int tick) {
                loc.add(0, 0.5, 0);
                player.getWorld().spawnParticle(Particle.CRIT, loc, 8, 0.2, 0.2, 0.2, 0.1);
                for (LivingEntity le : loc.getNearbyLivingEntities(1.0)) {
                    if (le.equals(player)) continue;
                    if (le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                        continue;
                    SpellUtils.dealWithChat(player, le, damage, "Blade Geyser");
                    Vector kb = le.getLocation().toVector().subtract(start.toVector()).normalize().multiply(0.4);
                    kb.setY(0.3);
                    le.setVelocity(le.getVelocity().add(kb));
                }
            }
        };
    }
}
