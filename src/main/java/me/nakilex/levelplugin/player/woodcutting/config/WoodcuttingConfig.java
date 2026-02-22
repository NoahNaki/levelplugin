package me.nakilex.levelplugin.player.woodcutting.config;

import me.nakilex.levelplugin.Main;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

/** Loads woodcutting node behavior from woodcutting.yml. */
public class WoodcuttingConfig {
    private final File file;
    private FileConfiguration config;

    public WoodcuttingConfig(Main plugin) {
        this.file = new File(plugin.getDataFolder(), "woodcutting.yml");
        if (!file.exists()) {
            try {
                plugin.saveResource("woodcutting.yml", true);
            } catch (Exception ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save woodcutting.yml", ex);
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public int getBaseXp() {
        return Math.max(0, config.getInt("xp.base", 12));
    }

    public int getRespawnSeconds() {
        return Math.max(1, config.getInt("nodes.respawn-seconds", 90));
    }

    public int getNodeDurability(String nodeId) {
        int fallback = Math.max(1, config.getInt("nodes.durability.default", 10));
        if (nodeId == null || nodeId.isBlank()) {
            return fallback;
        }
        return Math.max(1, config.getInt("nodes.durability.overrides." + nodeId.toLowerCase(Locale.ROOT), fallback));
    }

    public int getHpBarSegments() {
        return Math.max(5, config.getInt("nodes.hp-bar.segments", 10));
    }

    public Set<String> getNodeIds() {
        Set<String> set = new HashSet<>();
        for (String id : config.getStringList("nodes.ids")) {
            if (id != null && !id.isBlank()) {
                set.add(id.toLowerCase(Locale.ROOT));
            }
        }
        return set;
    }

    public Material getDropMaterial() {
        String raw = config.getString("drops.material", "OAK_LOG");
        try {
            return Material.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return Material.OAK_LOG;
        }
    }

    public int getDropAmountMin() {
        return Math.max(1, config.getInt("drops.amount.min", 1));
    }

    public int getDropAmountMax() {
        return Math.max(getDropAmountMin(), config.getInt("drops.amount.max", getDropAmountMin()));
    }
}
