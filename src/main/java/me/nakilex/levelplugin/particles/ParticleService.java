package me.nakilex.levelplugin.particles;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.Particle.DustOptions;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ParticleService {
    private static final ParticleService INSTANCE = new ParticleService();

    private final Map<String, ParticlePreset> presets = new HashMap<>();

    private ParticleService() {
        registerDefaults();
    }

    public static ParticleService getInstance() {
        return INSTANCE;
    }

    public Optional<ParticlePreset> resolvePreset(String input) {
        String key = input == null ? "" : input.trim().toUpperCase().replace('-', '_');
        if (key.isEmpty()) {
            return Optional.empty();
        }
        ParticlePreset preset = presets.get(key);
        if (preset != null) {
            return Optional.of(preset);
        }
        try {
            Particle particle = Particle.valueOf(key);
            return Optional.of(ParticlePreset.basic(particle));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public void sendToPlayer(Player player, ParticlePreset preset, Location location, int count) {
        spawnParticle(player.getWorld(), location, preset, count);
    }

    public void sendRing(Player player, ParticlePreset preset, Location center, double radius, int points) {
        double step = (Math.PI * 2.0) / points;
        for (int i = 0; i < points; i++) {
            double angle = step * i;
            double x = center.getX() + (Math.cos(angle) * radius);
            double z = center.getZ() + (Math.sin(angle) * radius);
            spawnParticle(player.getWorld(), new Location(center.getWorld(), x, center.getY(), z), preset, 1);
        }
    }

    public void sendArc(Player player, ParticlePreset preset, Location center, double radius, double degrees, int points) {
        double radians = Math.toRadians(degrees);
        double step = radians / Math.max(points - 1, 1);
        double start = -radians / 2.0;
        for (int i = 0; i < points; i++) {
            double angle = start + (step * i);
            double x = center.getX() + (Math.cos(angle) * radius);
            double z = center.getZ() + (Math.sin(angle) * radius);
            spawnParticle(player.getWorld(), new Location(center.getWorld(), x, center.getY(), z), preset, 1);
        }
    }

    public List<String> getPresetNames() {
        return presets.keySet().stream()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private void registerDefaults() {
        registerPreset("EMBER", new ParticlePreset(Particle.FLAME, null, 0.02, 0.02, 0.02, 0.0));
        registerPreset("ARCANE_SPARK", new ParticlePreset(
                Particle.DUST,
                new DustOptions(org.bukkit.Color.fromRGB(163, 73, 255), 1.3f),
                0.03, 0.03, 0.03, 0.0));
        registerPreset("SOUL_MIST", new ParticlePreset(Particle.SOUL, null, 0.02, 0.02, 0.02, 0.0));
        registerPreset("HEALING_AURA", new ParticlePreset(Particle.HEART, null, 0.03, 0.06, 0.03, 0.0));
        registerPreset("CELESTIAL", new ParticlePreset(Particle.END_ROD, null, 0.01, 0.01, 0.01, 0.0));
    }

    private void registerPreset(String name, ParticlePreset preset) {
        presets.put(name.toUpperCase(), preset);
    }

    private void spawnParticle(World world, Location location, ParticlePreset preset, int count) {
        if (preset.data() != null) {
            world.spawnParticle(preset.particle(), location, count,
                    preset.offsetX(), preset.offsetY(), preset.offsetZ(), preset.extra(), preset.data());
            return;
        }
        world.spawnParticle(preset.particle(), location, count,
                preset.offsetX(), preset.offsetY(), preset.offsetZ(), preset.extra());
    }

    public record ParticlePreset(Particle particle, Object data, double offsetX, double offsetY, double offsetZ, double extra) {
        public static ParticlePreset basic(Particle particle) {
            return new ParticlePreset(particle, null, 0.02, 0.02, 0.02, 0.0);
        }
    }
}
