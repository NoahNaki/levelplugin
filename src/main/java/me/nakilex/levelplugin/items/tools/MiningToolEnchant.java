package me.nakilex.levelplugin.items.tools;

import java.util.Locale;

public enum MiningToolEnchant {
    QUARRY("Quarry", "Mines in a 3x3 plane around the target block."),
    DEEPCORE("Deepcore", "Instantly mines the block to Bedrock and grants stage materials."),
    INSIGHT("Insight", "30% chance to gain +60% Mining XP.");

    private final String displayName;
    private final String[] description;

    MiningToolEnchant(String displayName, String... description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String[] getDescription() { return description; }
    public String getKey() { return name().toLowerCase(Locale.ROOT); }

    public static MiningToolEnchant fromKey(String key) {
        if (key == null) return null;
        for (MiningToolEnchant enchant : values()) {
            if (enchant.getKey().equalsIgnoreCase(key)) return enchant;
        }
        return null;
    }
}
