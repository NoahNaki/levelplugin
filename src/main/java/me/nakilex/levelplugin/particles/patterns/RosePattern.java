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

public record RosePattern(Particle particle, Object data, double radius, double petals,
                          double rotationSpeed, ParticlePlane plane, double tiltDegrees,
                          ParticleRotationAxis tiltAxis) implements ParticlePattern {

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
            double angle = (Math.PI * 2 * progress) + rotation;
            double radial = radius * Math.cos(petals * angle);
            Vector offset = ParticleMath.buildOffset(angle, radial, plane);
            offset = ParticleMath.orientAndTilt(offset, plane, context.orientation(), tiltAxis, tiltDegrees);
            Location spawn = context.center().clone().add(offset);
            ParticleSpawnUtil.spawn(world, spawn, particle, 1, data);
        }
    }
}
