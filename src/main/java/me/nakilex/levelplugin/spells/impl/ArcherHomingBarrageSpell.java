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
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class ArcherHomingBarrageSpell implements SpellHandler {
    private final Main plugin;
    private final int arrowCount;
    private final long intervalTicks;
    private final double arrowSpeed;
    private final double homingStrength;
    private final double baseDamage;
    private final double dexScale;

    public ArcherHomingBarrageSpell(Main plugin,
                                    int arrowCount,
                                    long intervalTicks,
                                    double arrowSpeed,
                                    double homingStrength,
                                    double baseDamage,
                                    double dexScale) {
        this.plugin = plugin;
        this.arrowCount = Math.max(1, arrowCount);
        this.intervalTicks = Math.max(1L, intervalTicks);
        this.arrowSpeed = Math.max(0.2, arrowSpeed);
        this.homingStrength = Math.max(0.02, Math.min(0.45, homingStrength));
        this.baseDamage = Math.max(0.1, baseDamage);
        this.dexScale = Math.max(0.0, dexScale);
    }

    @Override
    public void cast(SpellContext context) {
        Player caster = context.player();
        double damage = SpellEffectUtil.computeDexTecScaledDamage(caster, baseDamage, dexScale, 0.001);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_SKELETON_SHOOT, 0.85f, 1.28f);

        new BukkitRunnable() {
            private int fired;

            @Override
            public void run() {
                if (!caster.isOnline() || fired >= arrowCount) {
                    cancel();
                    return;
                }
                shootHomingArrow(caster, damage);
                fired++;
            }
        }.runTaskTimer(plugin, 0L, intervalTicks);
    }

    private void shootHomingArrow(Player caster, double damage) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vector direction = caster.getEyeLocation().getDirection().clone();
        direction.add(new Vector(
                random.nextDouble(-0.07, 0.07),
                random.nextDouble(-0.04, 0.04),
                random.nextDouble(-0.07, 0.07)));

        Arrow arrow = ArcherArrowUtil.launchClassArrow(plugin, caster, direction, arrowSpeed, damage);
        if (arrow == null) {
            return;
        }
        caster.getWorld().spawnParticle(Particle.CRIT, caster.getEyeLocation(), 5, 0.12, 0.08, 0.12, 0.02);
        attachHomingTask(caster, arrow);
    }

    private void attachHomingTask(Player caster, Arrow arrow) {
        new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isInBlock() || arrow.isOnGround() || !caster.isOnline()) {
                    cancel();
                    return;
                }
                if (ticks++ > 36) {
                    cancel();
                    return;
                }

                Location point = arrow.getLocation();
                LivingEntity target = SpellTargetingUtil.resolveTargetLivingEntity(caster, 18.0, 0.7,
                        living -> !living.equals(caster)
                                && living.getWorld().equals(point.getWorld())
                                && living.getLocation().distanceSquared(point) <= 20.0 * 20.0);
                if (target == null) {
                    return;
                }

                Location targetPoint = target.getLocation().clone().add(0.0, Math.min(1.2, target.getHeight() * 0.55), 0.0);
                Vector desired = targetPoint.toVector().subtract(point.toVector());
                if (desired.lengthSquared() <= 0.000001) {
                    return;
                }
                Vector current = arrow.getVelocity();
                Vector next = current.multiply(1.0 - homingStrength)
                        .add(desired.normalize().multiply(current.length() * homingStrength));
                if (next.lengthSquared() <= 0.000001) {
                    return;
                }
                arrow.setVelocity(next);
                point.getWorld().spawnParticle(Particle.CRIT, point, 1, 0.01, 0.01, 0.01, 0.01);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
