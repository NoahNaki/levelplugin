package me.nakilex.levelplugin.spells.effect.rogue;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class MultihitEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();

        double range = 8.0;
        Object rp = ctx.getExtraParam("targetRange");
        if (rp instanceof Number num) range += num.doubleValue();

        LivingEntity target = null;
        for (Entity e : player.getWorld().getNearbyEntities(player.getEyeLocation(), range, range, range)) {
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

        World world = player.getWorld();
        double damage = ctx.getFinalDamage();

        int totalHits = 5;
        Object eh = ctx.getExtraParam("extraHits");
        if (eh instanceof Number num) totalHits += Math.max(0, num.intValue());
        LivingEntity finalTarget = target;

        new BukkitRunnable() {
            int step = 0;
            @Override
            public void run() {
                if (step >= totalHits) {
                    // final crescent slash
                    Vector forward = player.getLocation().getDirection().setY(0).normalize();
                    Vector right = new Vector(-forward.getZ(), 0, forward.getX());
                    Location origin = player.getLocation().clone().add(forward.multiply(1));
                    for (double t = -Math.PI/2; t <= Math.PI/2; t += Math.PI/16) {
                        Vector offset = forward.clone().multiply(Math.cos(t)*1.5).add(right.clone().multiply(Math.sin(t)*1.5)).add(new Vector(0, Math.sin(t)*0.4, 0));
                        world.spawnParticle(Particle.CRIT, origin.clone().add(offset), 0, 0, 0, 0);
                    }
                    world.playSound(origin, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 1f);
                    SpellUtils.dealWithChat(player, finalTarget, damage, "Multihit");
                    cancel();
                    return;
                }
                double angle = step * (2*Math.PI/totalHits);
                Location loc = finalTarget.getLocation().clone().add(Math.cos(angle)*1.2, 0.2, Math.sin(angle)*1.2);
                world.spawnParticle(Particle.SWEEP_ATTACK, loc, 5, 0.1, 0.1, 0.1, 0.01);
                world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
                SpellUtils.dealWithChat(player, finalTarget, damage/totalHits, "Multihit");
                step++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 4L);

        player.sendMessage("§aYou strike your foe from every side!");
    }
}
