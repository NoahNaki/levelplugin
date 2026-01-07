package me.nakilex.levelplugin.items.tools;

import java.util.Locale;

public enum FarmingToolEnchant {
    REAPING("Reaping", "Harvests a 3x3 area."),
    BOUNTIFUL("Bountiful", "Doubles crop yield."),
    ABUNDANCE("Abundance", "3% chance to harvest a 10x10 circle."),
    CONSISTENCY("Consistency", "Harvest size grows every 100 crops without stopping.", "Resets after 3s of no harvesting.");

    private final String displayName;
    private final String[] description;

    FarmingToolEnchant(String displayName, String... description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String[] getDescription() {
        return description;
    }

    public String getKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static FarmingToolEnchant fromKey(String key) {
        if (key == null) return null;
        for (FarmingToolEnchant enchant : values()) {
            if (enchant.getKey().equalsIgnoreCase(key)) {
                return enchant;
            }
        }
        return null;
    }
}
