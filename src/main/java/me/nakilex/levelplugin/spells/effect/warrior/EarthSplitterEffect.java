package me.nakilex.levelplugin.spells.effect.warrior;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import me.nakilex.levelplugin.spells.utils.animation.SpellAnimation;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.Location;

/**
 * Sends a fissure forward that damages and knocks enemies upward.
 */
public class EarthSplitterEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double damage = ctx.getFinalDamage();
        Vector dir = player.getLocation().getDirection().normalize();
        World world = player.getWorld();
        Location start = player.getLocation().add(0, 1, 0);
        double range = 8.0;

        new SpellAnimation(2, 16) {
            Location pos = start.clone();
            @Override
            protected void onTick(int tick) {
                pos.add(dir.clone().multiply(0.5));
                world.spawnParticle(Particle.BLOCK_CRACK, pos, 8, 0.2, 0.1, 0.2, Material.DIRT.createBlockData());
                for (LivingEntity le : pos.getNearbyLivingEntities(1.0)) {
                    if (le.equals(player)) continue;
                    if (le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                        continue;
                    SpellUtils.dealWithChat(player, le, damage, "Earth Splitter");
                    Vector vel = dir.clone().multiply(0.3); vel.setY(0.4);
                    le.setVelocity(le.getVelocity().add(vel));
                }
                if (pos.distanceSquared(start) > range * range) cancel();
            }
        };
    }
}
