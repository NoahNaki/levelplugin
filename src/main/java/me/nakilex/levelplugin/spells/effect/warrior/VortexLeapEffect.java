package me.nakilex.levelplugin.spells.effect.warrior;

import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.Location;

/**
 * Heroic Leap variant that pulls enemies inward instead of knocking them away.
 */
public class VortexLeapEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double damage = ctx.getFinalDamage();
        double radius = 5.0;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 1f);
        Vector leap = player.getLocation().getDirection().normalize().multiply(1.2);
        leap.setY(0.5);
        player.setVelocity(leap);

        new BukkitRunnable() {
            boolean landed = false;
            @Override
            public void run() {
                if (!landed && player.isOnGround()) {
                    landed = true;
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 30, 1, 1, 1, 0.2);
                    for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
                        if (e instanceof LivingEntity le && !le.equals(player)) {
                            if (le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                                continue;
                            SpellUtils.dealWithChat(player, le, damage, "Vortex Leap");
                            Vector pull = player.getLocation().toVector().subtract(le.getLocation().toVector()).normalize().multiply(1.2);
                            pull.setY(0.4);
                            le.setVelocity(pull);
                        }
                    }
                    cancel();
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("LevelPlugin"), 4L, 1L);
    }
}
