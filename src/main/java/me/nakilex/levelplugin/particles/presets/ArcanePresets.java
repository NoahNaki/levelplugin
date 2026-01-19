package me.nakilex.levelplugin.particles.presets;

import me.nakilex.levelplugin.particles.ParticleCenter;
import me.nakilex.levelplugin.particles.ParticlePattern;
import me.nakilex.levelplugin.particles.ParticlePreset;
import me.nakilex.levelplugin.particles.ParticlePresetRegistry;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;

import java.util.List;

public class ArcanePresets implements ParticlePresetProvider {
    @Override
    public void register(ParticlePresetRegistry registry) {
        registry.register("ARCANE_SPARK", new ParticlePreset(
                Particle.DUST,
                new DustOptions(Color.fromRGB(163, 73, 255), 1.3f),
                0.03, 0.03, 0.03, 0.0,
                10, 100, ParticleCenter.LOOK,
                List.of(
                        ParticlePattern.ring(1.1, 30, 0.03),
                        ParticlePattern.star(1.4, 0.5, 5, 20, -0.03)
                )));
        registry.register("VOID_GLIMMER", new ParticlePreset(
                Particle.DUST,
                new DustOptions(Color.fromRGB(80, 20, 120), 1.1f),
                0.02, 0.02, 0.02, 0.0,
                12, 100, ParticleCenter.LOOK,
                List.of(
                        ParticlePattern.spiral(1.2, 24, 0.6, -0.05),
                        ParticlePattern.ring(0.6, 18, 0.02)
                )));
    }
}
