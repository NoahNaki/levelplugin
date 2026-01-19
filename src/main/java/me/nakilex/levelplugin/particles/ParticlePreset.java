package me.nakilex.levelplugin.particles;

import java.util.List;

import me.nakilex.levelplugin.particles.patterns.ParticlePattern;

public final class ParticlePreset {
    private final String name;
    private final List<ParticlePattern> patterns;
    private final ParticlePresetSettings settings;

    public ParticlePreset(String name, List<ParticlePattern> patterns, ParticlePresetSettings settings) {
        this.name = name;
        this.patterns = List.copyOf(patterns);
        this.settings = settings;
    }

    public String name() {
        return name;
    }

    public List<ParticlePattern> patterns() {
        return patterns;
    }

    public ParticlePresetSettings settings() {
        return settings;
    }
}
