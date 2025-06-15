package me.nakilex.levelplugin.spells.effect.rogue;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class MultihitEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        LivingEntity target = null;
        for (Entity e : player.getWorld().getNearbyEntities(player.getEyeLocation(), 10, 10, 10)) {
            if (!(e instanceof LivingEntity le) || le.equals(player)) continue;
            if (le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                continue;
            target = le;
            break;
        }
        if (target == null) {
            player.sendMessage("§cNo valid target in range!");
            return;
        }

        World world = target.getWorld();
        double damage = ctx.getFinalDamage();
        int hits = 8;

        LivingEntity finalTarget = target;
        new BukkitRunnable() {
            int step = 0;
            @Override
            public void run() {
                if (step >= hits) {
                    cancel();
                    return;
                }
                double angle = (2 * Math.PI / hits) * step;
                double radius = 1.5;
                double px = finalTarget.getLocation().getX() + Math.cos(angle) * radius;
                double pz = finalTarget.getLocation().getZ() + Math.sin(angle) * radius;
                Location strikeLoc = new Location(world, px, finalTarget.getLocation().getY(), pz);
                strikeLoc.setYaw((float)Math.toDegrees(angle + Math.PI));
                player.teleport(strikeLoc);

                world.spawnParticle(Particle.SWEEP_ATTACK, finalTarget.getLocation(), 5, 0.3, 0.3, 0.3);
                world.spawnParticle(Particle.CRIT, strikeLoc, 10, 0.2, 0.2, 0.2);
                finalTarget.setVelocity(finalTarget.getVelocity().add(new org.bukkit.util.Vector(0, 0.05, 0)));
                world.playSound(finalTarget.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
                SpellUtils.dealWithChat(player, finalTarget, damage / hits, "Multihit");
                step++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 2L);

        player.sendMessage("§aYou strike your foe multiple times!");
    }
}
