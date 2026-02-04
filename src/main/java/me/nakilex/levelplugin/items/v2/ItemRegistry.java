package me.nakilex.levelplugin.items.v2;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.utils.FileUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ItemRegistry {
    private static final int SCHEMA_VERSION = 2;
    private static final long SAVE_DEBOUNCE_TICKS = 40L;

    private final Main plugin;
    private final File dataFile;
    private final Map<Integer, ItemDefinition> byId = new ConcurrentHashMap<>();
    private BukkitTask pendingSave;

    public ItemRegistry(Main plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "items_v2.yml");
    }

    public void load() {
        byId.clear();
        if (!dataFile.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        int schema = config.getInt("schema", SCHEMA_VERSION);
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            return;
        }
        if (schema != SCHEMA_VERSION) {
            plugin.getLogger().warning("Unsupported item schema version " + schema + " in " + dataFile.getName());
            return;
        }
        loadSchemaV2(itemsSection);
    }

    public Optional<ItemDefinition> get(int id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Map<Integer, ItemDefinition> getAll() {
        return Map.copyOf(byId);
    }

    public ItemDefinition createNew(ItemDefinition definition) {
        int nextId = byId.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
        ItemDefinition created = definition.withId(nextId);
        byId.put(nextId, created);
        queueSave();
        return created;
    }

    public void update(ItemDefinition definition) {
        byId.put(definition.id(), definition);
        queueSave();
    }

    public void delete(int id) {
        byId.remove(id);
        queueSave();
    }

    public void queueSave() {
        if (pendingSave != null) {
            pendingSave.cancel();
        }
        pendingSave = plugin.getServer().getScheduler().runTaskLaterAsynchronously(
                plugin,
                this::saveNow,
                SAVE_DEBOUNCE_TICKS
        );
    }

    public void saveNow() {
        FileConfiguration config = new YamlConfiguration();
        config.set("schema", SCHEMA_VERSION);
        Map<String, Object> itemsMap = new HashMap<>();
        for (ItemDefinition definition : byId.values()) {
            itemsMap.put(String.valueOf(definition.id()), serializeDefinition(definition));
        }
        config.set("items", itemsMap);
        try {
            FileUtil.writeYamlAtomic(dataFile, config);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save items_v2.yml: " + e.getMessage());
        }
    }

    private void loadSchemaV2(ConfigurationSection itemsSection) {
        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection section = itemsSection.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            int id = parseId(key, section);
            if (id <= 0) {
                plugin.getLogger().warning("Skipping item with invalid id key: " + key);
                continue;
            }
            ItemDefinition definition = parseDefinition(id, section);
            byId.put(id, definition);
        }
    }

    private int parseId(String key, ConfigurationSection section) {
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException ex) {
            return section.getInt("id", -1);
        }
    }

    private ItemDefinition parseDefinition(int id, ConfigurationSection section) {
        String name = section.getString("name", "Item " + id);
        ItemType type = parseItemType(section.getString("type", "MISC"));
        ItemRarity rarity = parseRarity(section.getString("rarity", "COMMON"));

        ConfigurationSection reqSection = section.getConfigurationSection("requirements");
        int levelReq = reqSection != null ? reqSection.getInt("level", 1) : 1;
        List<PlayerClass> classes = parseClasses(reqSection != null ? reqSection.getStringList("classes") : List.of());
        ItemRequirements requirements = new ItemRequirements(levelReq, classes);

        ConfigurationSection visualsSection = section.getConfigurationSection("visuals");
        Material baseMaterial = visualsSection != null
                ? parseMaterial(visualsSection.getString("baseMaterial", "DIAMOND"), Material.DIAMOND)
                : Material.DIAMOND;
        String modelKey = visualsSection != null ? visualsSection.getString("modelKey", "unassigned") : "unassigned";
        ItemVisuals visuals = new ItemVisuals(baseMaterial, modelKey);

        ConfigurationSection genSection = section.getConfigurationSection("generation");
        ItemGenerationMode mode = genSection != null
                ? parseGenerationMode(genSection.getString("mode", "HANDMADE"))
                : ItemGenerationMode.HANDMADE;
        String profileKey = genSection != null ? genSection.getString("profileKey") : null;
        ItemGeneration generation = new ItemGeneration(mode, profileKey);

        Map<ItemStatType, StatValue> stats = parseStats(section.getConfigurationSection("stats"));
        Map<String, Object> meta = parseMeta(section.getConfigurationSection("meta"));
        int schemaVersion = section.getInt("schemaVersion", SCHEMA_VERSION);
        return new ItemDefinition(id, name, type, rarity, requirements, visuals, generation, stats, meta, schemaVersion);
    }

    private Map<ItemStatType, StatValue> parseStats(ConfigurationSection statSection) {
        Map<ItemStatType, StatValue> stats = new EnumMap<>(ItemStatType.class);
        if (statSection == null) {
            return stats;
        }
        for (String key : statSection.getKeys(false)) {
            ItemStatType type = ItemStatType.fromKey(key);
            if (type == null) {
                continue;
            }
            ConfigurationSection entry = statSection.getConfigurationSection(key);
            StatValue value = parseStatValue(entry, statSection.get(key));
            if (value != null) {
                stats.put(type, value);
            }
        }
        return stats;
    }

    private StatValue parseStatValue(ConfigurationSection entry, Object raw) {
        if (entry != null) {
            if (entry.contains("fixed")) {
                return StatValue.fixed(entry.getDouble("fixed"));
            }
            ConfigurationSection rangeSection = entry.getConfigurationSection("range");
            if (rangeSection != null) {
                double min = rangeSection.getDouble("min");
                double max = rangeSection.getDouble("max");
                return StatValue.range(min, max);
            }
        }
        if (raw instanceof Number number) {
            return StatValue.fixed(number.doubleValue());
        }
        if (raw instanceof String str) {
            return StatValue.fromLegacyRangeString(str);
        }
        return null;
    }

    private Map<String, Object> parseMeta(ConfigurationSection metaSection) {
        if (metaSection == null) {
            return Map.of();
        }
        return new HashMap<>(metaSection.getValues(false));
    }

    private Map<String, Object> serializeDefinition(ItemDefinition definition) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", definition.name());
        map.put("type", definition.type().name());
        map.put("rarity", definition.rarity().name());
        map.put("schemaVersion", definition.schemaVersion());

        ItemRequirements requirements = definition.requirements();
        Map<String, Object> req = new HashMap<>();
        req.put("level", requirements.level());
        if (!requirements.classes().isEmpty()) {
            List<String> classNames = new ArrayList<>();
            for (PlayerClass playerClass : requirements.classes()) {
                classNames.add(playerClass.name());
            }
            req.put("classes", classNames);
        }
        map.put("requirements", req);

        ItemVisuals visuals = definition.visuals();
        Map<String, Object> visualsMap = new HashMap<>();
        visualsMap.put("baseMaterial", visuals.baseMaterial().name());
        visualsMap.put("modelKey", visuals.modelKey());
        map.put("visuals", visualsMap);

        ItemGeneration generation = definition.generation();
        Map<String, Object> generationMap = new HashMap<>();
        generationMap.put("mode", generation.mode().name());
        if (generation.profileKey() != null) {
            generationMap.put("profileKey", generation.profileKey());
        }
        map.put("generation", generationMap);

        Map<String, Object> statMap = new HashMap<>();
        for (Map.Entry<ItemStatType, StatValue> entry : definition.stats().entrySet()) {
            Map<String, Object> statValue = new HashMap<>();
            StatValue value = entry.getValue();
            if (value.isFixed()) {
                statValue.put("fixed", value.min());
            } else {
                Map<String, Object> range = new HashMap<>();
                range.put("min", value.min());
                range.put("max", value.max());
                statValue.put("range", range);
            }
            statMap.put(entry.getKey().name(), statValue);
        }
        map.put("stats", statMap);

        if (!definition.meta().isEmpty()) {
            map.put("meta", definition.meta());
        }
        return map;
    }

    private ItemType parseItemType(String raw) {
        if (raw == null) return ItemType.MISC;
        try {
            return ItemType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ItemType.MISC;
        }
    }

    private ItemRarity parseRarity(String raw) {
        if (raw == null) return ItemRarity.COMMON;
        try {
            return ItemRarity.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ItemRarity.COMMON;
        }
    }

    private ItemGenerationMode parseGenerationMode(String raw) {
        if (raw == null) return ItemGenerationMode.HANDMADE;
        try {
            return ItemGenerationMode.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ItemGenerationMode.HANDMADE;
        }
    }

    private Material parseMaterial(String raw, Material fallback) {
        if (raw == null) return fallback;
        try {
            return Material.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private List<PlayerClass> parseClasses(List<String> raw) {
        List<PlayerClass> classes = new ArrayList<>();
        if (raw == null) {
            return classes;
        }
        for (String entry : raw) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            PlayerClass playerClass = PlayerClass.fromString(entry);
            if (playerClass != null && playerClass != PlayerClass.VILLAGER) {
                classes.add(playerClass);
            }
        }
        return classes;
    }
}
