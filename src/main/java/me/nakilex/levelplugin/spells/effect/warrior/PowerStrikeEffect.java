package me.nakilex.levelplugin.spells.effect.warrior;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import me.nakilex.levelplugin.spells.utils.animation.SpellAnimation;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import org.bukkit.Location;

/**
 * Short ranged ground strike inspired by EpicSpells' PowerStrike.
 * Spawns block debris instead of destroying terrain.
 */
public class PowerStrikeEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double damage = ctx.getFinalDamage();
        Vector dir = player.getLocation().getDirection().normalize();
        Location start = player.getLocation().add(0, 1, 0);
        double range = 6.0; // shorter than original

        new SpellAnimation(1, 12) {
            Location pos = start.clone();
            @Override
            protected void onTick(int tick) {
                pos.add(dir.clone().multiply(range / 12));
                player.getWorld().spawnParticle(Particle.SONIC_BOOM, pos, 0);
                player.getWorld().spawnParticle(Particle.CRIT, pos, 5, 0.2,0.2,0.2);

                // spawn falling block debris like Shockwave
                if (Math.random() < 0.3) {
                    Block ground = pos.getBlock().getRelative(0, -1, 0);
                    if (ground.getType() != Material.AIR) {
                        Location bLoc = ground.getLocation().add(0.5, 1, 0.5);
                        FallingBlock fb = player.getWorld().spawnFallingBlock(bLoc, ground.getBlockData());
                        fb.setDropItem(false);
                        fb.setVelocity(new Vector((Math.random()-0.5)*0.4, 0.4, (Math.random()-0.5)*0.4));
                        fb.setMetadata("Shockwave", new FixedMetadataValue(Main.getInstance(), true));
                        Bukkit.getScheduler().runTaskLater(Main.getInstance(), fb::remove, 40L);
                    }
                }

                for (Entity e : pos.getWorld().getNearbyEntities(pos, 1.5,1.5,1.5)) {
                    if (e instanceof LivingEntity le && !le.equals(player)) {
                        if (le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                            continue;
                        SpellUtils.dealWithChat(player, le, damage, "Power Strike");
                        Vector kb = dir.clone().multiply(0.6); kb.setY(0.3);
                        le.setVelocity(kb);
                    }
                }
            }
        };
    }
}
