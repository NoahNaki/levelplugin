package me.nakilex.levelplugin.horse.managers;

import me.nakilex.levelplugin.particles.ParticlePreset;
import me.nakilex.levelplugin.particles.ParticleService;
import me.nakilex.levelplugin.particles.presets.ElementalPresets;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Encapsulates horse trail preset utilities and per-player trail task lifecycle.
 */
public class HorseTrailService {

    public static final String OFF_PRESET = "OFF";

    private final ParticleService particleService;
    private final Map<UUID, BukkitTask> trailTasks = new java.util.HashMap<>();

    public HorseTrailService(ParticleService particleService) {
        this.particleService = particleService;
    }

    public List<String> getPresetOptions() {
        List<String> options = new ArrayList<>();
        options.add(OFF_PRESET);
        options.addAll(ElementalPresets.getPresetNames());
        return options;
    }

    public String normalizePreset(String presetName) {
        if (presetName == null || presetName.isBlank()) {
            return OFF_PRESET;
        }
        String normalized = presetName.toUpperCase(Locale.ROOT);
        if (OFF_PRESET.equals(normalized) || ElementalPresets.getPreset(normalized) != null) {
            return normalized;
        }
        return OFF_PRESET;
    }

    public String cyclePreset(String current, boolean backwards) {
        List<String> options = getPresetOptions();
        String normalizedCurrent = normalizePreset(current);

        int index = 0;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).equalsIgnoreCase(normalizedCurrent)) {
                index = i;
                break;
            }
        }

        index = backwards ? index - 1 : index + 1;
        if (index < 0) {
            index = options.size() - 1;
        } else if (index >= options.size()) {
            index = 0;
        }
        return options.get(index);
    }

    public String formatPresetName(String name) {
        String normalized = normalizePreset(name);
        if (OFF_PRESET.equals(normalized)) {
            return "Off";
        }
        String lower = normalized.toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] parts = lower.split(" ");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    public void startTrail(UUID ownerId, Player player, AbstractHorse horse, String presetName) {
        stopTrail(ownerId);
        if (player == null || horse == null || !horse.isValid()) {
            return;
        }

        String normalized = normalizePreset(presetName);
        if (OFF_PRESET.equals(normalized)) {
            return;
        }

        ParticlePreset preset = ElementalPresets.getPreset(normalized);
        if (preset == null) {
            return;
        }

        BukkitTask task = particleService.renderPresetWhile(
                player,
                preset,
                () -> horse.isValid() ? horse.getLocation().clone().add(0, 1.1, 0) : null,
                () -> player.isOnline()
                        && horse.isValid()
                        && player.isInsideVehicle()
                        && player.getVehicle() != null
                        && player.getVehicle().getUniqueId().equals(horse.getUniqueId())
                        && ownerId.equals(player.getUniqueId())
        );
        trailTasks.put(ownerId, task);
    }

    public void stopTrail(UUID ownerId) {
        BukkitTask task = trailTasks.remove(ownerId);
        if (task != null) {
            task.cancel();
        }
    }

    public void stopAll() {
        for (UUID ownerId : new ArrayList<>(trailTasks.keySet())) {
            stopTrail(ownerId);
        }
    }

    public AbstractHorse resolveOwnedHorse(UUID ownerId, UUID horseEntityId) {
        if (ownerId == null || horseEntityId == null) {
            return null;
        }
        var entity = Bukkit.getEntity(horseEntityId);
        if (entity instanceof AbstractHorse horse && horse.isValid()) {
            return horse;
        }
        return null;
    }
}
