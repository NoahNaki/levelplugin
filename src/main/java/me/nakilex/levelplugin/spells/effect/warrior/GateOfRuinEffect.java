package me.nakilex.levelplugin.spells.effect.warrior;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Judgement rune effect that rains several swords from above.
 */
public class GateOfRuinEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double damage = ctx.getFinalDamage();
        World world = player.getWorld();

        Location base = player.getTargetBlockExact(20) != null ?
                player.getTargetBlockExact(20).getLocation().add(0.5, 0, 0.5) :
                player.getLocation().add(player.getLocation().getDirection().multiply(8));

        int swords = 6;
        for (int i = 0; i < swords; i++) {
            Location spawn = base.clone().add(Math.random()*6-3, 15 + Math.random()*5, Math.random()*6-3);
            new BukkitRunnable() {
                Location loc = spawn.clone();
                @Override
                public void run() {
                    loc.add(0, -1, 0);
                    world.spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0,0,0,0);
                    if (loc.getY() <= base.getY()+1) {
                        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                        world.spawnParticle(Particle.EXPLOSION, loc, 1);
                        for (Entity e : world.getNearbyEntities(loc, 2,2,2)) {
                            if (e instanceof LivingEntity le && !le.equals(player)) {
                                if (le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                                    continue;
                                SpellUtils.dealWithChat(player, le, damage, "Gate of Ruin");
                            }
                        }
                        cancel();
                    }
                }
            }.runTaskTimer(Main.getInstance(), i*4L, 1L);
        }
    }
}
