package me.nakilex.levelplugin.items.tools;

import java.util.Locale;

public enum WoodcuttingToolEnchant {
    CLEAVING("Cleaving", "When a node breaks, also chops up to 3 adjacent logs."),
    IRONWOOD("Ironwood", "25% chance to gain +1 extra log."),
    WISDOM("Wisdom", "20% chance to gain +50% woodcutting XP.");

    private final String displayName;
    private final String[] description;

    WoodcuttingToolEnchant(String displayName, String... description) {
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

    public static WoodcuttingToolEnchant fromKey(String key) {
        if (key == null) return null;
        for (WoodcuttingToolEnchant enchant : values()) {
            if (enchant.getKey().equalsIgnoreCase(key)) {
                return enchant;
            }
        }
        return null;
    }
}
