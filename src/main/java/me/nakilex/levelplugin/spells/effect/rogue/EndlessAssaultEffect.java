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

public class EndlessAssaultEffect implements SpellEffect {
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
        LivingEntity finalTarget = target;

        new BukkitRunnable() {
            int hits = 0;
            @Override
            public void run() {
                if (hits >= 5) { cancel(); return; }
                finalTarget.setVelocity(finalTarget.getVelocity().add(new Vector(0, 0.15, 0)));
                SpellUtils.dealWithChat(player, finalTarget, damage / 5.0, "Endless Assault");
                world.spawnParticle(Particle.SWEEP_ATTACK, finalTarget.getLocation(), 5, 0.2, 0.2, 0.2, 0.01);
                world.playSound(finalTarget.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
                hits++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 6L);

        player.sendMessage("§aYou relentlessly strike your foe!");
    }
}
