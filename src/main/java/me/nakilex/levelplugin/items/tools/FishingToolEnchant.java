package me.nakilex.levelplugin.items.tools;

import java.util.Locale;

public enum FishingToolEnchant {
    LURE("Lure", "Fish bite 20% faster."),
    STEADY_HAND("Steady Hand", "Mini-games grant wider zones, more time,", "and slower moving targets."),
    TROPHY_HUNTER("Trophy Hunter", "Improves the chance to catch silver", "and gold trophy fish."),
    LUCKY_CAST("Lucky Cast", "Improves the chance to hook rare fish."),
    DOUBLE_HOOK("Double Hook", "15% chance to catch a second fish", "after completing a mini-game.");

    private final String displayName;
    private final String[] description;

    FishingToolEnchant(String displayName, String... description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String[] getDescription() { return description; }
    public String getKey() { return name().toLowerCase(Locale.ROOT); }

    public static FishingToolEnchant fromKey(String key) {
        if (key == null) return null;
        for (FishingToolEnchant enchant : values()) {
            if (enchant.getKey().equalsIgnoreCase(key)) return enchant;
        }
        return null;
    }
}
