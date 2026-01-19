package me.nakilex.levelplugin.particles;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ParticlePresetRegistry {
    private final Map<String, ParticlePreset> presets = new HashMap<>();

    public void register(String name, ParticlePreset preset) {
        presets.put(name.toUpperCase(), preset);
    }

    public Optional<ParticlePreset> get(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(presets.get(name.trim().toUpperCase()));
    }

    public List<String> getNames() {
        return presets.keySet().stream()
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
