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
import me.nakilex.levelplugin.spells.utils.magic.MagicEffects;
import me.nakilex.levelplugin.spells.utils.animation.SpellAnimation;

/**
 * Rapidly strike a target several times, lifting them into the air.
 */
public class MultihitEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();

        double range = 8.0;
        Object rp = ctx.getExtraParam("targetRange");
        if (rp instanceof Number num) range += num.doubleValue();

        LivingEntity target = null;
        Entity looked = player.getTargetEntity((int) range);
        if (looked instanceof LivingEntity le && !le.equals(player)) {
            if (!(le instanceof Player p) || DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId())) {
                target = le;
            }
        }
        if (target == null) {
            for (Entity e : player.getWorld().getNearbyEntities(player.getEyeLocation(), range, range, range)) {
                if (!(e instanceof LivingEntity le) || le.equals(player)) continue;
                if (le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                    continue;
                target = le;
                break;
            }
        }

        if (target == null) {
            player.sendMessage("§cNo valid target in range!");
            return;
        }

        World world = player.getWorld();
        double damage = ctx.getFinalDamage();

        int hits = 5;
        Object eh = ctx.getExtraParam("extraHits");
        if (eh instanceof Number num) hits += Math.max(0, num.intValue());
        LivingEntity finalTarget = target;

        // decorative helix and spiralling circle around the target
        MagicEffects.helix(finalTarget.getLocation().add(0, 0.5, 0), Particle.CRIT, 1.0, 1.8, 3, hits * 2);
        new SpellAnimation(1, hits * 2) {
            @Override
            protected void onTick(int tick) {
                double progress = (double) tick / (hits * 2);
                double rad = 1.0 + 0.3 * Math.sin(progress * Math.PI);
                MagicEffects.circle(world, finalTarget.getLocation().add(0, 0.5 + progress * 1.2, 0), Particle.CRIT, rad, 12);
            }
        };

        new BukkitRunnable() {
            int done = 0;
            @Override
            public void run() {
                if (done >= hits || !finalTarget.isValid()) { cancel(); return; }
                Vector v = finalTarget.getVelocity();
                v.setX(0);
                v.setZ(0);
                // gently lift the target instead of launching them high
                v.setY(v.getY() + 0.15);
                finalTarget.setVelocity(v);
                SpellUtils.dealWithChat(player, finalTarget, damage / hits, "Multihit");
                world.spawnParticle(Particle.SWEEP_ATTACK, finalTarget.getLocation(), 5, 0.2, 0.2, 0.2, 0.01);
                world.playSound(finalTarget.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
                done++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 2L);

        player.sendMessage("§aYou unleash a flurry of strikes!");
    }
}
