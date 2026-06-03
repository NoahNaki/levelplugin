package me.nakilex.levelplugin.player.woodcutting;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.woodcutting.drop.DropMode;
import me.nakilex.levelplugin.player.woodcutting.tree.TreeHeuristic;
import me.nakilex.levelplugin.player.woodcutting.tree.TreeType;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Level;

public class WoodcuttingConfig {
    public enum DirectionMode { FREE, EIGHT, FOUR;
        public static DirectionMode fromString(String raw) {
            if (raw == null) return EIGHT;
            try { return valueOf(raw.trim().toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException ex) { return EIGHT; }
        }
    }

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
        plugin.getLogger().info("[Woodcutting] Loading config from: " + file.getAbsolutePath());
        this.config = YamlConfiguration.loadConfiguration(file);
        applyBundledDefaults();
        this.tools = loadMaterials("woodcutting.break.tools");
        this.treeTypes = loadTreeTypes();
        logLoadedState();
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
    public int maxLogs() { return Math.max(minimumLogs(), config.getInt("woodcutting.detection.max-logs", 500)); }
    public int maxLeaves() { return Math.max(minimumLeaves(), config.getInt("woodcutting.detection.max-leaves", 2000)); }
    public boolean ignorePlayerPlacedWood() { return config.getBoolean("woodcutting.detection.ignore-player-placed-wood", true); }
    public boolean ignorePlayerPlacedLeaves() { return config.getBoolean("woodcutting.detection.ignore-player-placed-leaves", true); }
    public boolean allowMixedLeaves() { return config.getBoolean("woodcutting.detection.allow-mixed-leaves", true); }
    public boolean preventAirGapJumping() { return config.getBoolean("woodcutting.detection.prevent-air-gap-jumping", true); }
    public int leafSeedRadius() { return Math.max(1, config.getInt("woodcutting.detection.leaf-seed-radius", config.getInt("woodcutting.detection.mixed-leaf-radius", 4))); }
    public int mixedLeafRadius() { return leafSeedRadius(); }
    public int leafBoxExpansion() { return Math.max(0, config.getInt("woodcutting.detection.leaf-box-expansion", 8)); }
    public int leafConnectivityRadius() {
        int radius = config.getInt("woodcutting.detection.leaf-connectivity-radius", config.getInt("woodcutting.detection.canopy-expansion-radius", 1));
        return preventAirGapJumping() ? 1 : Math.max(1, radius);
    }
    public int canopyExpansionRadius() { return leafConnectivityRadius(); }
    public boolean animationEnabled() { return config.getBoolean("woodcutting.animation.enabled", true); }
    public double fallSpeed() { return Math.max(0.01D, config.getDouble("woodcutting.animation.fall-speed", 0.06D)); }
    public double initialAngleRadians() { return Math.toRadians(Math.max(0.1D, config.getDouble("woodcutting.animation.initial-angle-degrees", 3.0D))); }
    public double gravity() { return Math.max(0.0D, config.getDouble("woodcutting.animation.gravity", 9.81D)); }
    public double animationDeltaTime() { return Math.max(0.001D, config.getDouble("woodcutting.animation.delta-time", 0.05D)); }
    public long ticksPerFrame() { return Math.max(1L, config.getLong("woodcutting.animation.ticks-per-frame", 1L)); }
    public long lyingDelayTicks() { return Math.max(0L, config.getLong("woodcutting.animation.lying-delay-ticks", 20L)); }
    public boolean collisionEnabled() { return config.getBoolean("woodcutting.animation.collision.enabled", false); }
    public double collisionMinAngleDegrees() { return Math.max(0.0D, config.getDouble("woodcutting.animation.collision.min-angle-degrees", 15.0D)); }
    public double playerDamage() { return Math.max(0.0D, config.getDouble("woodcutting.animation.collision.player-damage", 6.0D)); }
    public double entityDamage() { return Math.max(0.0D, config.getDouble("woodcutting.animation.collision.entity-damage", 0.0D)); }
    public boolean collisionParticles() { return config.getBoolean("woodcutting.animation.collision.particles", true); }
    public DirectionMode directionMode() { return DirectionMode.fromString(config.getString("woodcutting.animation.direction-mode", "EIGHT")); }
    public boolean animationSoundsEnabled() { return config.getBoolean("woodcutting.animation.sounds.enabled", true); }
    public Sound animationStartSound() { return sound("woodcutting.animation.sounds.start", Sound.BLOCK_WOOD_BREAK); }
    public Sound animationFallingSound() { return sound("woodcutting.animation.sounds.falling", Sound.BLOCK_WOOD_STEP); }
    public Sound animationImpactSound() { return sound("woodcutting.animation.sounds.impact", Sound.BLOCK_WOOD_PLACE); }
    public double fallingSoundChance() { return clampChance(config.getDouble("woodcutting.animation.sounds.falling-chance", 0.12D)); }
    public float animationSoundVolume() { return (float) Math.max(0.0D, config.getDouble("woodcutting.animation.sounds.volume", 0.8D)); }
    public float animationSoundPitch() { return (float) Math.max(0.0D, config.getDouble("woodcutting.animation.sounds.pitch", 0.75D)); }
    public boolean animationParticlesEnabled() { return config.getBoolean("woodcutting.animation.particles.enabled", true); }
    public double fallingParticleChance() { return clampChance(config.getDouble("woodcutting.animation.particles.falling-chance", 0.08D)); }
    public int fallingParticleAmount() { return Math.max(0, config.getInt("woodcutting.animation.particles.amount", 4)); }
    public double fallingParticleSpread() { return Math.max(0.0D, config.getDouble("woodcutting.animation.particles.spread", 0.25D)); }
    public int impactParticleAmount() { return Math.max(0, config.getInt("woodcutting.animation.particles.impact-amount", 24)); }
    public boolean impactPlaceFallenBlocks() { return config.getBoolean("woodcutting.animation.impact.place-fallen-blocks", false); }
    public boolean impactDestroyLeavesOnImpact() { return config.getBoolean("woodcutting.animation.impact.destroy-leaves-on-impact", false); }
    public boolean impactDestroySoftBlocks() { return config.getBoolean("woodcutting.animation.impact.destroy-soft-blocks", false); }
    public DropMode dropMode() { return DropMode.fromString(config.getString("woodcutting.post-fall.drop-location", "LOCAL")); }
    public boolean autoReplantEnabled() { return config.getBoolean("woodcutting.post-fall.auto-replant.enabled", true); }
    public boolean replantLargeTrees() { return config.getBoolean("woodcutting.post-fall.auto-replant.large-trees", true); }
    public boolean protectSaplings() { return config.getBoolean("woodcutting.post-fall.auto-replant.protect-saplings", false); }
    public boolean exactRegrowEnabled() { return config.getBoolean("woodcutting.post-fall.exact-regrow.enabled", true); }
    public long exactRegrowDelayTicks() { return Math.max(0L, config.getLong("woodcutting.post-fall.exact-regrow.delay-ticks", 1200L)); }
    public boolean exactRegrowOnlyIfSpaceClear() { return config.getBoolean("woodcutting.post-fall.exact-regrow.only-if-space-clear", true); }
    public boolean exactRegrowReplaceSaplings() { return config.getBoolean("woodcutting.post-fall.exact-regrow.replace-saplings", true); }
    public boolean debug() { return config.getBoolean("woodcutting.debug", false); }
    public int defaultXpPerLog() { return Math.max(0, config.getInt("woodcutting.rewards.xp-per-log", 6)); }
    public int defaultXpPerLeaf() { return Math.max(0, config.getInt("woodcutting.rewards.xp-per-leaf", 0)); }
    public double xpMultiplier() { return Math.max(0.0D, config.getDouble("woodcutting.rewards.xp-multiplier", 1.0D)); }
    public int minimumXp() { return Math.max(0, config.getInt("woodcutting.rewards.minimum-xp", 8)); }
    public int maximumXpPerTree() { return Math.max(0, config.getInt("woodcutting.rewards.maximum-xp-per-tree", 400)); }
    public int levelRequired(TreeType type) { return Math.max(1, config.getInt(rewardTreePath(type, "level-required"), 1)); }
    public int xpPerLog(TreeType type) { return Math.max(0, config.getInt(rewardTreePath(type, "xp-per-log"), defaultXpPerLog())); }
    public boolean allowCreative() { return config.getBoolean("woodcutting.break.allow-creative", false); }
    public boolean canChopIn(GameMode gameMode) { return gameMode != GameMode.CREATIVE || allowCreative(); }
    public Set<Material> tools() { return tools; }
    public Map<String, TreeType> treeTypes() { return treeTypes; }
    public boolean isConfiguredTool(Material material) { return tools.contains(material); }
    public String toolNames() { return tools.stream().map(Material::name).sorted().collect(Collectors.joining(", ")); }
    public String treeTypeNames() { return treeTypes.keySet().stream().sorted().collect(Collectors.joining(", ")); }

