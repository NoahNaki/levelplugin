package me.nakilex.levelplugin.particles;

import java.util.List;
import org.bukkit.Particle;

public record ParticlePreset(Particle particle,
                             Object data,
                             double offsetX,
                             double offsetY,
                             double offsetZ,
                             double extra,
                             int defaultCount,
                             int defaultTicks,
                             ParticleCenter defaultCenter,
                             List<ParticlePattern> patterns) {
    public static ParticlePreset basic(Particle particle) {
        return new ParticlePreset(particle, null, 0.02, 0.02, 0.02, 0.0, 8, 100,
                ParticleCenter.LOOK, List.of(ParticlePattern.point()));
    }
}
