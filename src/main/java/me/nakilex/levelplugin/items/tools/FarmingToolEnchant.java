package me.nakilex.levelplugin.items.tools;

import java.util.Locale;

public enum FarmingToolEnchant {
    REAPING("Reaping"),
    BOUNTIFUL("Bountiful");

    private final String displayName;

    FarmingToolEnchant(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
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
