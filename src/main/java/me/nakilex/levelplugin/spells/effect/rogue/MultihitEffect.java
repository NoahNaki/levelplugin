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

        int totalHits = 8;
        Object eh = ctx.getExtraParam("extraHits");
        if (eh instanceof Number num) totalHits += Math.max(0, num.intValue());
        LivingEntity finalTarget = target;

        Vector dir = player.getLocation().getDirection().setY(0).normalize();
        player.setVelocity(dir.clone().multiply(0.8));

        int finalTotalHits = totalHits;
        new BukkitRunnable() {
            int hit = 0;
            @Override
            public void run() {
                if (!finalTarget.isValid() || finalTarget.isDead()) { cancel(); return; }
                if (hit >= finalTotalHits) {
                    Vector kb = player.getLocation().toVector().subtract(finalTarget.getLocation().toVector()).normalize().multiply(1.4).setY(0.5);
                    finalTarget.setVelocity(kb);
                    world.spawnParticle(Particle.CRIT, finalTarget.getLocation(), 30, 0.3, 0.3, 0.3, 0.05);
                    SpellUtils.dealWithChat(player, finalTarget, damage / finalTotalHits, "Multihit");
                    world.playSound(finalTarget.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 1f);
                    cancel();
                    return;
                }

                double angle = (hit * (2 * Math.PI / finalTotalHits));
                Location pos = finalTarget.getLocation().clone().add(Math.cos(angle), 0, Math.sin(angle));
                pos.setDirection(finalTarget.getLocation().toVector().subtract(pos.toVector()));
                player.teleport(pos);
                world.spawnParticle(Particle.SWEEP_ATTACK, finalTarget.getLocation(), 5, 0.2, 0.2, 0.2, 0);
                world.playSound(finalTarget.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.2f);
                SpellUtils.dealWithChat(player, finalTarget, damage / finalTotalHits, "Multihit");
                hit++;
            }
        }.runTaskTimer(Main.getInstance(), 4L, 4L);

        player.sendMessage("§aYou unleash a flurry of strikes!");
    }
}
