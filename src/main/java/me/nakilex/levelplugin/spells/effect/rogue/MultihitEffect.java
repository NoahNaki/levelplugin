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
import org.bukkit.util.Vector;

public class MultihitEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double range = 10.0;
        Object r = ctx.getExtraParam("targetRange");
        if (r instanceof Number num) range += num.doubleValue();

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
        boolean backstab = Boolean.TRUE.equals(ctx.getExtraParam("backstab"));
        World world = target.getWorld();
        double damage = ctx.getFinalDamage();

        if (backstab) {
            var behind = target.getLocation().clone().add(target.getLocation().getDirection().normalize().multiply(-1));
            player.teleport(behind);
            SpellUtils.dealWithChat(player, target, damage * 1.5, "Backstab");
            world.spawnParticle(Particle.CRIT, target.getLocation(), 20, 0.5, 1, 0.5);
            world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
            return;
        }

        int extraHits = 0;
        Object eh = ctx.getExtraParam("extraHits");
        if (eh instanceof Number num) extraHits = num.intValue();

        int totalHits = 8 + extraHits;
        Vector[] dirs = new Vector[totalHits + 2];
        int idx = 0;
        for (int i = 0; i < totalHits; i++) {
            double angle = Math.toRadians(i * (360.0 / totalHits));
            dirs[idx++] = new Vector(Math.cos(angle), 0.2, Math.sin(angle));
        }
        dirs[idx++] = new Vector(0, -1, 0);
        dirs[idx] = new Vector(0, 1, 0);

        double knockback = 0.0;
        Object kb = ctx.getExtraParam("knockback");
        if (kb instanceof Number num) knockback = num.doubleValue();

        LivingEntity finalTarget = target;
        new BukkitRunnable() {
            int step = 0;
            @Override
            public void run() {
                if (step >= dirs.length) {
                    SpellUtils.dealWithChat(player, finalTarget, damage, "Multihit");
                    if (knockback > 0) {
                        finalTarget.setVelocity(player.getLocation().getDirection().normalize().multiply(knockback));
                    }
                    cancel();
                    return;
                }
                finalTarget.setVelocity(dirs[step].clone().multiply(1.2));
                world.spawnParticle(Particle.CRIT, finalTarget.getLocation(), 15, 0.5, 1, 0.5);
                world.playSound(finalTarget.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
                step++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 4L);

        player.sendMessage("§aYou strike your foe multiple times!");
    }
}
