package me.nakilex.levelplugin.particles;

import me.nakilex.levelplugin.particles.presets.ArcanePresets;
import me.nakilex.levelplugin.particles.presets.DivinePresets;
import me.nakilex.levelplugin.particles.presets.ElementalPresets;
import me.nakilex.levelplugin.particles.presets.ParticlePresetProvider;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Optional;

public final class ParticleService {
    private static final ParticleService INSTANCE = new ParticleService();

    private final ParticlePresetRegistry registry = new ParticlePresetRegistry();

    private ParticleService() {
        registerProviders(List.of(
                new ElementalPresets(),
                new ArcanePresets(),
                new DivinePresets()
        ));
    }

    public static ParticleService getInstance() {
        return INSTANCE;
    }

    public Optional<ParticlePreset> resolvePreset(String input) {
        String key = input == null ? "" : input.trim().toUpperCase().replace('-', '_');
        if (key.isEmpty()) {
            return Optional.empty();
        }
        Optional<ParticlePreset> preset = registry.get(key);
        if (preset.isPresent()) {
            return preset;
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

    public void renderPreset(Player player, ParticlePreset preset, Location center, Location orientation, int count, int tick) {
        if (preset.patterns() == null || preset.patterns().isEmpty()) {
            spawnParticle(player.getWorld(), center, preset, count);
            return;
        }
        for (ParticlePattern pattern : preset.patterns()) {
            renderPattern(player, preset, pattern, center, orientation, count, tick);
        }
    }

    public void sendRing(Player player, ParticlePreset preset, Location center, double radius, int points,
                         ParticleAxis axis, ParticleRotationAxis tiltAxis, double tiltDegrees, Location orientation) {
        double step = (Math.PI * 2.0) / points;
        for (int i = 0; i < points; i++) {
            double angle = step * i;
            Vector offset = buildOffset(angle, radius, axis, tiltAxis, tiltDegrees, orientation);
            spawnParticle(player.getWorld(), center.clone().add(offset), preset, 1);
        }
    }

    public void sendArc(Player player, ParticlePreset preset, Location center, double radius, double degrees, int points,
                        ParticleAxis axis, ParticleRotationAxis tiltAxis, double tiltDegrees, Location orientation) {
        double radians = Math.toRadians(degrees);
        double step = radians / Math.max(points - 1, 1);
        double start = -radians / 2.0;
        for (int i = 0; i < points; i++) {
            double angle = start + (step * i);
            Vector offset = buildOffset(angle, radius, axis, tiltAxis, tiltDegrees, orientation);
            spawnParticle(player.getWorld(), center.clone().add(offset), preset, 1);
        }
    }

    public List<String> getPresetNames() {
        return registry.getNames();
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

    private void registerProviders(List<ParticlePresetProvider> providers) {
        providers.forEach(provider -> provider.register(registry));
    }

    private Vector buildOffset(double angle, double radius, ParticleAxis axis, ParticleRotationAxis tiltAxis,
                               double tiltDegrees, Location orientation) {
        Vector base;
        switch (axis) {
            case X -> base = new Vector(0.0, Math.cos(angle) * radius, Math.sin(angle) * radius);
            case Z -> base = new Vector(Math.cos(angle) * radius, Math.sin(angle) * radius, 0.0);
            case LOOK -> base = new Vector(0.0, Math.sin(angle) * radius, Math.cos(angle) * radius);
            case Y -> base = new Vector(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
            default -> base = new Vector(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
        }

        if (tiltDegrees != 0.0) {
            base = rotateByAxis(base, tiltAxis, tiltDegrees);
        }

        if (axis == ParticleAxis.LOOK && orientation != null) {
            return rotateByOrientation(base, orientation);
        }
        return base;
    }

    private Vector rotateByOrientation(Vector vector, Location orientation) {
        Vector rotated = vector.clone();
        rotated.rotateAroundX(Math.toRadians(orientation.getPitch()));
        rotated.rotateAroundY(Math.toRadians(-orientation.getYaw()));
        return rotated;
    }

    private Vector rotateByAxis(Vector vector, ParticleRotationAxis axis, double degrees) {
        Vector rotated = vector.clone();
        double radians = Math.toRadians(degrees);
        return switch (axis) {
            case X -> rotated.rotateAroundX(radians);
            case Y -> rotated.rotateAroundY(radians);
            case Z -> rotated.rotateAroundZ(radians);
        };
    }

    private void renderPattern(Player player, ParticlePreset preset, ParticlePattern pattern, Location center,
                               Location orientation, int count, int tick) {
        int countPerPoint = Math.max(1, count / Math.max(pattern.points(), 1));
        double rotation = pattern.rotationSpeed() * tick;
        switch (pattern.type()) {
            case POINT -> spawnParticle(player.getWorld(), center, preset, count);
            case RING -> renderRingPattern(player, preset, pattern, center, orientation, rotation, countPerPoint);
            case STAR -> renderStarPattern(player, preset, pattern, center, orientation, rotation, countPerPoint);
            case SPIRAL -> renderSpiralPattern(player, preset, pattern, center, orientation, rotation, countPerPoint);
        }
    }

    private void renderRingPattern(Player player, ParticlePreset preset, ParticlePattern pattern, Location center,
                                   Location orientation, double rotation, int count) {
        double step = (Math.PI * 2.0) / Math.max(pattern.points(), 1);
        for (int i = 0; i < pattern.points(); i++) {
            double angle = rotation + (step * i);
            Vector offset = new Vector(Math.cos(angle) * pattern.radius(), pattern.height(), Math.sin(angle) * pattern.radius());
            spawnParticle(player.getWorld(), center.clone().add(rotateAroundYaw(offset, orientation)), preset, count);
        }
    }

    private void renderStarPattern(Player player, ParticlePreset preset, ParticlePattern pattern, Location center,
                                   Location orientation, double rotation, int count) {
        int points = pattern.points() > 0 ? pattern.points() : Math.max(pattern.arms() * 2, 2);
        double step = (Math.PI * 2.0) / points;
        for (int i = 0; i < points; i++) {
            double angle = rotation + (step * i);
            double radius = i % 2 == 0 ? pattern.radius() : pattern.innerRadius();
            Vector offset = new Vector(Math.cos(angle) * radius, pattern.height(), Math.sin(angle) * radius);
            spawnParticle(player.getWorld(), center.clone().add(rotateAroundYaw(offset, orientation)), preset, count);
        }
    }

    private void renderSpiralPattern(Player player, ParticlePreset preset, ParticlePattern pattern, Location center,
                                     Location orientation, double rotation, int count) {
        int points = Math.max(pattern.points(), 1);
        double step = (Math.PI * 2.0) / points;
        for (int i = 0; i < points; i++) {
            double progress = points == 1 ? 1.0 : (double) i / (points - 1);
            double angle = rotation + (step * i);
            double radius = pattern.radius() * progress;
            double height = pattern.height() * progress;
            Vector offset = new Vector(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
            spawnParticle(player.getWorld(), center.clone().add(rotateAroundYaw(offset, orientation)), preset, count);
        }
    }

    private Vector rotateAroundYaw(Vector vector, Location orientation) {
        if (orientation == null) {
            return vector;
        }
        Vector rotated = vector.clone();
        rotated.rotateAroundY(Math.toRadians(-orientation.getYaw()));
        return rotated;
    }
}
