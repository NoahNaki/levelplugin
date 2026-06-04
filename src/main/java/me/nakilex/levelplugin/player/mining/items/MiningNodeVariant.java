package me.nakilex.levelplugin.player.mining.items;

import org.bukkit.ChatColor;

/** Optional ore-node modifiers that reuse the standard ore entity pipeline. */
public enum MiningNodeVariant {
    NORMAL("normal", "", ChatColor.WHITE),
    RICH("rich", "Rich", ChatColor.GOLD),
    BRITTLE("brittle", "Brittle", ChatColor.YELLOW),
    DENSE("dense", "Dense", ChatColor.DARK_GRAY),
    GEODE("geode", "Geode", ChatColor.LIGHT_PURPLE);

    private final String key;
    private final String displayName;
    private final ChatColor color;

    MiningNodeVariant(String key, String displayName, ChatColor color) {
        this.key = key;
        this.displayName = displayName;
        this.color = color;
    }

    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }
    public ChatColor getColor() { return color; }
    public boolean isSpecial() { return this != NORMAL; }
}
