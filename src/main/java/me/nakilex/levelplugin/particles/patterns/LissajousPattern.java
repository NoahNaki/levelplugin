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

public record LissajousPattern(Particle particle, Object data, double amplitudeX, double amplitudeZ,
                               double frequencyX, double frequencyZ, double phaseDegrees,
                               double rotationSpeed, ParticlePlane plane, double tiltDegrees,
                               ParticleRotationAxis tiltAxis) implements ParticlePattern {

    @Override
    public void render(ParticleRenderContext context) {
        int points = context.points();
        if (points <= 0) {
            return;
        }
        double phase = Math.toRadians(phaseDegrees);
        double rotation = Math.toRadians(rotationSpeed) * context.tick();
        World world = context.center().getWorld();
        for (int i = 0; i < points; i++) {
            double progress = points == 1 ? 1.0 : (double) i / (points - 1);
            double t = (Math.PI * 2 * progress) + rotation;
            double x = amplitudeX * Math.sin(frequencyX * t + phase);
            double z = amplitudeZ * Math.sin(frequencyZ * t);
            Vector base = new Vector(x, 0, z);
            Vector offset = ParticleMath.mapToPlane(base, plane);
            offset = ParticleMath.orientAndTilt(offset, plane, context.orientation(), tiltAxis, tiltDegrees);
            Location spawn = context.center().clone().add(offset);
            ParticleSpawnUtil.spawn(world, spawn, particle, 1, data);
        }
    }
}
