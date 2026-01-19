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

public record CrescentPattern(Particle particle, Object data, double outerRadius, double innerRadius,
                              double offsetDistance, Vector localOffset, double rotationSpeed, ParticlePlane plane,
                              double tiltDegrees, ParticleRotationAxis tiltAxis) implements ParticlePattern {

    private static final double DEFAULT_POINT_SPACING = 0.18;
    private static final int MIN_ARC_POINTS = 2;

    @Override
    public void render(ParticleRenderContext context) {
        int configuredPoints = context.points();
        if (configuredPoints <= 0 || outerRadius <= 0 || innerRadius <= 0 || outerRadius <= innerRadius
                || offsetDistance <= 0) {
            return;
        }
        double rotation = Math.toRadians(rotationSpeed) * context.tick();
        World world = context.center().getWorld();
        Location origin = context.center().clone();
        if (localOffset != null) {
            Vector shift = ParticleMath.mapToPlane(localOffset, plane);
            shift = ParticleMath.orientAndTilt(shift, plane, context.orientation(), tiltAxis, tiltDegrees);
            origin.add(shift);
        }

        double xStar = (outerRadius * outerRadius - innerRadius * innerRadius + offsetDistance * offsetDistance)
                / (2 * offsetDistance);
        double yStarSquared = outerRadius * outerRadius - xStar * xStar;
        if (yStarSquared <= 0) {
            return;
        }
        double yStar = Math.sqrt(yStarSquared);
        double thetaStart = Math.atan2(-yStar, xStar);
        double thetaEnd = Math.atan2(yStar, xStar);

        double innerX = xStar - offsetDistance;
        double phiStart = Math.atan2(-yStar, innerX);
        double phiEnd = Math.atan2(yStar, innerX);

        double outerAngleSpan = thetaEnd - thetaStart;
        double innerAngleSpan = phiEnd - phiStart;
        int computedOuter = ParticleMath.pointsForArc(outerRadius, outerAngleSpan, DEFAULT_POINT_SPACING,
                MIN_ARC_POINTS);
        int computedInner = ParticleMath.pointsForArc(innerRadius, innerAngleSpan, DEFAULT_POINT_SPACING,
                MIN_ARC_POINTS);
        int computedTotal = computedOuter + computedInner;
        int outerPoints = computedOuter;
        int innerPoints = computedInner;
        if (configuredPoints > computedTotal) {
            double ratio = configuredPoints / (double) computedTotal;
            outerPoints = Math.max(MIN_ARC_POINTS, (int) Math.ceil(computedOuter * ratio));
            innerPoints = Math.max(MIN_ARC_POINTS, (int) Math.ceil(computedInner * ratio));
        }

        for (int i = 0; i < outerPoints; i++) {
            double progress = outerPoints == 1 ? 1.0 : (double) i / (outerPoints - 1);
            double angle = thetaStart + (thetaEnd - thetaStart) * progress + rotation;
            Vector offset = ParticleMath.buildOffset(angle, outerRadius, plane);
            offset = ParticleMath.orientAndTilt(offset, plane, context.orientation(), tiltAxis, tiltDegrees);
            Location spawn = origin.clone().add(offset);
            ParticleSpawnUtil.spawn(world, spawn, particle, 1, data);
        }

        for (int i = 0; i < innerPoints; i++) {
            double progress = innerPoints == 1 ? 1.0 : (double) i / (innerPoints - 1);
            double angle = phiEnd + (phiStart - phiEnd) * progress + rotation;
            double x = offsetDistance + innerRadius * Math.cos(angle);
            double z = innerRadius * Math.sin(angle);
            Vector offset = ParticleMath.mapToPlane(new Vector(x, 0, z), plane);
            offset = ParticleMath.orientAndTilt(offset, plane, context.orientation(), tiltAxis, tiltDegrees);
            Location spawn = origin.clone().add(offset);
            ParticleSpawnUtil.spawn(world, spawn, particle, 1, data);
        }
    }
}