    private void applyBundledDefaults() {
        try (InputStream stream = plugin.getResource("woodcutting.yml")) {
            if (stream == null) {
                plugin.getLogger().warning("[Woodcutting] Bundled woodcutting.yml resource is missing.");
                return;
            }
            FileConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
            config.setDefaults(defaults);
            config.options().copyDefaults(true);
            config.save(file);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "[Woodcutting] Failed to apply bundled woodcutting.yml defaults", ex);
        }
    }

    private void logLoadedState() {
        plugin.getLogger().info("[Woodcutting] Has woodcutting: " + config.isConfigurationSection("woodcutting"));
        plugin.getLogger().info("[Woodcutting] Has woodcutting.tree-types: " + config.isConfigurationSection("woodcutting.tree-types"));
        plugin.getLogger().info("[Woodcutting] Raw OAK logs: " + rawStringList("woodcutting.tree-types.OAK.logs"));
        plugin.getLogger().info("[Woodcutting] Raw tools: " + rawStringList("woodcutting.break.tools"));
        plugin.getLogger().info("[Woodcutting] Enabled: " + enabled());
        plugin.getLogger().info("[Woodcutting] Loaded tools: " + tools.size() + (tools.isEmpty() ? "" : " " + toolNames()));
        plugin.getLogger().info("[Woodcutting] Loaded tree types: " + treeTypes.size() + (treeTypes.isEmpty() ? "" : " " + treeTypeNames()));
        plugin.getLogger().info("[Woodcutting] OAK_LOG recognized: " + treeTypes.values().stream().anyMatch(type -> type.isLog(Material.OAK_LOG)));
        plugin.getLogger().info("[Woodcutting] DIAMOND_AXE recognized: " + tools.contains(Material.DIAMOND_AXE));
        plugin.getLogger().info("[Woodcutting] Rewards loaded: " + config.isConfigurationSection("woodcutting.rewards"));
        int configuredConnectivityRadius = config.getInt("woodcutting.detection.leaf-connectivity-radius", config.getInt("woodcutting.detection.canopy-expansion-radius", 1));
        if (configuredConnectivityRadius > 1) {
            String suffix = preventAirGapJumping() ? " It will be clamped to 1 because prevent-air-gap-jumping is enabled." : " This can merge nearby separate trees across air gaps.";
            plugin.getLogger().warning("[Woodcutting] leaf-connectivity-radius/canopy-expansion-radius is " + configuredConnectivityRadius + "." + suffix);
        }
    }

