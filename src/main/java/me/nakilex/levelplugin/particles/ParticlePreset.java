package me.nakilex.levelplugin.particles;

import org.bukkit.Particle;

public record ParticlePreset(Particle particle, Object data, double offsetX, double offsetY, double offsetZ, double extra) {
    public static ParticlePreset basic(Particle particle) {
        return new ParticlePreset(particle, null, 0.02, 0.02, 0.02, 0.0);
    }
}
