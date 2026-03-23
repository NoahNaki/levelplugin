package me.nakilex.levelplugin.spells.impl;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.ArcherArrowUtil;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellHandler;
import me.nakilex.levelplugin.spells.SpellTargetingUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class ArcherArrowRainSpell implements SpellHandler {
    private final Main plugin;
    private final int volleys;
    private final int arrowsPerVolley;
    private final int volleyIntervalTicks;
    private final double radius;
    private final double height;
    private final double baseDamage;
    private final double dexScale;

    public ArcherArrowRainSpell(Main plugin,
                                int volleys,
                                int arrowsPerVolley,
                                int volleyIntervalTicks,
                                double radius,
                                double height,
                                double baseDamage,
                                double dexScale) {
        this.plugin = plugin;
        this.volleys = Math.max(1, volleys);
        this.arrowsPerVolley = Math.max(1, arrowsPerVolley);
        this.volleyIntervalTicks = Math.max(1, volleyIntervalTicks);
        this.radius = Math.max(1.0, radius);
        this.height = Math.max(4.0, height);
        this.baseDamage = Math.max(0.1, baseDamage);
        this.dexScale = Math.max(0.0, dexScale);
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        Location center = SpellTargetingUtil.resolveTargetGround(caster, 34.0);
        if (center == null) {
            center = caster.getEyeLocation().clone().add(caster.getEyeLocation().getDirection().multiply(14.0));
        }
        if (center.getWorld() == null) {
            return;
        }

        double damage = SpellEffectUtil.computeDexTecScaledDamage(caster, baseDamage, dexScale, 0.001);
        Location targetCenter = center.clone().add(0.0, 0.15, 0.0);
        targetCenter.getWorld().spawnParticle(Particle.CLOUD, targetCenter, 22, radius * 0.4, 0.12, radius * 0.4, 0.01);
        targetCenter.getWorld().playSound(targetCenter, Sound.ENTITY_ARROW_SHOOT, 0.95f, 0.82f);

        new BukkitRunnable() {
            private int volley;

            @Override
            public void run() {
                if (!caster.isOnline() || volley >= volleys) {
                    cancel();
                    return;
                }
                spawnVolley(caster, targetCenter, damage);
                volley++;
            }
        }.runTaskTimer(plugin, 0L, volleyIntervalTicks);
    }

    private void spawnVolley(Player caster, Location center, double damage) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < arrowsPerVolley; i++) {
            double angle = random.nextDouble(0.0, Math.PI * 2.0);
            double distance = random.nextDouble(0.0, radius);
            double x = Math.cos(angle) * distance;
            double z = Math.sin(angle) * distance;
            Location spawn = center.clone().add(x, height + random.nextDouble(-1.2, 1.2), z);
            Location impactPoint = center.clone().add(
                    random.nextDouble(-radius * 0.75, radius * 0.75),
                    0.0,
                    random.nextDouble(-radius * 0.75, radius * 0.75));
            Vector velocity = impactPoint.toVector().subtract(spawn.toVector()).normalize().multiply(2.7);
            ArcherArrowUtil.spawnClassArrow(plugin, caster, spawn, velocity, damage);
            spawn.getWorld().spawnParticle(Particle.CRIT, spawn, 1, 0.02, 0.02, 0.02, 0.01);
        }
    }
}
