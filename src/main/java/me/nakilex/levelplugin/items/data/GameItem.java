package me.nakilex.levelplugin.items.data;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import me.nakilex.levelplugin.potions.data.PotionTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Lightweight representation of an item used across the plugin.  A game item
 * always has a unique id and name but may optionally expose additional
 * components such as gear statistics, potion information, essence data or a
 * raw currency value.  The builder pattern is used to allow only the relevant
 * components to be populated for a particular item type.
 */
public class GameItem {

    private final String id;
    private final String name;
    private final Map<StatType, Integer> gearStats;
    private final PotionTemplate potionData;
    private final EssenceData essenceData;
    private final Integer currencyValue;

    private GameItem(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.gearStats = builder.gearStats != null
                ? Collections.unmodifiableMap(new HashMap<>(builder.gearStats))
                : Collections.emptyMap();
        this.potionData = builder.potionData;
        this.essenceData = builder.essenceData;
        this.currencyValue = builder.currencyValue;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<StatType, Integer> getGearStats() {
        return gearStats;
    }

    public PotionTemplate getPotionData() {
        return potionData;
    }

    public EssenceData getEssenceData() {
        return essenceData;
    }

    public Integer getCurrencyValue() {
        return currencyValue;
    }

    /** Builder for {@link GameItem}. */
    public static class Builder {
        private final String id;
        private final String name;
        private Map<StatType, Integer> gearStats;
        private PotionTemplate potionData;
        private EssenceData essenceData;
        private Integer currencyValue;

        public Builder(String id, String name) {
            this.id = Objects.requireNonNull(id, "id");
            this.name = Objects.requireNonNull(name, "name");
        }

        /** Attach gear statistics to this item. */
        public Builder gearStats(Map<StatType, Integer> stats) {
            this.gearStats = stats;
            return this;
        }

        /** Attach potion template information. */
        public Builder potionData(PotionTemplate potion) {
            this.potionData = potion;
            return this;
        }

        /** Attach essence metadata. */
        public Builder essenceData(EssenceData essence) {
            this.essenceData = essence;
            return this;
        }

        /** Set a raw currency value for this item. */
        public Builder currencyValue(int value) {
            this.currencyValue = value;
            return this;
        }

        public GameItem build() {
            return new GameItem(this);
        }
    }

    /** Simple record describing essence related attributes. */
    public static record EssenceData(String clazz, ItemRarity rarity, int stars) {}
}
