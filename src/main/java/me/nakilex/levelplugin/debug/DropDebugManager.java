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

    public DropDebugManager(Main main) {
        this.main = main;
        FileConfiguration cfg = main.getCustomConfig();
        forceMobDrops = cfg != null && cfg.getBoolean("debug.force-mob-drops", false);
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
}
