package me.nakilex.levelplugin.cooking.config;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cooking.model.CookingRecipe;
import me.nakilex.levelplugin.cooking.model.CookingStage;
import me.nakilex.levelplugin.cooking.model.CookingStageType;
import me.nakilex.levelplugin.cooking.model.CookingWorkstationType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

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
        if (!configFile.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
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
            List<ItemStack> rewards = loadRewards(id, section.getConfigurationSection("rewards"));
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
            Material item = type == CookingStageType.INSERT_ITEM
                    ? parseMaterial(section.getString("material", section.getString("item-material")), "recipes." + recipeId + ".stages." + key + ".material")
                    : null;
            if (type == CookingStageType.INSERT_ITEM && item == null) continue;
            stages.add(new CookingStage(
                    type,
                    item,
                    section.getInt("amount", 1),
                    section.getLong("duration-ticks", section.getLong("ticks", 0L)),
                    section.getString("minigame-id", section.getString("mini-game-id"))
            ));
        }
        return stages;
    }

    private List<ItemStack> loadRewards(String recipeId, ConfigurationSection root) {
        List<ItemStack> rewards = new ArrayList<>();
        if (root == null) return rewards;
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) continue;
            Material material = parseMaterial(section.getString("material"), "recipes." + recipeId + ".rewards." + key + ".material");
            if (material == null) continue;
            rewards.add(new ItemStack(material, Math.max(1, section.getInt("amount", 1))));
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
            workstations.add(new CookingWorkstationType(id, block, item, recipeIds, section.getString("permission")));
        }
        return workstations;
    }

    private Material parseMaterial(String raw, String path) {
        if (raw == null || raw.isBlank()) {
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
        if (raw == null || raw.isBlank()) {
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
