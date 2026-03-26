package me.nakilex.levelplugin.horse.managers;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
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

    private static final List<String> PRESET_OPTIONS = List.of(
            OFF_PRESET,
            "FLAME",
            "HEART",
            "HAPPY_VILLAGER",
            "CRIT",
            "ENCHANT",
            "END_ROD"
    );
    private static final Map<String, UnlockRequirement> UNLOCK_REQUIREMENTS = Map.of(
            OFF_PRESET, new UnlockRequirement(0.0, 0),
            "FLAME", new UnlockRequirement(0.0, 0),
            "HEART", new UnlockRequirement(250.0, 0),
            "HAPPY_VILLAGER", new UnlockRequirement(600.0, 0),
            "CRIT", new UnlockRequirement(0.0, 20),
            "ENCHANT", new UnlockRequirement(1200.0, 40),
            "END_ROD", new UnlockRequirement(2200.0, 80)
    );

    private final Map<UUID, BukkitTask> trailTasks = new java.util.HashMap<>();

    public List<String> getPresetOptions() {
        return PRESET_OPTIONS;
    }

    public String normalizePreset(String presetName) {
        if (presetName == null || presetName.isBlank()) {
            return OFF_PRESET;
        }
        String normalized = presetName.toUpperCase(Locale.ROOT);
        for (String option : PRESET_OPTIONS) {
            if (option.equalsIgnoreCase(normalized)) {
                return option;
            }
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

    public boolean isUnlocked(String preset, double riddenMeters, int jumpCount) {
        String normalized = normalizePreset(preset);
        UnlockRequirement requirement = UNLOCK_REQUIREMENTS.getOrDefault(normalized, new UnlockRequirement(0.0, 0));
        return riddenMeters >= requirement.requiredMeters && jumpCount >= requirement.requiredJumps;
    }

    public String formatUnlockRequirement(String preset) {
        String normalized = normalizePreset(preset);
        UnlockRequirement requirement = UNLOCK_REQUIREMENTS.getOrDefault(normalized, new UnlockRequirement(0.0, 0));
        if (requirement.requiredMeters <= 0.0 && requirement.requiredJumps <= 0) {
            return "Unlocked by default";
        }
        if (requirement.requiredMeters > 0.0 && requirement.requiredJumps > 0) {
            return String.format("Ride %.0fm and land %d jumps", requirement.requiredMeters, requirement.requiredJumps);
        }
        if (requirement.requiredMeters > 0.0) {
            return String.format("Ride %.0fm", requirement.requiredMeters);
        }
        return String.format("Land %d jumps", requirement.requiredJumps);
    }

    private record UnlockRequirement(double requiredMeters, int requiredJumps) {
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

        Particle particle = parseParticle(normalized);
        if (particle == null) {
            return;
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(
                me.nakilex.levelplugin.Main.getInstance(),
                () -> {
                    if (!player.isOnline()
                            || !horse.isValid()
                            || !player.isInsideVehicle()
                            || player.getVehicle() == null
                            || !player.getVehicle().getUniqueId().equals(horse.getUniqueId())
                            || !ownerId.equals(player.getUniqueId())) {
                        stopTrail(ownerId);
                        return;
                    }
                    var loc = horse.getLocation().clone().add(0, 0.65, 0);
                    horse.getWorld().spawnParticle(particle, loc, 5, 0.14, 0.04, 0.14, 0.0);
                },
                0L,
                2L
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

    private Particle parseParticle(String normalized) {
        try {
            return Particle.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
