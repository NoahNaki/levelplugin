package me.nakilex.levelplugin.player.fishing.minigame;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** YAML-backed fishing challenge settings. */
public final class FishingMinigameSettings {
    private FishingMinigameSettings() { }

    public record AccurateClick(double pointerSpeed, double targetWidth, double minTarget, double maxTarget,
                                double durationMultiplier, long minimumDurationMs, FishingGaugeSettings gauge) {
        public AccurateClick {
            pointerSpeed = Math.max(0.001, pointerSpeed);
            targetWidth = Math.max(0.01, Math.min(1.0, targetWidth));
            minTarget = Math.max(0.0, Math.min(0.99, minTarget));
            maxTarget = Math.max(minTarget + 0.01, Math.min(1.0, maxTarget));
            durationMultiplier = Math.max(0.1, durationMultiplier);
            minimumDurationMs = Math.max(500L, minimumDurationMs);
        }
    }
    public record Hold(double waterResistance, double pullingStrength, double maxVelocity, double targetWidth,
                       double requiredProgress, double progressGain, double progressLoss, boolean clickControl,
                       double durationMultiplier, long minimumDurationMs, FishingGaugeSettings gauge) {
        public Hold {
            waterResistance = Math.max(0.0, waterResistance);
            pullingStrength = Math.max(0.0, pullingStrength);
            maxVelocity = Math.max(0.001, maxVelocity);
            targetWidth = Math.max(0.01, Math.min(1.0, targetWidth));
            requiredProgress = Math.max(0.01, requiredProgress);
            progressGain = Math.max(0.001, progressGain);
            progressLoss = Math.max(0.0, progressLoss);
            durationMultiplier = Math.max(0.1, durationMultiplier);
            minimumDurationMs = Math.max(500L, minimumDurationMs);
        }
    }
    public record Click(double requiredProgress, double progressPerClick, double decayPerTick,
                        double durationMultiplier, long minimumDurationMs) {
        public Click {
            requiredProgress = Math.max(1.0, requiredProgress);
            progressPerClick = Math.max(0.01, progressPerClick);
            decayPerTick = Math.max(0.0, decayPerTick);
            durationMultiplier = Math.max(0.1, durationMultiplier);
            minimumDurationMs = Math.max(500L, minimumDurationMs);
        }
    }
    public record Dance(int sequenceLength, double durationMultiplier, long minimumDurationMs) {
        public Dance {
            sequenceLength = Math.max(1, sequenceLength);
            durationMultiplier = Math.max(0.1, durationMultiplier);
            minimumDurationMs = Math.max(500L, minimumDurationMs);
        }
    }
    public record Tension(double safeMin, double safeMax, double increasePerTick, double decreasePerTick,
                          double requiredProgress, double progressGain, double progressLoss,
                          double durationMultiplier, long minimumDurationMs, FishingGaugeSettings gauge) {
        public Tension {
            safeMin = Math.max(0.0, Math.min(0.99, safeMin));
            safeMax = Math.max(safeMin + 0.01, Math.min(1.0, safeMax));
            increasePerTick = Math.max(0.0, increasePerTick);
            decreasePerTick = Math.max(0.0, decreasePerTick);
            requiredProgress = Math.max(0.01, requiredProgress);
            progressGain = Math.max(0.001, progressGain);
            progressLoss = Math.max(0.0, progressLoss);
            durationMultiplier = Math.max(0.1, durationMultiplier);
            minimumDurationMs = Math.max(500L, minimumDurationMs);
        }
    }

    public static AccurateClick accurateClick(FileConfiguration config) { return accurateClick(config, "accurate_click"); }
    public static AccurateClick accurateClick(FileConfiguration config, String id) {
        ConfigurationSection section = config.getConfigurationSection("minigames." + id);
        return new AccurateClick(number(section, "pointer_speed", 0.055), number(section, "target_width", 0.22),
                number(section, "min_target", 0.20), number(section, "max_target", 0.80),
                number(section, "duration_multiplier", 3.0), integer(section, "minimum_duration_ms", 4_000L),
                FishingGaugeSettings.from(section == null ? null : section.getConfigurationSection("gauge")));
    }

    public static Hold hold(FileConfiguration config) { return hold(config, "hold"); }
    public static Hold hold(FileConfiguration config, String id) {
        ConfigurationSection section = config.getConfigurationSection("minigames." + id);
        return new Hold(number(section, "water_resistance", 0.014), number(section, "pulling_strength", 0.028),
                number(section, "max_velocity", 0.09), number(section, "target_width", 0.30),
                number(section, "required_progress", 1.0), number(section, "progress_gain", 0.018),
                number(section, "progress_loss", 0.012), section != null && section.getBoolean("click_control", false),
                number(section, "duration_multiplier", 5.0),
                integer(section, "minimum_duration_ms", 7_000L),
                FishingGaugeSettings.from(section == null ? null : section.getConfigurationSection("gauge")));
    }

    public static Click click(FileConfiguration config) { return click(config, "click"); }
    public static Click click(FileConfiguration config, String id) {
        ConfigurationSection section = config.getConfigurationSection("minigames." + id);
        return new Click(number(section, "required_progress", integer(section, "required_clicks", 12L)),
                number(section, "progress_per_click", 1.0), number(section, "decay_per_tick", 0.0),
                number(section, "duration_multiplier", 2.5), integer(section, "minimum_duration_ms", 4_000L));
    }

    public static Dance dance(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("minigames.dance");
        return new Dance((int) integer(section, "sequence_length", 6L), number(section, "duration_multiplier", 4.0),
                integer(section, "minimum_duration_ms", 6_000L));
    }

    public static Tension tension(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("minigames.tension");
        return new Tension(number(section, "safe_min", 0.35), number(section, "safe_max", 0.70),
                number(section, "increase_per_tick", 0.025), number(section, "decrease_per_tick", 0.016),
                number(section, "required_progress", 1.0), number(section, "progress_gain", 0.014),
                number(section, "progress_loss", 0.018), number(section, "duration_multiplier", 5.0),
                integer(section, "minimum_duration_ms", 7_000L),
                FishingGaugeSettings.from(section == null ? null : section.getConfigurationSection("gauge")));
    }

    private static double number(ConfigurationSection section, String path, double fallback) {
        return section == null ? fallback : section.getDouble(path, fallback);
    }

    private static long integer(ConfigurationSection section, String path, long fallback) {
        return section == null ? fallback : section.getLong(path, fallback);
    }
}
