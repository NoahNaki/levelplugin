package me.nakilex.levelplugin.fishing.core;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.fishing.api.FishingMechanism;
import me.nakilex.levelplugin.fishing.core.config.ConfiguredAction;
import me.nakilex.levelplugin.fishing.core.config.ConfiguredCondition;
import me.nakilex.levelplugin.fishing.core.game.GameDefinition;
import me.nakilex.levelplugin.fishing.core.loot.LootEntry;
import me.nakilex.levelplugin.fishing.core.registry.MechanismRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FishingConfigManager {
    private final Main plugin;
    private final MechanismRegistry mechanismRegistry;
    private final File lootFile;
    private final File gameFile;
    private final File feedbackFile;
    private final Map<FishingMechanism, List<LootEntry>> lootPools = new EnumMap<>(FishingMechanism.class);
    private final Map<String, GameDefinition> gameDefinitions = new HashMap<>();

    private List<FishingMechanism> enabledMechanisms = List.of(FishingMechanism.WATER);
    private String defaultGameId = "timing_bar_default";
    private boolean debug;
    private String languageKey = "en";
    private int voidCheckMinY = -64;
    private final Map<FishingMechanism, me.nakilex.levelplugin.fishing.core.feedback.FishingTheme> themes = new EnumMap<>(FishingMechanism.class);
    private me.nakilex.levelplugin.fishing.core.feedback.FeedbackPreset bitePreset;
    private me.nakilex.levelplugin.fishing.core.feedback.FeedbackPreset hookedPreset;
    private me.nakilex.levelplugin.fishing.core.feedback.FeedbackPreset successPreset;
    private me.nakilex.levelplugin.fishing.core.feedback.FeedbackPreset failPreset;
    private me.nakilex.levelplugin.fishing.core.feedback.FeedbackPreset linePreset;
    private me.nakilex.levelplugin.fishing.core.feedback.FeedbackPreset windowPreset;

    public FishingConfigManager(Main plugin, MechanismRegistry mechanismRegistry) {
        this.plugin = plugin;
        this.mechanismRegistry = mechanismRegistry;
        File dataFolder = new File(plugin.getDataFolder(), "fishing");
        File lootFolder = new File(dataFolder, "loots");
        File gameFolder = new File(dataFolder, "games");
        File feedbackFolder = new File(dataFolder, "feedback");
        lootFolder.mkdirs();
        gameFolder.mkdirs();
        feedbackFolder.mkdirs();
        this.lootFile = new File(lootFolder, "default.yml");
        this.gameFile = new File(gameFolder, "timing_bar.yml");
        this.feedbackFile = new File(feedbackFolder, "feedback.yml");
    }

    public void load() {
        saveDefaults();
        loadSettings();
        loadLoots();
        loadGames();
        loadFeedback();
    }

    public void reload() {
        load();
    }

    private void saveDefaults() {
        if (!lootFile.exists()) {
            plugin.saveResource("fishing/loots/default.yml", false);
        }
        if (!gameFile.exists()) {
            plugin.saveResource("fishing/games/timing_bar.yml", false);
        }
        if (!feedbackFile.exists()) {
            plugin.saveResource("fishing/feedback/feedback.yml", false);
        }
    }

    private void loadSettings() {
        FileConfiguration config = plugin.getCustomConfig();
        List<String> mechanismKeys = config.getStringList("fishing.enabled-mechanisms");
        List<FishingMechanism> mechanisms = new ArrayList<>();
        if (mechanismKeys != null) {
            for (String key : mechanismKeys) {
                FishingMechanism mechanism = mechanismRegistry.get(key);
                if (mechanism != null) {
                    mechanisms.add(mechanism);
                }
            }
        }
        if (!mechanisms.isEmpty()) {
            enabledMechanisms = List.copyOf(mechanisms);
        }
        defaultGameId = config.getString("fishing.default-minigame", defaultGameId);
        debug = config.getBoolean("fishing.debug", false);
        languageKey = config.getString("fishing.language-key", languageKey);
        voidCheckMinY = config.getInt("fishing.void-check-min-y", voidCheckMinY);
    }

    private void loadLoots() {
        lootPools.clear();
        FileConfiguration loaded = YamlConfiguration.loadConfiguration(lootFile);
        ConfigurationSection pools = loaded.getConfigurationSection("loot-pools");
        if (pools == null) {
            return;
        }
        for (String key : pools.getKeys(false)) {
            FishingMechanism mechanism = mechanismRegistry.get(key);
            if (mechanism == null) {
                continue;
            }
            List<Map<?, ?>> entries = pools.getMapList(key);
            List<LootEntry> lootEntries = new ArrayList<>();
            for (Map<?, ?> map : entries) {
                LootEntry entry = parseLootEntry(map);
                if (entry != null) {
                    lootEntries.add(entry);
                }
            }
            lootPools.put(mechanism, lootEntries);
        }
    }

    private void loadGames() {
        gameDefinitions.clear();
        FileConfiguration loaded = YamlConfiguration.loadConfiguration(gameFile);
        ConfigurationSection games = loaded.getConfigurationSection("games");
        if (games == null) {
            return;
        }
        for (String key : games.getKeys(false)) {
            ConfigurationSection section = games.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            String type = section.getString("type", "TIMING_BAR");
            int durationTicks = section.getInt("duration_ticks", 60);
            double windowMin = section.getDouble("window_min", 0.35);
            double windowMax = section.getDouble("window_max", 0.45);
            List<ConfiguredCondition> conditions = parseConditions(section.getMapList("conditions"));
            GameDefinition definition = new GameDefinition(key, type, durationTicks, windowMin, windowMax, conditions);
            gameDefinitions.put(key, definition);
        }
    }

    private void loadFeedback() {
        themes.clear();
        FileConfiguration loaded = YamlConfiguration.loadConfiguration(feedbackFile);
        ConfigurationSection themeSection = loaded.getConfigurationSection("themes");
        if (themeSection != null) {
            for (String key : themeSection.getKeys(false)) {
                FishingMechanism mechanism = mechanismRegistry.get(key);
                if (mechanism == null) {
                    continue;
                }
                ConfigurationSection section = themeSection.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                String title = section.getString("bossbar_title", "&bReel it in!");
                String colorName = section.getString("bossbar_color", "BLUE");
                org.bukkit.boss.BarColor color;
                try {
                    color = org.bukkit.boss.BarColor.valueOf(colorName.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    color = org.bukkit.boss.BarColor.BLUE;
                }
                themes.put(mechanism, new me.nakilex.levelplugin.fishing.core.feedback.FishingTheme(title, color));
            }
        }
        bitePreset = loadPreset(loaded.getConfigurationSection("feedback.bite"));
        hookedPreset = loadPreset(loaded.getConfigurationSection("feedback.hooked"));
        successPreset = loadPreset(loaded.getConfigurationSection("feedback.success"));
        failPreset = loadPreset(loaded.getConfigurationSection("feedback.fail"));
        linePreset = loadPreset(loaded.getConfigurationSection("feedback.line"));
        windowPreset = loadPreset(loaded.getConfigurationSection("feedback.window"));
    }

    private me.nakilex.levelplugin.fishing.core.feedback.FeedbackPreset loadPreset(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String sound = section.getString("sound");
        float volume = (float) section.getDouble("volume", 1.0);
        float pitch = (float) section.getDouble("pitch", 1.0);
        String particle = section.getString("particle");
        int particleCount = section.getInt("particle_count", 5);
        double offset = section.getDouble("particle_offset", 0.2);
        String title = section.getString("title");
        String subtitle = section.getString("subtitle");
        String actionBar = section.getString("actionbar");
        java.util.List<String> messages = section.getStringList("messages");
        int fadeIn = section.getInt("title_fade_in", 5);
        int stay = section.getInt("title_stay", 25);
        int fadeOut = section.getInt("title_fade_out", 10);
        return new me.nakilex.levelplugin.fishing.core.feedback.FeedbackPreset(
                sound,
                volume,
                pitch,
                particle,
                particleCount,
                offset,
                title,
                subtitle,
                actionBar,
                messages,
                fadeIn,
                stay,
                fadeOut
        );
    }

    private LootEntry parseLootEntry(Map<?, ?> map) {
        if (map == null) {
            return null;
        }
        String id = asString(map.get("id"));
        if (id == null) {
            return null;
        }
        double weight = asDouble(map.get("weight"), 1.0);
        Double minSize = map.containsKey("size_min") ? asDouble(map.get("size_min"), null) : null;
        Double maxSize = map.containsKey("size_max") ? asDouble(map.get("size_max"), null) : null;
        List<ConfiguredCondition> conditions = parseConditions(getMapList(map.get("conditions")));
        List<ConfiguredAction> actions = parseActions(getMapList(map.get("actions")));
        return new LootEntry(id, weight, minSize, maxSize, conditions, actions);
    }

    private List<Map<?, ?>> getMapList(Object value) {
        if (value instanceof List<?> list) {
            List<Map<?, ?>> output = new ArrayList<>();
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> map) {
                    output.add(map);
                }
            }
            return output;
        }
        return Collections.emptyList();
    }

    private List<ConfiguredCondition> parseConditions(List<Map<?, ?>> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<ConfiguredCondition> conditions = new ArrayList<>();
        for (Map<?, ?> entry : raw) {
            String type = asString(entry.get("type"));
            if (type == null) {
                continue;
            }
            Map<String, Object> args = new HashMap<>();
            for (Map.Entry<?, ?> arg : entry.entrySet()) {
                if ("type".equalsIgnoreCase(asString(arg.getKey()))) {
                    continue;
                }
                if (arg.getKey() != null) {
                    args.put(String.valueOf(arg.getKey()), arg.getValue());
                }
            }
            conditions.add(new ConfiguredCondition(type, args));
        }
        return conditions;
    }

    private List<ConfiguredAction> parseActions(List<Map<?, ?>> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<ConfiguredAction> actions = new ArrayList<>();
        for (Map<?, ?> entry : raw) {
            String type = asString(entry.get("type"));
            if (type == null) {
                continue;
            }
            Map<String, Object> args = new HashMap<>();
            for (Map.Entry<?, ?> arg : entry.entrySet()) {
                if ("type".equalsIgnoreCase(asString(arg.getKey()))) {
                    continue;
                }
                if (arg.getKey() != null) {
                    args.put(String.valueOf(arg.getKey()), arg.getValue());
                }
            }
            actions.add(new ConfiguredAction(type, args));
        }
        return actions;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Double asDouble(Object value, Double fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public List<FishingMechanism> getEnabledMechanisms() {
        return enabledMechanisms;
    }

    public String getDefaultGameId() {
        return defaultGameId;
    }

    public boolean isDebug() {
        return debug;
    }

    public String getLanguageKey() {
        return languageKey;
    }

    public int getVoidCheckMinY() {
        return voidCheckMinY;
    }

    public List<LootEntry> getLootPool(FishingMechanism mechanism) {
        return lootPools.getOrDefault(mechanism, Collections.emptyList());
    }

    public Map<String, GameDefinition> getGameDefinitions() {
        return Collections.unmodifiableMap(gameDefinitions);
    }

    public GameDefinition getGameDefinition(String id) {
        if (id == null) {
            return null;
        }
        return gameDefinitions.get(id);
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        FileConfiguration config = plugin.getCustomConfig();
        config.set("fishing.debug", debug);
        plugin.saveCustomConfig();
    }

    public me.nakilex.levelplugin.fishing.core.feedback.FishingTheme getTheme(FishingMechanism mechanism) {
        return themes.get(mechanism);
    }

    public me.nakilex.levelplugin.fishing.core.feedback.FeedbackPreset getBitePreset() {
        return bitePreset;
    }

    public me.nakilex.levelplugin.fishing.core.feedback.FeedbackPreset getHookedPreset() {
        return hookedPreset;
    }

    public me.nakilex.levelplugin.fishing.core.feedback.FeedbackPreset getSuccessPreset() {
        return successPreset;
    }

    public me.nakilex.levelplugin.fishing.core.feedback.FeedbackPreset getFailPreset() {
        return failPreset;
    }

    public me.nakilex.levelplugin.fishing.core.feedback.FeedbackPreset getLinePreset() {
        return linePreset;
    }

    public me.nakilex.levelplugin.fishing.core.feedback.FeedbackPreset getWindowPreset() {
        return windowPreset;
    }
}
