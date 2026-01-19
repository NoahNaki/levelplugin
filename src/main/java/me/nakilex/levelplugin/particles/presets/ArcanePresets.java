package me.nakilex.levelplugin.particles.presets;

import me.nakilex.levelplugin.particles.ParticlePreset;
import me.nakilex.levelplugin.particles.ParticlePresetRegistry;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;

public class ArcanePresets implements ParticlePresetProvider {
    @Override
    public void register(ParticlePresetRegistry registry) {
        registry.register("ARCANE_SPARK", new ParticlePreset(
                Particle.DUST,
                new DustOptions(Color.fromRGB(163, 73, 255), 1.3f),
                0.03, 0.03, 0.03, 0.0));
        registry.register("VOID_GLIMMER", new ParticlePreset(
                Particle.DUST,
                new DustOptions(Color.fromRGB(80, 20, 120), 1.1f),
                0.02, 0.02, 0.02, 0.0));
    }
}
