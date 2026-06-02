package me.nakilex.levelplugin.player.fishing.data;

import org.bukkit.ChatColor;

/** Reusable trophy quality derived from a fish's position within its species size range. */
public enum FishingQuality {
    NORMAL("Standard", ChatColor.WHITE, 1.0),
    SILVER("Silver", ChatColor.GRAY, 1.35),
    GOLD("Gold", ChatColor.GOLD, 1.80);

    private final String displayName;
    private final ChatColor color;
    private final double valueMultiplier;

    FishingQuality(String displayName, ChatColor color, double valueMultiplier) {
        this.displayName = displayName;
        this.color = color;
        this.valueMultiplier = valueMultiplier;
    }

    public String getDisplayName() { return displayName; }
    public ChatColor getColor() { return color; }
    public double getValueMultiplier() { return valueMultiplier; }

    public static FishingQuality fromSize(FishDefinition definition, double size) {
        if (definition == null) return NORMAL;
        return fromNormalizedSize(normalizeSize(definition.minSize(), definition.maxSize(), size));
    }

    public static FishingQuality fromNormalizedSize(double normalizedSize) {
        if (normalizedSize >= 0.80) return GOLD;
        if (normalizedSize >= 0.50) return SILVER;
        return NORMAL;
    }

    public static double normalizeSize(double min, double max, double size) {
        double range = Math.max(1.0, max - min);
        return Math.max(0.0, Math.min(1.0, (size - min) / range));
    }
}
