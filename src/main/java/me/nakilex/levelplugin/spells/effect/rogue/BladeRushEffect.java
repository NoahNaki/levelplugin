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

/**
 * Dash to a nearby target and unleash several quick slashes.
 */
public class BladeRushEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        double range = 8.0;
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

        World world = player.getWorld();
        double damage = ctx.getFinalDamage();
        Location start = player.getLocation();
        Location loc = target.getLocation().clone().add(0, 0.1, 0);

        world.playSound(start, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
        player.teleport(loc);

        LivingEntity finalTarget = target;
        new BukkitRunnable() {
            int hits = 0;
            @Override
            public void run() {
                if (hits >= 3) {
                    player.teleport(start);
                    cancel();
                    return;
                }
                world.spawnParticle(Particle.SWEEP_ATTACK, finalTarget.getLocation(), 10, 0.3, 0.3, 0.3, 0.01);
                world.playSound(finalTarget.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
                SpellUtils.dealWithChat(player, finalTarget, damage / 3.0, "Blade Rush");
                hits++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 4L);
    }
}
