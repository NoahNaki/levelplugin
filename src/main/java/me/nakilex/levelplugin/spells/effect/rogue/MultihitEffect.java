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
        player.setVelocity(dir.clone().multiply(0.6));

        int finalTotalHits = totalHits;
        new BukkitRunnable() {
            int hit = 0;
            @Override
            public void run() {
                if (hit >= finalTotalHits) {
                    Vector kb = dir.clone().multiply(1.2).setY(0.4);
                    finalTarget.setVelocity(kb);
                    world.spawnParticle(Particle.CRIT, finalTarget.getLocation(), 20, 0.3, 0.3, 0.3, 0.05);
                    SpellUtils.dealWithChat(player, finalTarget, damage / finalTotalHits, "Multihit");
                    world.playSound(finalTarget.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 1f);
                    cancel();
                    return;
                }
                world.spawnParticle(Particle.SWEEP_ATTACK, finalTarget.getLocation(), 5, 0.2, 0.2, 0.2, 0);
                world.playSound(finalTarget.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
                SpellUtils.dealWithChat(player, finalTarget, damage / finalTotalHits, "Multihit");
                Vector pull = dir.clone().multiply(-0.15);
                finalTarget.setVelocity(finalTarget.getVelocity().add(pull));
                hit++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 2L);

        player.sendMessage("§aYou unleash a flurry of strikes!");
    }
}
