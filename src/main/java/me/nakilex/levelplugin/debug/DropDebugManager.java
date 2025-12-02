package me.nakilex.levelplugin.debug;

import me.nakilex.levelplugin.Main;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Stores transient debug flags related to loot drops and persists them in the
 * custom config so they survive reloads.
 */
public class DropDebugManager {
    private final Main main;
    private boolean forceMobDrops;
    private double globalGearDropRate;

    public DropDebugManager(Main main) {
        this.main = main;
        FileConfiguration cfg = main.getCustomConfig();
        forceMobDrops = cfg != null && cfg.getBoolean("debug.force-mob-drops", false);
        double configuredDropRate = cfg != null ? cfg.getDouble("debug.mob-gear-drop-rate", 8.0) : 8.0;
        globalGearDropRate = clampChance(configuredDropRate);
    }

    public boolean isForceMobDrops() {
        return forceMobDrops;
    }

    public boolean toggleForceMobDrops() {
        setForceMobDrops(!forceMobDrops);
        return forceMobDrops;
    }

    public void setForceMobDrops(boolean enabled) {
        this.forceMobDrops = enabled;
        FileConfiguration cfg = main.getCustomConfig();
        if (cfg != null) {
            cfg.set("debug.force-mob-drops", enabled);
            main.saveConfig();
        }
    }

    public double getGlobalGearDropRate() {
        return globalGearDropRate;
    }

    public void setGlobalGearDropRate(double chancePercent) {
        globalGearDropRate = Math.max(0.0, Math.min(100.0, chancePercent));
        FileConfiguration cfg = main.getCustomConfig();
        if (cfg != null) {
            cfg.set("debug.mob-gear-drop-rate", globalGearDropRate);
            main.saveConfig();
        }
    }

    public double resolveDropChance(org.bukkit.configuration.ConfigurationSection mobConfig) {
        if (mobConfig != null) {
            if (mobConfig.contains("drop_override")) {
                return clampChance(mobConfig.getDouble("drop_override"));
            }
            if (mobConfig.contains("tier_chance")) {
                return clampChance(mobConfig.getDouble("tier_chance"));
            }
        }
        return globalGearDropRate;
    }

    private double clampChance(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }
}
