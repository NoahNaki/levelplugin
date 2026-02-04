package me.nakilex.levelplugin.items.v2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import me.nakilex.levelplugin.items.data.ItemRarity;

public class ItemDefinition {
    private final int id;
    private final String name;
    private final ItemType type;
    private final ItemRarity rarity;
    private final ItemRequirements requirements;
    private final ItemVisuals visuals;
    private final ItemGeneration generation;
    private final Map<ItemStatType, StatValue> stats;
    private final Map<String, Object> meta;
    private final int schemaVersion;

    public ItemDefinition(int id,
                          String name,
                          ItemType type,
                          ItemRarity rarity,
                          ItemRequirements requirements,
                          ItemVisuals visuals,
                          ItemGeneration generation,
                          Map<ItemStatType, StatValue> stats,
                          Map<String, Object> meta,
                          int schemaVersion) {
        this.id = id;
        this.name = name == null ? "Unnamed Item" : name;
        this.type = type == null ? ItemType.MISC : type;
        this.rarity = rarity == null ? ItemRarity.COMMON : rarity;
        this.requirements = requirements == null ? new ItemRequirements(1, null) : requirements;
        this.visuals = visuals == null ? new ItemVisuals(null, null) : visuals;
        this.generation = generation == null ? new ItemGeneration(ItemGenerationMode.HANDMADE, null) : generation;
        this.stats = stats == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(stats));
        this.meta = meta == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(meta));
        this.schemaVersion = schemaVersion <= 0 ? 2 : schemaVersion;
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public ItemType type() {
        return type;
    }

    public ItemRarity rarity() {
        return rarity;
    }

    public ItemRequirements requirements() {
        return requirements;
    }

    public ItemVisuals visuals() {
        return visuals;
    }

    public ItemGeneration generation() {
        return generation;
    }

    public Map<ItemStatType, StatValue> stats() {
        return stats;
    }

    public Map<String, Object> meta() {
        return meta;
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public ItemDefinition withId(int newId) {
        return new ItemDefinition(newId, name, type, rarity, requirements, visuals, generation, stats, meta, schemaVersion);
    }
}
