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
        int bombCount = 1;
        Object bc = ctx.getExtraParam("bombCount");
        if (bc instanceof Number num) bombCount = Math.max(1, num.intValue());

        double radius = 3.0;
        Object rb = ctx.getExtraParam("radiusBonus");
        if (rb instanceof Number num) radius += num.doubleValue();

        int slowDur = 20;
        Object sd = ctx.getExtraParam("slowDuration");
        if (sd instanceof Number num) slowDur = num.intValue();

        Location targetLoc;
        Block block = player.getTargetBlockExact(15);
        if (block != null) {
            targetLoc = block.getLocation().add(0.5, 1, 0.5);
        } else {
            targetLoc = player.getLocation().add(player.getLocation().getDirection().multiply(8));
        }

        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 1f);

        boolean playerBuff = Boolean.TRUE.equals(ctx.getExtraParam("playerBuff"));

        if (playerBuff) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, slowDur * bombCount, 0));
        }

        for (int i = 0; i < bombCount; i++) {
            Location loc = targetLoc.clone().add(0, i * 0.2, 0);
            int finalSlowDur = slowDur;
            double finalRadius = radius;
            double finalRadius1 = radius;
            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (ticks++ >= 80) { cancel(); return; }
                    world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 10, finalRadius, 0.5, finalRadius1, 0.01);
                    for (Entity e : world.getNearbyEntities(loc, finalRadius, 2, finalRadius1)) {
                        if (e instanceof LivingEntity le && !le.equals(player)) {
                            SpellUtils.dealWithChat(player, le, ctx.getFinalDamage()/4.0, "Smoke Bomb");
                            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, finalSlowDur, 1, false, false));
                        }
                    }
                }
            }.runTaskTimer(Main.getInstance(), i * 10L, 20L);
        }
    }
}
