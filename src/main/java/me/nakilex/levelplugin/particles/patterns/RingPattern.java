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

public record RingPattern(Particle particle, Object data, double radius, double rotationSpeed,
                          ParticlePlane plane, double tiltDegrees, ParticleRotationAxis tiltAxis)
        implements ParticlePattern {

    @Override
    public void render(ParticleRenderContext context) {
        int points = context.points();
        if (points <= 0) {
            return;
        }
        double rotation = Math.toRadians(rotationSpeed) * context.tick();
        World world = context.center().getWorld();
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2 * i / points) + rotation;
            Vector offset = ParticleMath.buildOffset(angle, radius, plane);
            if (plane == ParticlePlane.LOOK) {
                offset = ParticleMath.rotateByOrientation(offset, context.player().getLocation());
            }
            offset = ParticleMath.rotateByAxis(offset, tiltAxis, tiltDegrees);
            Location spawn = context.center().clone().add(offset);
            ParticleSpawnUtil.spawn(world, spawn, particle, 1, data);
        }
    }
}
