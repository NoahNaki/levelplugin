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

public record TrochoidPattern(Particle particle, Object data, double fixedRadius, double rollingRadius,
                              double offsetDistance, double rotationSpeed, ParticlePlane plane,
                              double tiltDegrees, ParticleRotationAxis tiltAxis) implements ParticlePattern {

    @Override
    public void render(ParticleRenderContext context) {
        int points = context.points();
        if (points <= 0) {
            return;
        }
        double rotation = Math.toRadians(rotationSpeed) * context.tick();
        World world = context.center().getWorld();
        double ratio = (fixedRadius - rollingRadius) / rollingRadius;
        for (int i = 0; i < points; i++) {
            double progress = points == 1 ? 1.0 : (double) i / (points - 1);
            double t = (Math.PI * 2 * progress) + rotation;
            double cos = Math.cos(t);
            double sin = Math.sin(t);
            double x = (fixedRadius - rollingRadius) * cos + offsetDistance * Math.cos(ratio * t);
            double z = (fixedRadius - rollingRadius) * sin - offsetDistance * Math.sin(ratio * t);
            Vector base = new Vector(x, 0, z);
            Vector offset = ParticleMath.mapToPlane(base, plane);
            offset = ParticleMath.orientAndTilt(offset, plane, context.player().getLocation(), tiltAxis, tiltDegrees);
            Location spawn = context.center().clone().add(offset);
            ParticleSpawnUtil.spawn(world, spawn, particle, 1, data);
        }
    }
}
