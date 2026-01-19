package me.nakilex.levelplugin.particles.presets;

import me.nakilex.levelplugin.particles.ParticleCenter;
import me.nakilex.levelplugin.particles.ParticlePattern;
import me.nakilex.levelplugin.particles.ParticlePreset;
import me.nakilex.levelplugin.particles.ParticlePresetRegistry;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;

import java.util.List;

public class DivinePresets implements ParticlePresetProvider {
    @Override
    public void register(ParticlePresetRegistry registry) {
        registry.register("HEALING_AURA", new ParticlePreset(
                Particle.HEART,
                null,
                0.03, 0.06, 0.03, 0.0,
                6, 100, ParticleCenter.LOOK,
                List.of(
                        ParticlePattern.ring(1.0, 22, 0.01),
                        ParticlePattern.spiral(0.6, 16, 0.8, 0.05)
                )));
        registry.register("CELESTIAL", new ParticlePreset(
                Particle.END_ROD,
                null,
                0.01, 0.01, 0.01, 0.0,
                8, 100, ParticleCenter.LOOK,
                List.of(
                        ParticlePattern.ring(1.3, 28, 0.02),
                        ParticlePattern.star(1.0, 0.4, 4, 16, 0.02)
                )));
        registry.register("RADIANT_SIGIL", new ParticlePreset(
                Particle.DUST,
                new DustOptions(Color.fromRGB(255, 221, 89), 1.4f),
                0.02, 0.02, 0.02, 0.0,
                10, 100, ParticleCenter.LOOK,
                List.of(
                        ParticlePattern.star(1.5, 0.5, 6, 24, 0.03),
                        ParticlePattern.ring(0.8, 18, -0.02)
                )));
    }
}
