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
                double angle = step * Math.PI / 4;
                for (int i = 0; i < 8; i++) {
                    double a = angle + i * Math.PI / 4;
                    double x = Math.cos(a) * 0.5;
                    double z = Math.sin(a) * 0.5;
                    world.spawnParticle(Particle.CRIT, finalTarget.getLocation().add(x, 0.1 * i, z), 0);
                }
                finalTarget.setVelocity(finalTarget.getVelocity().add(new org.bukkit.util.Vector(0, 0.1, 0)));
                world.playSound(finalTarget.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
                SpellUtils.dealWithChat(player, finalTarget, damage / hits, "Multihit");
                step++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 3L);

        player.sendMessage("§aYou strike your foe multiple times!");
    }
}
