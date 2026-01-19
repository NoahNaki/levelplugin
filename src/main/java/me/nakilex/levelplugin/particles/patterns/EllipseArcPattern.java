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
                                double width, double startAngleDegrees, double endAngleDegrees,
                                double rotationSpeed, ParticlePlane plane, double tiltDegrees,
                                ParticleRotationAxis tiltAxis)
        implements ParticlePattern {

    private static final double DEFAULT_POINT_SPACING = 0.18;
    private static final double DEFAULT_WIDTH_STEP = 0.08;

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
        int widthSteps = width <= 0 ? 1 : Math.max(1, (int) Math.ceil(width / DEFAULT_WIDTH_STEP));
        double halfWidth = width / 2.0;
        for (int i = 0; i < points; i++) {
            double progress = points == 1 ? 1.0 : (double) i / (points - 1);
            double angle = start + (end - start) * progress + rotation;
            for (int w = 0; w < widthSteps; w++) {
                double wProgress = widthSteps == 1 ? 0.5 : (double) w / (widthSteps - 1);
                double offsetAmount = -halfWidth + (width * wProgress);
                double adjustedRadiusX = Math.max(0.05, radiusX + offsetAmount);
                double adjustedRadiusZ = Math.max(0.05, radiusZ + offsetAmount);
                Vector offset = buildEllipseOffset(angle, adjustedRadiusX, adjustedRadiusZ);
                offset = ParticleMath.orientAndTilt(offset, plane, context.orientation(), tiltAxis, tiltDegrees);
                Location spawn = context.center().clone().add(offset);
                ParticleSpawnUtil.spawn(world, spawn, particle, 1, data);
            }
        }
    }

    private Vector buildEllipseOffset(double angle, double radiusX, double radiusZ) {
        if (plane == ParticlePlane.LOOK_VERTICAL) {
            double x = Math.cos(angle) * radiusX;
            double y = Math.sin(angle) * radiusZ;
            return new Vector(x, y, 0);
        }
        return ParticleMath.buildEllipseOffset(angle, radiusX, radiusZ, plane);
    }
}
