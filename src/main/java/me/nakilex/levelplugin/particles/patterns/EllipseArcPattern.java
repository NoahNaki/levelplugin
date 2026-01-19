package me.nakilex.levelplugin.particles.patterns;

import me.nakilex.levelplugin.particles.ParticleMath;
import me.nakilex.levelplugin.particles.ParticlePlane;
import me.nakilex.levelplugin.particles.ParticleRenderContext;
import me.nakilex.levelplugin.particles.ParticleRotationAxis;
import me.nakilex.levelplugin.particles.ParticleSpawnUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

public record EllipseArcPattern(Particle particle, Object data, double radiusX, double radiusZ,
                                double startAngleDegrees, double endAngleDegrees, double rotationSpeed,
                                ParticlePlane plane, double tiltDegrees, ParticleRotationAxis tiltAxis)
        implements ParticlePattern {

    private static final double DEFAULT_POINT_SPACING = 0.18;

    @Override
    public void render(ParticleRenderContext context) {
        int configuredPoints = context.points();
        if (configuredPoints <= 0 || radiusX <= 0 || radiusZ <= 0) {
            return;
        }
        double start = Math.toRadians(startAngleDegrees);
        double end = Math.toRadians(endAngleDegrees);
        double averageRadius = (radiusX + radiusZ) / 2.0;
        int points = ParticleMath.pointsForArc(averageRadius, end - start, DEFAULT_POINT_SPACING, configuredPoints);
        double rotation = Math.toRadians(rotationSpeed) * context.tick();
        World world = context.center().getWorld();
        for (int i = 0; i < points; i++) {
            double progress = points == 1 ? 1.0 : (double) i / (points - 1);
            double angle = start + (end - start) * progress + rotation;
            Vector offset = ParticleMath.buildEllipseOffset(angle, radiusX, radiusZ, plane);
            offset = ParticleMath.orientAndTilt(offset, plane, context.orientation(), tiltAxis, tiltDegrees);
            Location spawn = context.center().clone().add(offset);
            ParticleSpawnUtil.spawn(world, spawn, particle, 1, data);
        }
    }
}
