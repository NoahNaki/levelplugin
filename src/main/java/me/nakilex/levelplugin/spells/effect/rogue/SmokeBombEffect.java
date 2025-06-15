package me.nakilex.levelplugin.spells.effect.rogue;

import me.nakilex.levelplugin.Main;
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
 * Simple smoke bomb that damages enemies in an area for a short time.
 */
public class SmokeBombEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        World world = player.getWorld();
        Location target = player.getLocation().add(player.getLocation().getDirection().multiply(5));

        world.playSound(target, Sound.ENTITY_TNT_PRIMED, 1f, 1.2f);

        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 100; // 5 seconds
            @Override
            public void run() {
                if (ticks++ >= duration) { cancel(); return; }
                world.spawnParticle(Particle.SMOKE_NORMAL, target, 20, 1, 1, 1, 0.1);
                for (Entity e : world.getNearbyEntities(target, 3, 3, 3)) {
                    if (e instanceof LivingEntity le && !le.equals(player)) {
                        SpellUtils.dealWithChat(player, le, ctx.getFinalDamage() * 0.2, "Smoke Bomb");
                    }
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 20L);
    }
}
