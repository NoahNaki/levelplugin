package me.nakilex.levelplugin.cooking.config;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cooking.minigame.CookingMiniGameType;
import me.nakilex.levelplugin.cooking.model.CookingIngredientRequirement;
import me.nakilex.levelplugin.cooking.model.CookingRecipe;
import me.nakilex.levelplugin.cooking.model.CookingReward;
import me.nakilex.levelplugin.cooking.model.CookingStage;
import me.nakilex.levelplugin.cooking.model.CookingStageType;
import me.nakilex.levelplugin.cooking.model.CookingWorkstationType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/** Loads LevelPlugin-owned cooking definitions from cooking.yml. */
public class CookingConfigLoader {
    private static final String FILE_NAME = "cooking.yml";

    private final Main plugin;
    private final File configFile;

    public CookingConfigLoader(Main plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), FILE_NAME);
    }

    public CookingConfigData load() {
        ensureConfigExists();
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        return new CookingConfigData(loadRecipes(config), loadWorkstations(config));
    }

    private void ensureConfigExists() {
        if (configFile.exists()) {
            plugin.saveResource(FILE_NAME, true);
            plugin.getLogger().info("[Cooking] Replaced cooking.yml from bundled resource.");
            return;
        }
        plugin.saveResource(FILE_NAME, false);
    }

    private List<CookingRecipe> loadRecipes(FileConfiguration config) {
        List<CookingRecipe> recipes = new ArrayList<>();
        ConfigurationSection root = config.getConfigurationSection("recipes");
        if (root == null) {
            warn("cooking.yml has no recipes section.");
            return recipes;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;
            Material display = parseMaterial(section.getString("display-material", section.getString("display-item", "PAPER")), "recipes." + id + ".display-material");
            if (display == null) continue;
            List<CookingStage> stages = loadStages(id, section.getConfigurationSection("stages"));
            if (stages.isEmpty()) {
                warn("Skipping cooking recipe '" + id + "' because it has no valid stages.");
                continue;
            }
            List<CookingReward> rewards = loadRewards(id, section.getConfigurationSection("rewards"));
            if (rewards.isEmpty()) {
                warn("Skipping cooking recipe '" + id + "' because it has no valid rewards.");
                continue;
            }
            String displayName = section.getString("display-name", section.getString("name", id));
            List<String> lore = parseLore(section);
            recipes.add(new CookingRecipe(id, displayName, display, lore, stages, rewards));
        }
        return recipes;
    }

    private List<String> parseLore(ConfigurationSection section) {
        List<String> lore = new ArrayList<>(section.getStringList("lore"));
        String description = section.getString("description");
        if (description != null && !description.isBlank()) {
            lore.add(0, description);
        }
        return lore;
    }

    private List<CookingStage> loadStages(String recipeId, ConfigurationSection root) {
        List<CookingStage> stages = new ArrayList<>();
        if (root == null) return stages;
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) continue;
            CookingStageType type = parseStageType(section.getString("type"), "recipes." + recipeId + ".stages." + key + ".type");
            if (type == null) continue;
            List<CookingIngredientRequirement> requirements = type == CookingStageType.INSERT_ITEM
                    ? loadIngredientRequirements(recipeId, key, section)
                    : List.of();
            if (type == CookingStageType.INSERT_ITEM && requirements.isEmpty()) continue;
            CookingMiniGameType miniGameType = type == CookingStageType.MINI_GAME
                    ? parseMiniGameType(section.getString("mini-game-type", section.getString("miniGameType")), "recipes." + recipeId + ".stages." + key + ".mini-game-type")
                    : null;
            if (type == CookingStageType.MINI_GAME && miniGameType == null) continue;
            stages.add(new CookingStage(
                    type,
                    requirements,
                    section.getLong("duration-ticks", section.getLong("durationTicks", section.getLong("ticks", 0L))),
                    section.getString("minigame-id", section.getString("mini-game-id")),
                    section.getString("tooltip"),
                    miniGameType,
                    section.getLong("hit-window-ticks", section.getLong("hitWindowTicks",
                            section.getLong("target-window-ticks", section.getLong("targetWindowTicks", 0L)))),
                    section.getInt("required-clicks", section.getInt("requiredClicks", 0)),
                    section.getInt("bar-size", section.getInt("barSize", 0)),
                    section.getInt("target-score", section.getInt("targetScore", 0)),
                    section.getInt("health", 0),
                    section.getLong("speed-ticks", section.getLong("speedTicks", 0L))
            ));
        }
        return stages;
    }

    private CookingMiniGameType parseMiniGameType(String raw, String path) {
        if (raw == null || raw.isBlank()) {
            warn("Missing cooking mini-game type at " + path + ".");
            return null;
        }
        try {
            return CookingMiniGameType.valueOf(raw.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            warn("Invalid cooking mini-game type '" + raw + "' at " + path + ".");
            return null;
        }
    }

    private List<CookingIngredientRequirement> loadIngredientRequirements(String recipeId, String stageKey, ConfigurationSection stageSection) {
        List<CookingIngredientRequirement> requirements = new ArrayList<>();
        String basePath = "recipes." + recipeId + ".stages." + stageKey;
        if (stageSection.isList("requirements")) {
            int index = 0;
            for (java.util.Map<?, ?> rawRequirement : stageSection.getMapList("requirements")) {
                index++;
                Object materialValue = rawRequirement.get("material");
                if (materialValue == null) {
                    materialValue = rawRequirement.get("item-material");
                }
                Material material = parseMaterial(stringValue(materialValue), basePath + ".requirements." + index + ".material");
                if (material == null) {
                    continue;
                }
                String nexoItemId = stringValue(rawRequirement.get("nexo-item-id"));
                if (nexoItemId == null) {
                    nexoItemId = stringValue(rawRequirement.get("nexo-id"));
                }
                int amount = intValue(rawRequirement.get("amount"), 1);
                if (amount <= 0) {
                    warn("Skipping ingredient requirement at " + basePath + ".requirements." + index + " because amount must be positive.");
                    continue;
                }
                requirements.add(new CookingIngredientRequirement(material, nexoItemId, amount));
            }
            return requirements;
        }
        ConfigurationSection requirementSection = stageSection.getConfigurationSection("requirements");
        if (requirementSection != null) {
            for (String requirementKey : requirementSection.getKeys(false)) {
                ConfigurationSection section = requirementSection.getConfigurationSection(requirementKey);
                if (section == null) {
                    warn("Skipping invalid ingredient requirement at " + basePath + ".requirements." + requirementKey + ".");
                    continue;
                }
                Material material = parseMaterial(section.getString("material", section.getString("item-material")), basePath + ".requirements." + requirementKey + ".material");
                if (material == null) {
                    continue;
                }
                int amount = section.getInt("amount", 1);
                if (amount <= 0) {
                    warn("Skipping ingredient requirement at " + basePath + ".requirements." + requirementKey + " because amount must be positive.");
                    continue;
                }
                requirements.add(new CookingIngredientRequirement(material, section.getString("nexo-item-id", section.getString("nexo-id")), amount));
            }
            return requirements;
        }
        Material legacyItem = parseMaterial(stageSection.getString("material", stageSection.getString("item-material")), basePath + ".material");
        if (legacyItem == null) {
            return requirements;
        }
        int legacyAmount = stageSection.getInt("amount", 1);
        if (legacyAmount <= 0) {
            warn("Skipping legacy ingredient requirement at " + basePath + " because amount must be positive.");
            return requirements;
        }
        requirements.add(new CookingIngredientRequirement(
                legacyItem,
                stageSection.getString("nexo-item-id", stageSection.getString("nexo-id")),
                legacyAmount
        ));
        return requirements;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String string = String.valueOf(value);
        return string.isBlank() ? null : string;
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private List<CookingReward> loadRewards(String recipeId, ConfigurationSection root) {
        List<CookingReward> rewards = new ArrayList<>();
        if (root == null) {
            warn("Recipe '" + recipeId + "' has no rewards section in cooking.yml.");
            return rewards;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                warn("Skipping invalid reward entry at recipes." + recipeId + ".rewards." + key + ".");
                continue;
            }
            Material material = parseMaterial(section.getString("material"), "recipes." + recipeId + ".rewards." + key + ".material");
            if (material == null) {
                continue;
            }
            int amount = section.getInt("amount", 1);
            if (amount <= 0) {
                warn("Skipping reward at recipes." + recipeId + ".rewards." + key + " because amount must be positive.");
                continue;
            }
            rewards.add(new CookingReward(material, amount));
        }
        return rewards;
    }

    private List<CookingWorkstationType> loadWorkstations(FileConfiguration config) {
        List<CookingWorkstationType> workstations = new ArrayList<>();
        ConfigurationSection root = config.getConfigurationSection("workstations");
        if (root == null) {
            warn("cooking.yml has no workstations section.");
            return workstations;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;
            Material block = parseMaterial(section.getString("block-material"), "workstations." + id + ".block-material");
            Material item = parseMaterial(section.getString("item-material", section.getString("block-material")), "workstations." + id + ".item-material");
            List<String> recipeIds = section.getStringList("recipes");
            if (block == null || item == null || recipeIds.isEmpty()) {
                warn("Skipping cooking workstation '" + id + "' because block, item, or recipes are invalid.");
                continue;
            }
            workstations.add(new CookingWorkstationType(
                    id,
                    block,
                    item,
                    section.getString("nexo-item-id", section.getString("nexo-id")),
                    recipeIds,
                    section.getString("permission")
            ));
        }
        return workstations;
    }

    private Material parseMaterial(String raw, String path) {
        if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("null")) {
            warn("Missing material at " + path + " in cooking.yml.");
            return null;
        }
        Material material = Material.matchMaterial(raw.toUpperCase(Locale.ROOT));
        if (material == null) {
            warn("Unknown material at " + path + " in cooking.yml: " + raw);
        }
        return material;
    }

    private CookingStageType parseStageType(String raw, String path) {
        if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("null")) {
            warn("Missing stage type at " + path + " in cooking.yml.");
            return null;
        }
        try {
            return CookingStageType.valueOf(raw.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            warn("Unknown stage type at " + path + " in cooking.yml: " + raw);
            return null;
        }
    }

    private void warn(String message) {
        Logger logger = plugin.getLogger();
        logger.warning("[Cooking] " + message);
    }

    public record CookingConfigData(List<CookingRecipe> recipes, List<CookingWorkstationType> workstations) {}
}
