package me.nakilex.levelplugin.spells.effect.warrior;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import me.nakilex.levelplugin.spells.utils.animation.SpellAnimation;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.bukkit.Location;

import java.util.List;

/**
 * Ground slam style effect that expands outward in rings.
 * Radius can be increased via the "aoeRadius" extra param.
 */
public class ShockwaveEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double damage = ctx.getFinalDamage();

        // Base radius
        double maxRadius = 10.0;
        Object radiusParam = ctx.getExtraParam("aoeRadius");
        if (radiusParam instanceof Number n) {
            maxRadius += n.doubleValue();
        } else if (radiusParam instanceof List<?> list) {
            for (Object o : list) if (o instanceof Number n) maxRadius += n.doubleValue();
        }

        int duration = 20;
        int steps = 10;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1f);
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 10, 0.5, 0.5, 0.5);

        Plugin plugin = Bukkit.getPluginManager().getPlugin("LevelPlugin");

        double finalMaxRadius = maxRadius;
        new SpellAnimation(duration / steps, duration) {
            double current = 0;
            @Override
            protected void onTick(int tick) {
                if (current >= finalMaxRadius) { cancel(); return; }
                current += finalMaxRadius / steps;

                for (double angle = 0; angle < 360; angle += 10) {
                    double rad = Math.toRadians(angle);
                    double x = Math.cos(rad) * current;
                    double z = Math.sin(rad) * current;
                    Location loc = player.getLocation().clone().add(x, 0, z);

                    loc.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, loc, 10, 0.2, 0.2, 0.2, 0.1, Material.DIRT.createBlockData());
                    loc.getWorld().spawnParticle(Particle.CRIT, loc, 5, 0.2, 0.2, 0.2);

                    if (Math.random() < 0.2) { // spawn fewer blocks for performance
                        Block ground = loc.getWorld().getHighestBlockAt(loc);
                        if (ground.getType() != Material.AIR) {
                            Location bLoc = ground.getLocation().add(0.5, 1.0, 0.5);
                            FallingBlock fb = loc.getWorld().spawnFallingBlock(bLoc, ground.getBlockData());
                            fb.setDropItem(false);
                            // Reduce upward velocity so blocks stay closer to the ground
                            fb.setVelocity(new Vector(Math.cos(rad) * 0.2, 0.15, Math.sin(rad) * 0.2));
                            fb.setMetadata("Shockwave", new FixedMetadataValue(plugin, true));
                            // Give the block plenty of time to fall before it despawns
                            Bukkit.getScheduler().runTaskLater(plugin, fb::remove, 80L);
                        }
                    }

                    for (Entity e : loc.getWorld().getNearbyEntities(loc, 1, 1, 1)) {
                        if (e instanceof LivingEntity le && !le.equals(player)) {
                            if (le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                                continue;
                            SpellUtils.dealWithChat(player, le, damage, "Shockwave");
                            Vector kb = le.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.5);
                            kb.setY(0.3);
                            le.setVelocity(kb);
                        }
                    }

                    if (!loc.getBlock().isPassable()) break;
                }

                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 0.5f, 0.8f);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 0.7f, 1f);
            }
        };
    }
}
