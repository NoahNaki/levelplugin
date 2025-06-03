package me.nakilex.levelplugin.spells.effect.warrior;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.Location;

public class GroundSlamEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double damage = ctx.getFinalDamage();
        double maxRadius = 10.0;
        int duration = 20;
        int steps = 10;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 10, 0.5, 0.5, 0.5);

        new BukkitRunnable() {
            double current = 0;
            @Override
            public void run() {
                if (current >= maxRadius) { cancel(); return; }
                current += maxRadius / steps;

                for (double angle = 0; angle < 360; angle += 10) {
                    double rad = Math.toRadians(angle);
                    double x = Math.cos(rad) * current;
                    double z = Math.sin(rad) * current;
                    Location loc = player.getLocation().clone().add(x, 0, z);

                    loc.getWorld().spawnParticle(Particle.BLOCK_CRACK, loc, 10, 0.2, 0.2, 0.2, 0.1, Material.DIRT.createBlockData());
                    loc.getWorld().spawnParticle(Particle.CRIT, loc, 5, 0.2, 0.2, 0.2);

                    for (Entity e : loc.getWorld().getNearbyEntities(loc, 1, 1, 1)) {
                        if (e instanceof LivingEntity le && !le.equals(player)) {
                            if (le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                                continue;
                            SpellUtils.dealWithChat(player, le, damage, "Ground Slam");
                            Vector kb = le.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.5);
                            kb.setY(0.3);
                            le.setVelocity(kb);
                        }
                    }

                    if (!loc.getBlock().isPassable()) break;
                }

                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 0.5f, 0.8f);
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 0L, duration / steps);
    }
}
