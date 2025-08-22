package me.nakilex.levelplugin.spells.effect.warrior;

import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.context.SpellCastContextCompat;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.Main;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.Location;

/**
 * Launches the warrior forward and damages entities along the leap path and on landing.
 */
public class HeroicLeapEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double damage = ctx.getFinalDamage();

        Vector launch = player.getLocation().getDirection().normalize().multiply(1.2);
        launch.setY(0.8);
        player.setVelocity(launch);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.2f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                Location loc = player.getLocation();
                // Trail particles while travelling
                loc.getWorld().spawnParticle(Particle.CLOUD, loc, 8, 0.2, 0.2, 0.2, 0.01);

                // Damage entities intersected during the leap path
                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                    if (e instanceof LivingEntity le && !le.equals(player)) {
                        if (le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                            continue;
                        SpellUtils.dealWithChat(player, le, damage, "Heroic Leap");
                    }
                }

                if (player.isOnGround() || ticks++ > 20) {
                    Location land = player.getLocation();
                    land.getWorld().playSound(land, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                    land.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, land, 20, 0.5, 0.5, 0.5, 0.1);

                    for (Entity e : land.getWorld().getNearbyEntities(land, 3.0, 1.5, 3.0)) {
                        if (e instanceof LivingEntity le && !le.equals(player)) {
                            if (le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                                continue;
                            SpellUtils.dealWithChat(player, le, damage, "Heroic Leap");
                        }
                    }

                    cancel();
                    SpellCastContextCompat.markSuccess(ctx, true);
                }
            }
        }.runTaskTimer(Main.getPlugin(), 0L, 1L);
    }
}