    public void debugLog(String message) {
        if (debug()) plugin.getLogger().info(message);
    }

    private List<String> rawStringList(String path) {
        return config.getStringList(path);
    }

    private Map<String, TreeType> loadTreeTypes() {
        Map<String, TreeType> loaded = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection("woodcutting.tree-types");
        if (section == null) return loaded;
        for (String key : section.getKeys(false)) {
            String path = "woodcutting.tree-types." + key + ".";
            Set<Material> logs = loadMaterials(path + "logs");
            Set<Material> leaves = loadMaterials(path + "leaves");
            Set<Material> attachedBlocks = loadMaterials(path + "attached-blocks");
            for (String group : config.getStringList(path + "leaf-groups")) {
                leaves.addAll(loadMaterials("woodcutting.leaf-groups." + group));
            }
            moveKnownAttachedBlocksOutOfLeaves(leaves, attachedBlocks);
            Material sapling = material(config.getString(path + "sapling"), Material.OAK_SAPLING);
            TreeHeuristic heuristic = new TreeHeuristic(config.getInt(path + "diameter", 4), config.getInt(path + "height", 12));
            if (!logs.isEmpty()) loaded.put(key.toUpperCase(Locale.ROOT), new TreeType(key.toUpperCase(Locale.ROOT), logs, leaves, attachedBlocks, sapling, heuristic));
        }
        return loaded;
    }

    private void moveKnownAttachedBlocksOutOfLeaves(Set<Material> leaves, Set<Material> attachedBlocks) {
        Set<Material> knownAttached = EnumSet.noneOf(Material.class);
        for (String materialName : List.of(
                "BEE_NEST", "VINE", "COCOA", "MOSS_CARPET", "PALE_MOSS_CARPET",
                "HANGING_ROOTS", "WEEPING_VINES", "WEEPING_VINES_PLANT", "TWISTING_VINES",
                "TWISTING_VINES_PLANT", "CAVE_VINES", "CAVE_VINES_PLANT", "GLOW_LICHEN"
        )) {
            Material material = material(materialName, null);
            if (material != null) knownAttached.add(material);
        }
        for (Material material : Set.copyOf(leaves)) {
            if (knownAttached.contains(material)) {
                leaves.remove(material);
                attachedBlocks.add(material);
            }
        }
    }

    private String rewardTreePath(TreeType type, String child) {
        String key = type == null ? "" : type.key();
        return "woodcutting.rewards.tree-types." + key + "." + child;
    }

    private double clampChance(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private Sound sound(String path, Sound fallback) {
        String raw = config.getString(path);
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Sound.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
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
