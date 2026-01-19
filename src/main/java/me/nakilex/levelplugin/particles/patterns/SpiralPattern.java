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

public record SpiralPattern(Particle particle, Object data, double baseRadius, double baseHeight,
                            double rotationSpeed, double turns, boolean inward, ParticlePlane plane,
                            double tiltDegrees, ParticleRotationAxis tiltAxis) implements ParticlePattern {

    @Override
    public void render(ParticleRenderContext context) {
        int points = context.points();
        if (points <= 0) {
            return;
        }
        double rotation = Math.toRadians(rotationSpeed) * context.tick();
        World world = context.center().getWorld();
        for (int i = 0; i < points; i++) {
            double progress = points == 1 ? 1.0 : (double) i / (points - 1);
            double radiusProgress = inward ? 1.0 - progress : progress;
            double radius = baseRadius * radiusProgress;
            double height = baseHeight * progress;
            double angle = (Math.PI * 2 * turns * progress) + rotation;
            Vector offset = ParticleMath.buildOffset(angle, radius, plane);
            offset = ParticleMath.addHeight(offset, height, plane, context.orientation());
            offset = ParticleMath.orientAndTilt(offset, plane, context.orientation(), tiltAxis, tiltDegrees);
            Location spawn = context.center().clone().add(offset);
            ParticleSpawnUtil.spawn(world, spawn, particle, 1, data);
        }
    }
}
