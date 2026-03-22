package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.debug.ArcSlashDebugManager;
import me.nakilex.levelplugin.particles.ParticlePlane;
import me.nakilex.levelplugin.particles.ParticleRenderContext;
import me.nakilex.levelplugin.particles.ParticleRotationAxis;
import me.nakilex.levelplugin.particles.patterns.EllipseArcPattern;
import org.bukkit.Location;
import org.bukkit.entity.Player;
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
        ParticleRenderContext context = new ParticleRenderContext(caster, center, orientation,
                Math.max(1, config.points()), 0, 1);

        EllipseArcPattern arcPreset = new EllipseArcPattern(config.particle(), null, radiusX, radiusZ, config.width(),
                config.startAngleDegrees(), config.endAngleDegrees(), 0.0, ParticlePlane.LOOK_VERTICAL,
                baseTilt, ParticleRotationAxis.LOOK_RIGHT,
                config.rotateXDegrees(), config.rotateYDegrees(), config.rotateZDegrees());
        arcPreset.render(context);

        SpellEffectUtil.applyAreaDamage(caster, center, damageRadius, damage);
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
