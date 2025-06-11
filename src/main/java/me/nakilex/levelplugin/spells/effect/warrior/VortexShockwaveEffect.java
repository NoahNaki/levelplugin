package me.nakilex.levelplugin.spells.effect.warrior;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.Location;

import java.util.List;

/**
 * Shockwave variant that pulls enemies toward the caster instead of knocking them away.
 */
public class VortexShockwaveEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double damage = ctx.getFinalDamage();

        double maxRadius = 10.0;
        Object radiusParam = ctx.getExtraParam("aoeRadius");
        if (radiusParam instanceof Number n) {
            maxRadius += n.doubleValue();
        } else if (radiusParam instanceof List<?> list) {
            for (Object o : list) if (o instanceof Number n) maxRadius += n.doubleValue();
        }

        int duration = 20;
        int steps = 10;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.8f);
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 10, 0.5, 0.5, 0.5);

        Plugin plugin = Bukkit.getPluginManager().getPlugin("LevelPlugin");

        double finalMaxRadius = maxRadius;
        new BukkitRunnable() {
            double current = 0;
            @Override
            public void run() {
                if (current >= finalMaxRadius) { cancel(); return; }
                current += finalMaxRadius / steps;

                for (double angle = 0; angle < 360; angle += 10) {
                    double rad = Math.toRadians(angle);
                    double x = Math.cos(rad) * current;
                    double z = Math.sin(rad) * current;
                    Location loc = player.getLocation().clone().add(x, 0, z);

                    loc.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, loc, 10, 0.2, 0.2, 0.2, 0.1, Material.DIRT.createBlockData());
                    loc.getWorld().spawnParticle(Particle.CRIT, loc, 5, 0.2, 0.2, 0.2);

                    if (Math.random() < 0.2) {
                        Block ground = loc.getWorld().getHighestBlockAt(loc);
                        if (ground.getType() != Material.AIR) {
                            Location bLoc = ground.getLocation().add(0.5, 1.0, 0.5);
                            FallingBlock fb = loc.getWorld().spawnFallingBlock(bLoc, ground.getBlockData());
                            fb.setDropItem(false);
                            fb.setVelocity(new Vector(Math.cos(rad) * 0.2, 0.15, Math.sin(rad) * 0.2));
                            fb.setMetadata("Shockwave", new FixedMetadataValue(plugin, true));
                            Bukkit.getScheduler().runTaskLater(plugin, fb::remove, 40L);
                        }
                    }

                    for (Entity e : loc.getWorld().getNearbyEntities(loc, 1, 1, 1)) {
                        if (e instanceof LivingEntity le && !le.equals(player)) {
                            if (le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                                continue;
                            SpellUtils.dealWithChat(player, le, damage, "Vortex Shockwave");
                            Vector pull = player.getLocation().toVector().subtract(le.getLocation().toVector()).normalize().multiply(0.5);
                            pull.setY(0.3);
                            le.setVelocity(pull);
                        }
                    }

                    if (!loc.getBlock().isPassable()) break;
                }

                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 0.5f, 0.8f);
            }
        }.runTaskTimer(plugin, 0L, duration / steps);
    }
}
