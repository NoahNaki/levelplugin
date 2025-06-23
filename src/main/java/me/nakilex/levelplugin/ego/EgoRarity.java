package me.nakilex.levelplugin.ego;

import org.bukkit.ChatColor;

public enum EgoRarity {
    // Updated scaling so higher rarities provide a much larger boost
    // which incentivises evolving weapons.
    COMMON   (1.0, ChatColor.GRAY,        "<glyph:common>"),
    UNCOMMON (1.5, ChatColor.GREEN,       "<glyph:uncommon>"),
    RARE     (2.0, ChatColor.BLUE,        "<glyph:rare>"),
    EPIC     (3.0, ChatColor.LIGHT_PURPLE,"<glyph:epic>"),
    LEGENDARY(4.0, ChatColor.GOLD,        "<glyph:legendary>"),
    MYTHIC   (5.0, ChatColor.RED,         "<glyph:mythic>");

    private final double scale;
    private final ChatColor color;
    private final String symbol;

    EgoRarity(double scale, ChatColor color, String symbol) {
        this.scale  = scale;
        this.color  = color;
        this.symbol = symbol;
    }

    public double getScale() { return scale; }
    public ChatColor getColor() { return color; }
    public String getSymbol() { return symbol; }

    /** Returns the next higher rarity or this if at max. */
    public EgoRarity next() {
        int ord = this.ordinal();
        EgoRarity[] vals = values();
        if (ord + 1 >= vals.length) return this;
        return vals[ord + 1];
    }
}
