package me.nakilex.levelplugin.woodcutting;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.woodcutting.drop.DropMode;
import me.nakilex.levelplugin.woodcutting.tree.TreeHeuristic;
import me.nakilex.levelplugin.woodcutting.tree.TreeType;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class WoodcuttingConfig {
    public enum PoseMode { BOTH, CROUCH, STAND;
        public static PoseMode fromString(String raw) {
            if (raw == null) return BOTH;
            try { return valueOf(raw.trim().toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException ex) { return BOTH; }
        }
    }

    private final Main plugin;
    private final File file;
    private FileConfiguration config;
    private Map<String, TreeType> treeTypes = Map.of();
    private Set<Material> tools = EnumSet.noneOf(Material.class);

    public WoodcuttingConfig(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "woodcutting.yml");
        if (!file.exists()) {
            try { plugin.saveResource("woodcutting.yml", false); } catch (Exception ex) { plugin.getLogger().log(Level.SEVERE, "Failed to save woodcutting.yml", ex); }
        }
        reload();
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(file);
        this.tools = loadMaterials("woodcutting.break.tools");
        this.treeTypes = loadTreeTypes();
    }

    public boolean enabled() { return config.getBoolean("woodcutting.enabled", true) && config.getBoolean("woodcutting.enabled-by-default", true); }
    public PoseMode poseMode() { return PoseMode.fromString(config.getString("woodcutting.break.pose", "BOTH")); }
    public int maxHeightAboveRoot() { return Math.max(0, config.getInt("woodcutting.break.max-height-above-root", 3)); }
    public boolean axeDamageProportional() { return config.getBoolean("woodcutting.break.axe-damage.proportional", true); }
    public double axeDamageMultiplier() { return Math.max(0.0D, config.getDouble("woodcutting.break.axe-damage.multiplier", 1.0D)); }
    public boolean leaveAtOneDurability() { return config.getBoolean("woodcutting.break.axe-damage.leave-at-one-durability", true); }
    public boolean slowBreakEnabled() { return config.getBoolean("woodcutting.break.slow-break.enabled", false); }
    public int minimumLogs() { return Math.max(1, config.getInt("woodcutting.detection.minimum-logs", 4)); }
    public int minimumLeaves() { return Math.max(0, config.getInt("woodcutting.detection.minimum-leaves", 12)); }
    public int maxLogs() { return Math.max(minimumLogs(), config.getInt("woodcutting.detection.max-logs", 220)); }
    public int maxLeaves() { return Math.max(minimumLeaves(), config.getInt("woodcutting.detection.max-leaves", 500)); }
    public boolean ignorePlayerPlacedWood() { return config.getBoolean("woodcutting.detection.ignore-player-placed-wood", true); }
    public boolean ignorePlayerPlacedLeaves() { return config.getBoolean("woodcutting.detection.ignore-player-placed-leaves", true); }
    public boolean animationEnabled() { return config.getBoolean("woodcutting.animation.enabled", true); }
    public double fallSpeed() { return Math.max(0.01D, config.getDouble("woodcutting.animation.fall-speed", 0.06D)); }
    public long ticksPerFrame() { return Math.max(1L, config.getLong("woodcutting.animation.ticks-per-frame", 1L)); }
    public long lyingDelayTicks() { return Math.max(0L, config.getLong("woodcutting.animation.lying-delay-ticks", 20L)); }
    public boolean collisionEnabled() { return config.getBoolean("woodcutting.animation.collision.enabled", false); }
    public double collisionMinAngleDegrees() { return Math.max(0.0D, config.getDouble("woodcutting.animation.collision.min-angle-degrees", 15.0D)); }
    public double playerDamage() { return Math.max(0.0D, config.getDouble("woodcutting.animation.collision.player-damage", 6.0D)); }
    public double entityDamage() { return Math.max(0.0D, config.getDouble("woodcutting.animation.collision.entity-damage", 0.0D)); }
    public boolean collisionParticles() { return config.getBoolean("woodcutting.animation.collision.particles", true); }
    public DropMode dropMode() { return DropMode.fromString(config.getString("woodcutting.post-fall.drop-location", "LOCAL")); }
    public boolean autoReplantEnabled() { return config.getBoolean("woodcutting.post-fall.auto-replant.enabled", true); }
    public boolean replantLargeTrees() { return config.getBoolean("woodcutting.post-fall.auto-replant.large-trees", true); }
    public boolean protectSaplings() { return config.getBoolean("woodcutting.post-fall.auto-replant.protect-saplings", false); }
    public boolean allowCreative() { return config.getBoolean("woodcutting.break.allow-creative", false); }
    public boolean canChopIn(GameMode gameMode) { return gameMode != GameMode.CREATIVE || allowCreative(); }
    public Set<Material> tools() { return tools; }
    public Map<String, TreeType> treeTypes() { return treeTypes; }

    private Map<String, TreeType> loadTreeTypes() {
        Map<String, TreeType> loaded = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection("woodcutting.tree-types");
        if (section == null) return loaded;
        for (String key : section.getKeys(false)) {
            String path = "woodcutting.tree-types." + key + ".";
            Set<Material> logs = loadMaterials(path + "logs");
            Set<Material> leaves = loadMaterials(path + "leaves");
            Material sapling = material(config.getString(path + "sapling"), Material.OAK_SAPLING);
            TreeHeuristic heuristic = new TreeHeuristic(config.getInt(path + "diameter", 4), config.getInt(path + "height", 12));
            if (!logs.isEmpty()) loaded.put(key.toUpperCase(Locale.ROOT), new TreeType(key.toUpperCase(Locale.ROOT), logs, leaves, sapling, heuristic));
        }
        return loaded;
    }

    private Set<Material> loadMaterials(String path) {
        Set<Material> result = EnumSet.noneOf(Material.class);
        for (String raw : config.getStringList(path)) {
            Material material = material(raw, null);
            if (material != null) result.add(material);
        }
        return result;
    }

    private Material material(String raw, Material fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        Material matched = Material.matchMaterial(raw.trim().toUpperCase(Locale.ROOT));
        return matched == null ? fallback : matched;
    }
}
