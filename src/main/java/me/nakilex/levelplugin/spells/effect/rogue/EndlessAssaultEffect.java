package me.nakilex.levelplugin.spells.effect.rogue;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class EndlessAssaultEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        LivingEntity target = null;
        for (org.bukkit.entity.Entity e : player.getWorld().getNearbyEntities(player.getEyeLocation(), 10, 10, 10)) {
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
        double damage = ctx.getFinalDamage() * 1.2; // slight buff
        int hits = 6;

        LivingEntity finalTarget = target;
        new BukkitRunnable() {
            int step = 0;
            @Override
            public void run() {
                if (step++ >= hits) {
                    cancel();
                    return;
                }
                finalTarget.setVelocity(finalTarget.getVelocity().add(new Vector(0, 0.2, 0)));
                world.spawnParticle(Particle.CRIT, finalTarget.getLocation(), 15, 0.5, 1, 0.5);
                world.playSound(finalTarget.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
                SpellUtils.dealWithChat(player, finalTarget, damage / hits, "Endless Assault");
            }
        }.runTaskTimer(Main.getInstance(), 0L, 4L);

        player.sendMessage("§aYou unleash a flurry of strikes!");
    }
}
