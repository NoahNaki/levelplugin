package me.nakilex.levelplugin.player.fishing.config;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.player.fishing.data.FishDefinition;
import me.nakilex.levelplugin.utils.RandomUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

/**
 * Loads fishing_rewards.yml which defines fish pools, rarity, XP, and value data.
 */
public class FishingRewardsConfig {

    private final File configFile;
    private FileConfiguration config;
    private final List<FishDefinition> fish = new ArrayList<>();

    public FishingRewardsConfig(Main plugin) {
        configFile = new File(plugin.getDataFolder(), "fishing_rewards.yml");
        if (!configFile.exists()) {
            try {
                plugin.saveResource("fishing_rewards.yml", true);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save fishing_rewards.yml!", e);
            }
        }
        reloadConfig();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public List<FishDefinition> getFish() {
        return new ArrayList<>(fish);
    }

    public FishDefinition rollFish(int fishingLevel, boolean inLava, boolean hasHighestTier, double rarityBonus, Random random) {
        List<FishDefinition> available = new ArrayList<>();
        for (FishDefinition def : fish) {
            if (def.minLevel() > fishingLevel) continue;
            if (def.requiresLava() && !inLava) continue;
            if (def.requiresHighestTier() && !hasHighestTier) continue;
            available.add(def);
        }

        if (available.isEmpty()) {
            available = fish;
        }

        Map<FishDefinition, Double> weights = new LinkedHashMap<>();
        for (FishDefinition def : available) {
            weights.put(def, applyRarityBonus(def, rarityBonus, hasHighestTier));
        }
        return RandomUtil.pickWeighted(random, weights);
    }


    private double applyRarityBonus(FishDefinition definition, double rarityBonus, boolean hasHighestTier) {
        double safeBonus = Math.max(0.0, rarityBonus);
        if (safeBonus <= 0.0) {
            return Math.max(0.01, definition.weight());
        }

        double rarityModifier = switch (definition.rarity()) {
            case COMMON -> Math.max(0.40, 1.0 - (safeBonus * 0.70));
            case UNCOMMON -> Math.max(0.55, 1.0 - (safeBonus * 0.35));
            case RARE -> 1.0 + (safeBonus * 0.45);
            case EPIC -> 1.0 + (safeBonus * 0.70);
            case LEGENDARY -> 1.0 + (safeBonus * 1.00);
            case MYTHIC, FABLED -> 1.0 + (safeBonus * 1.20);
        };

        if (!hasHighestTier && (definition.rarity() == ItemRarity.LEGENDARY || definition.rarity() == ItemRarity.MYTHIC || definition.rarity() == ItemRarity.FABLED)) {
            rarityModifier *= 0.85;
        }

        return Math.max(0.01, definition.weight() * rarityModifier);
    }
    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        fish.clear();
        ConfigurationSection root = config.getConfigurationSection("fish");
        if (root == null) {
            return;
        }

        for (String key : root.getKeys(false)) {
            ConfigurationSection node = root.getConfigurationSection(key);
            if (node == null) continue;
            String display = node.getString("display", key);
            int minSize = node.getInt("min_size", 10);
            int maxSize = Math.max(minSize, node.getInt("max_size", minSize));
            String rarityName = node.getString("rarity", "COMMON");
            ItemRarity rarity;
            try {
                rarity = ItemRarity.valueOf(rarityName.toUpperCase());
            } catch (IllegalArgumentException ex) {
                rarity = ItemRarity.COMMON;
            }
            int xp = node.getInt("xp", 5);
            int value = node.getInt("value", 10);
            double weight = node.getDouble("weight", 1.0);
            int minLevel = node.getInt("min_level", 1);
            boolean requiresLava = node.getBoolean("requires_lava", false);
            boolean requiresHighestTier = node.getBoolean("requires_highest_tier", false);
            String baseNexoId = node.getString("nexo_base");
            String silverNexoId = node.getString("nexo_silver");
            String goldNexoId = node.getString("nexo_gold");
            fish.add(new FishDefinition(key, display, minSize, maxSize, rarity, xp, value, weight,
                    minLevel, requiresLava, requiresHighestTier, baseNexoId, silverNexoId, goldNexoId));
        }
    }
}
