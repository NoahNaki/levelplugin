package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.debug.ArcSlashDebugManager;
import me.nakilex.levelplugin.particles.ParticlePlane;
import me.nakilex.levelplugin.particles.ParticleRenderContext;
import me.nakilex.levelplugin.particles.ParticleRotationAxis;
import me.nakilex.levelplugin.particles.patterns.EllipseArcPattern;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/** Shared arc-slash visual + damage helpers for rogue-style melee spells. */
public final class ArcSlashCombatUtil {
    private static final ArcSlashDebugManager.ArcSlashConfig ARC_PRESET =
            ArcSlashDebugManager.ArcSlashConfig.defaultConfig();

    private ArcSlashCombatUtil() {
    }

    public static void strike(Player caster,
                              Location center,
                              Location orientation,
                              double damage,
                              double damageRadius) {
        if (caster == null || center == null || orientation == null || center.getWorld() == null) {
            return;
        }
        ArcSlashDebugManager.ArcSlashConfig config = ARC_PRESET.copy();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double radiusX = random.nextDouble(config.radiusXMin(), config.radiusXMax());
        double radiusZ = random.nextDouble(config.radiusZMin(), config.radiusZMax());
        double baseTilt = random.nextDouble(config.baseTiltMin(), config.baseTiltMax());
        double travelDistance = config.travelDistance() * 0.125;
        int renderTicks = Math.max(1, (int) Math.round(config.ticks() * 0.7));
        Vector direction = orientation.getDirection().clone().setY(0.0);
        if (direction.lengthSquared() <= 0.0001) {
            direction = caster.getLocation().getDirection().clone().setY(0.0);
        }
        direction.normalize();
        final Vector renderDirection = direction;
        Vector right = new Vector(0, 1, 0).crossProduct(direction).normalize();
        Vector up = new Vector(0, 1, 0);
        double sideShift = radiusX * config.sideShiftFactor() + config.rightOffset();
        Location baseCenter = center.clone()
                .add(renderDirection.clone().multiply(config.startDistance() + config.forwardOffset()))
                .add(right.clone().multiply(sideShift))
                .add(up.clone().multiply(config.upOffset()));

        EllipseArcPattern arcPreset = new EllipseArcPattern(config.particle(), null, radiusX, radiusZ, config.width(),
                config.startAngleDegrees(), config.endAngleDegrees(), 0.0, ParticlePlane.LOOK_VERTICAL,
                baseTilt, ParticleRotationAxis.LOOK_RIGHT,
                config.rotateXDegrees(), config.rotateYDegrees(), config.rotateZDegrees());

        Location finalImpact = baseCenter.clone().add(renderDirection.clone().multiply(travelDistance));
        SpellEffectUtil.applyAreaDamage(caster, finalImpact, damageRadius, damage);

        Main plugin = Main.getInstance();
        if (plugin == null) {
            ParticleRenderContext context = new ParticleRenderContext(caster, finalImpact, orientation,
                    Math.max(1, config.points()), 0, 1);
            arcPreset.render(context);
            return;
        }

        new BukkitRunnable() {
            private int tick;

            @Override
            public void run() {
                if (!caster.isOnline()) {
                    cancel();
                    return;
                }
                int frameStep = Math.max(1, config.frameStep());
                if (tick % frameStep != 0) {
                    tick++;
                    if (tick >= renderTicks) {
                        cancel();
                    }
                    return;
                }
                int frameCount = (int) Math.ceil((double) renderTicks / frameStep);
                int frameIndex = Math.min(frameCount - 1, tick / frameStep);
                double progress = frameCount <= 1 ? 1.0 : (double) frameIndex / (frameCount - 1);
                Location frameCenter = baseCenter.clone().add(renderDirection.clone().multiply(travelDistance * progress));
                ParticleRenderContext context = new ParticleRenderContext(caster, frameCenter, orientation,
                        Math.max(1, config.points()), tick, renderTicks);
                arcPreset.render(context);
                tick++;
                if (tick >= renderTicks) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public static void strikeForward(Player caster,
                                     double forwardDistance,
                                     double upOffset,
                                     double damage,
                                     double damageRadius) {
        if (caster == null) {
            return;
        }
        Vector forward = caster.getLocation().getDirection().setY(0.0);
        if (forward.lengthSquared() <= 0.0001) {
            return;
        }
        forward.normalize();
        Location center = caster.getLocation().clone()
                .add(forward.multiply(forwardDistance))
                .add(0.0, upOffset, 0.0);
        Location orientation = caster.getLocation().clone();
        orientation.setDirection(center.toVector().subtract(caster.getLocation().toVector()));
        strike(caster, center, orientation, damage, damageRadius);
    }
}
