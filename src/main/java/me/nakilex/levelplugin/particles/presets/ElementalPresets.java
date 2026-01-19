package me.nakilex.levelplugin.particles.presets;

import me.nakilex.levelplugin.particles.ParticleCenter;
import me.nakilex.levelplugin.particles.ParticlePattern;
import me.nakilex.levelplugin.particles.ParticlePreset;
import me.nakilex.levelplugin.particles.ParticlePresetRegistry;
import org.bukkit.Particle;

import java.util.List;

public class ElementalPresets implements ParticlePresetProvider {
    @Override
    public void register(ParticlePresetRegistry registry) {
        registry.register("EMBER", new ParticlePreset(
                Particle.FLAME,
                null,
                0.02, 0.02, 0.02, 0.0,
                8, 100, ParticleCenter.LOOK,
                List.of(
                        ParticlePattern.ring(1.2, 32, 0.02),
                        ParticlePattern.star(1.4, 0.6, 5, 20, -0.02)
                )));
        registry.register("SOUL_MIST", new ParticlePreset(
                Particle.SOUL,
                null,
                0.02, 0.02, 0.02, 0.0,
                10, 100, ParticleCenter.LOOK,
                List.of(
                        ParticlePattern.spiral(1.0, 28, 1.2, 0.08),
                        ParticlePattern.ring(1.4, 28, -0.01)
                )));
        registry.register("FROST_SHARD", new ParticlePreset(
                Particle.SNOWFLAKE,
                null,
                0.02, 0.02, 0.02, 0.0,
                12, 100, ParticleCenter.LOOK,
                List.of(
                        ParticlePattern.star(1.6, 0.7, 6, 24, 0.0),
                        ParticlePattern.ring(1.1, 26, 0.01)
                )));
    }
}
