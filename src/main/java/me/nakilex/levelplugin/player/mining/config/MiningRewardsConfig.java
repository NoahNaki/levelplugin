package me.nakilex.levelplugin.player.mining.config;

import me.nakilex.levelplugin.Main;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/** Loads config-backed rewards and progression requirements for kingdom-mine blocks. */
public class MiningRewardsConfig {

    private final Main plugin;
    private final File configFile;
    private FileConfiguration config;
    private final Map<Material, MiningBlockReward> rewards = new EnumMap<>(Material.class);

    public MiningRewardsConfig(Main plugin) {
        this.plugin = plugin;
        configFile = new File(plugin.getDataFolder(), "mining_rewards.yml");
        if (!configFile.exists()) {
            try {
                plugin.saveResource("mining_rewards.yml", true);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save mining_rewards.yml!", e);
            }
        }
        reloadConfig();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public MiningBlockReward getReward(Material blockMaterial) {
        return blockMaterial == null ? null : rewards.get(blockMaterial);
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        rewards.clear();
        ConfigurationSection root = config.getConfigurationSection("blocks");
        if (root == null) {
            FileConfiguration defaults = loadBundledDefaults();
            root = defaults != null ? defaults.getConfigurationSection("blocks") : null;
            plugin.getLogger().warning("mining_rewards.yml does not define kingdom-mine blocks; using bundled defaults until the file is regenerated.");
        }
        if (root == null) {
            plugin.getLogger().warning("mining_rewards.yml does not define any kingdom-mine blocks.");
            return;
        }

        for (String key : root.getKeys(false)) {
            ConfigurationSection node = root.getConfigurationSection(key);
            if (node == null) continue;
            Material blockMaterial = parseMaterial(key);
            if (blockMaterial == null) continue;

            Material replacementMaterial = parseOptionalMaterial(node.getString("replacement"), key + ".replacement");
            Material dropMaterial = parseOptionalMaterial(node.getString("drop.material"), key + ".drop.material");
            int dropMin = Math.max(0, node.getInt("drop.min", dropMaterial == null ? 0 : 1));
            int dropMax = Math.max(dropMin, node.getInt("drop.max", dropMin));
            int xp = Math.max(0, node.getInt("xp", 0));
            int level = Math.max(1, node.getInt("level", 1));
            String questOreId = node.getString("quest");
            rewards.put(blockMaterial, new MiningBlockReward(
                    blockMaterial, replacementMaterial, dropMaterial, dropMin, dropMax, xp, level, questOreId));
        }
    }

    private FileConfiguration loadBundledDefaults() {
        try (InputStream stream = plugin.getResource("mining_rewards.yml")) {
            if (stream == null) return null;
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load bundled mining_rewards.yml defaults!", e);
            return null;
        }
    }

    private Material parseMaterial(String value) {
        Material material = Material.matchMaterial(value.toUpperCase(Locale.ROOT));
        if (material == null) {
            plugin.getLogger().warning("Ignoring unknown kingdom-mine block material in mining_rewards.yml: " + value);
        }
        return material;
    }

    private Material parseOptionalMaterial(String value, String path) {
        if (value == null || value.isBlank()) return null;
        Material material = Material.matchMaterial(value.toUpperCase(Locale.ROOT));
        if (material == null) {
            plugin.getLogger().warning("Ignoring unknown material at blocks." + path + " in mining_rewards.yml: " + value);
        }
        return material;
    }

    public record MiningBlockReward(Material blockMaterial,
                                    Material replacementMaterial,
                                    Material dropMaterial,
                                    int dropMin,
                                    int dropMax,
                                    int xp,
                                    int levelRequirement,
                                    String questOreId) {
    }
}
