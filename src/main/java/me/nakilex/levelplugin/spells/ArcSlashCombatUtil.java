package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.particles.ParticlePlane;
import me.nakilex.levelplugin.particles.ParticleRenderContext;
import me.nakilex.levelplugin.particles.ParticleRotationAxis;
import me.nakilex.levelplugin.particles.patterns.EllipseArcPattern;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/** Shared arc-slash visual + damage helpers for rogue-style melee spells. */
public final class ArcSlashCombatUtil {
    public static final double ARC_RADIUS_X_MIN = 1.0;
    public static final double ARC_RADIUS_X_MAX = 2.5;
    public static final double ARC_RADIUS_Z_MIN = 1.0;
    public static final double ARC_RADIUS_Z_MAX = 2.0;

    private ArcSlashCombatUtil() {
    }

    public static void strike(Player caster,
                              Location center,
                              Location orientation,
                              Particle particle,
                              double damage,
                              double damageRadius) {
        if (caster == null || center == null || orientation == null || center.getWorld() == null) {
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double radiusX = random.nextDouble(ARC_RADIUS_X_MIN, ARC_RADIUS_X_MAX);
        double radiusZ = random.nextDouble(ARC_RADIUS_Z_MIN, ARC_RADIUS_Z_MAX);
        ParticleRenderContext context = new ParticleRenderContext(caster, center, orientation, 9, 0, 1);

        EllipseArcPattern main = new EllipseArcPattern(particle, null, radiusX, radiusZ, 0.08,
                -75.0, 95.0, 0.0, ParticlePlane.LOOK_VERTICAL,
                random.nextDouble(-57.0, 72.0), ParticleRotationAxis.LOOK_RIGHT,
                180.0, -90.0, 180.0);
        EllipseArcPattern mirror = new EllipseArcPattern(particle, null, radiusX, radiusZ, 0.08,
                -75.0, 95.0, 0.0, ParticlePlane.LOOK_VERTICAL,
                random.nextDouble(-57.0, 72.0), ParticleRotationAxis.LOOK_RIGHT,
                180.0, 90.0, 180.0);
        main.render(context);
        mirror.render(context);

        SpellEffectUtil.applyAreaDamage(caster, center, damageRadius, damage);
    }
}
