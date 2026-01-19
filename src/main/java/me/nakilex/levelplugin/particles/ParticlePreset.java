package me.nakilex.levelplugin.particles;

import org.bukkit.Particle;

public record ParticlePreset(Particle particle,
                             Object data,
                             double offsetX,
                             double offsetY,
                             double offsetZ,
                             double extra,
                             int defaultCount,
                             int defaultTicks,
                             ParticleCenter defaultCenter) {
    public static ParticlePreset basic(Particle particle) {
        return new ParticlePreset(particle, null, 0.02, 0.02, 0.02, 0.0, 8, 100, ParticleCenter.LOOK);
    }
}
