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

public class EndlessAssaultEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();

        double range = 10.0;
        Object r = ctx.getExtraParam("targetRange");
        if (r instanceof Number num) range += num.doubleValue();

        int maxTargets = 5;
        Object m = ctx.getExtraParam("maxTargets");
        if (m instanceof Number num) maxTargets = Math.max(1, num.intValue());

        java.util.List<LivingEntity> targets = new java.util.ArrayList<>();
        for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), range, range, range)) {
            if (!(e instanceof LivingEntity le) || le.equals(player)) continue;
            if (le instanceof Player p && !DuelManager.getInstance().areInDuel(player.getUniqueId(), p.getUniqueId()))
                continue;
            targets.add(le);
        }

        if (targets.isEmpty()) {
            player.sendMessage("§cNo targets nearby!");
            return;
        }

        targets.sort(java.util.Comparator.comparingDouble(t -> t.getLocation().distanceSquared(player.getLocation())));
        if (targets.size() > maxTargets) targets = targets.subList(0, maxTargets);

        World world = player.getWorld();
        Location start = player.getLocation();
        double damage = ctx.getFinalDamage();

        new BukkitRunnable() {
            int idx = 0;
            @Override
            public void run() {
                if (idx >= targets.size()) {
                    player.teleport(start);
                    cancel();
                    return;
                }
                LivingEntity t = targets.get(idx++);
                player.teleport(t.getLocation().clone().add(0, 0.1, 0));
                SpellUtils.dealWithChat(player, t, damage, "Endless Assault");
                world.spawnParticle(Particle.SWEEP_ATTACK, t.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);
                world.playSound(t.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
            }
        }.runTaskTimer(Main.getInstance(), 0L, 10L);

        player.sendMessage("§aYou dash between foes in a blur!");
    }
}
