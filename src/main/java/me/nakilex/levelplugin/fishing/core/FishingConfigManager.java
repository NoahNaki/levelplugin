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
    private final Map<FishingMechanism, List<LootEntry>> lootPools = new EnumMap<>(FishingMechanism.class);
    private final Map<String, GameDefinition> gameDefinitions = new HashMap<>();

    private List<FishingMechanism> enabledMechanisms = List.of(FishingMechanism.WATER);
    private String defaultGameId = "timing_bar_default";
    private boolean debug;
    private String languageKey = "en";
    private int voidCheckMinY = -64;

    public FishingConfigManager(Main plugin, MechanismRegistry mechanismRegistry) {
        this.plugin = plugin;
        this.mechanismRegistry = mechanismRegistry;
        File dataFolder = new File(plugin.getDataFolder(), "fishing");
        File lootFolder = new File(dataFolder, "loots");
        File gameFolder = new File(dataFolder, "games");
        lootFolder.mkdirs();
        gameFolder.mkdirs();
        this.lootFile = new File(lootFolder, "default.yml");
        this.gameFile = new File(gameFolder, "timing_bar.yml");
    }

    public void load() {
        saveDefaults();
        loadSettings();
        loadLoots();
        loadGames();
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
}
