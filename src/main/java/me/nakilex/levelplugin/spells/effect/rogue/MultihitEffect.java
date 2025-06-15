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

        Location start = player.getLocation();
        Location tLoc = target.getLocation();

        Location[] spots = new Location[] {
                tLoc.clone().add(1.5, 0, 0),
                tLoc.clone().add(-1.5, 0, 0),
                tLoc.clone().add(0, 0, 1.5),
                tLoc.clone().add(0, 0, -1.5),
                tLoc.clone().add(0, 1, 0)
        };

        new BukkitRunnable() {
            int idx = 0;
            @Override
            public void run() {
                if (idx >= spots.length) {
                    player.teleport(start);
                    world.spawnParticle(Particle.CRIT, tLoc, 25, 0.5, 0.5, 0.5, 0.1);
                    world.playSound(tLoc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 1f);
                    SpellUtils.dealWithChat(player, target, damage, "Multihit");
                    cancel();
                    return;
                }
                Location dest = spots[idx++];
                player.teleport(dest);
                world.spawnParticle(Particle.SWEEP_ATTACK, dest, 10, 0.3, 0.3, 0.3, 0.01);
                world.playSound(dest, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
                SpellUtils.dealWithChat(player, target, damage / spots.length, "Multihit");
            }
        }.runTaskTimer(Main.getInstance(), 0L, 6L);

        player.sendMessage("§aYou blink around your foe in a deadly combo!");
    }
}
