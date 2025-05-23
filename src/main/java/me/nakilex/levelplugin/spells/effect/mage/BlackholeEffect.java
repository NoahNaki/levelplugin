package me.nakilex.levelplugin.spells.effect.mage;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Optional;
import java.util.UUID;

/**
 * Exact reincarnation of original castBlackhole logic as a SpellEffect.
 */
public class BlackholeEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        Main plugin = Main.getInstance();
        UUID pid = player.getUniqueId();

        // Cancel any existing blackhole tasks
        plugin.getServer().getScheduler().cancelTasks(plugin);

        // Compute damage
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(pid);
        double damage = 10.0 + 0.5 * (ps.baseIntelligence + ps.bonusIntelligence);
        damage *= ctx.getFinalDamage()/ctx.getBaseSpell().getBaseDamage();

        // Determine center
        Location center = Optional.ofNullable(player.getTargetBlockExact(20))
            .map(b -> b.getLocation().add(0.5, 1, 0.5))
            .orElseGet(() -> player.getEyeLocation().add(player.getLocation().getDirection().multiply(10)));

        double pullRadius = 5.0;
        double damageRadius = 5.0;

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 1f);

        // Visual effect task
        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (++ticks > 50) { cancel(); return; }
                // Portal and smoke particles rotating
                for (double angle = 0; angle < 360; angle += 10) {
                    double rad = Math.toRadians(angle);
                    double x = pullRadius * Math.cos(rad);
                    double z = pullRadius * Math.sin(rad);
                    Location loc = center.clone().add(x, 0, z)
                        .add(0, Math.sin(ticks/10.0)*0.5, 0);
                    center.getWorld().spawnParticle(Particle.PORTAL, loc, 1);
                    center.getWorld().spawnParticle(Particle.SMOKE, loc, 1);
                }
                center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 5);
            }
        }.runTaskTimer(plugin, 0L, 2L);

        // Pull & damage task
        double finalDamage = damage;
        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks++ >= 50) { cancel(); return; }
                for (Entity e : center.getWorld().getNearbyEntities(center, pullRadius, pullRadius, pullRadius)) {
                    if (!(e instanceof LivingEntity le) || le == player) continue;
                    if (le instanceof Player && !DuelManager.getInstance().areInDuel(pid, ((Player) le).getUniqueId())) continue;
                    // Pull
                    le.setVelocity(center.toVector().subtract(le.getLocation().toVector()).normalize().multiply(0.2));
                    // Damage check
                    if (le.getLocation().distance(center) <= damageRadius) {
                        SpellUtils.dealWithChat(player, le, finalDamage, "Blackhole");
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}