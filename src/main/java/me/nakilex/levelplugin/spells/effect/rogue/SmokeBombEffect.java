package me.nakilex.levelplugin.spells.effect.rogue;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class SmokeBombEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        Location targetLoc;
        Block block = player.getTargetBlockExact(15);
        if (block != null) {
            targetLoc = block.getLocation().add(0.5, 1, 0.5);
        } else {
            targetLoc = player.getLocation().add(player.getLocation().getDirection().multiply(8));
        }
        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 1f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ >= 80) { cancel(); return; }
                world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, targetLoc, 10, 0.5, 0.5, 0.5, 0.01);
                for (Entity e : world.getNearbyEntities(targetLoc, 3, 2, 3)) {
                    if (e instanceof LivingEntity le && !le.equals(player)) {
                        SpellUtils.dealWithChat(player, le, ctx.getFinalDamage()/4.0, "Smoke Bomb");
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 1, false, false));
                    }
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 20L);
    }
}
