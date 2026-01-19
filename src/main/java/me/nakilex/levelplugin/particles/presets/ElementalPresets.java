package me.nakilex.levelplugin.particles.presets;

import me.nakilex.levelplugin.particles.ParticleCenter;
import me.nakilex.levelplugin.particles.ParticlePreset;
import me.nakilex.levelplugin.particles.ParticlePresetRegistry;
import org.bukkit.Particle;

public class ElementalPresets implements ParticlePresetProvider {
    @Override
    public void register(ParticlePresetRegistry registry) {
        registry.register("EMBER", new ParticlePreset(Particle.FLAME, null, 0.02, 0.02, 0.02, 0.0, 8, 100, ParticleCenter.LOOK));
        registry.register("SOUL_MIST", new ParticlePreset(Particle.SOUL, null, 0.02, 0.02, 0.02, 0.0, 10, 100, ParticleCenter.LOOK));
        registry.register("FROST_SHARD", new ParticlePreset(Particle.SNOWFLAKE, null, 0.02, 0.02, 0.02, 0.0, 12, 100, ParticleCenter.LOOK));
    }
}
