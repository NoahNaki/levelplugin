package me.nakilex.levelplugin.ego;

import org.bukkit.ChatColor;

public enum EgoRarity {
    COMMON   (1.0,  ChatColor.GRAY,        "<glyph:common>"),
    UNCOMMON (1.25, ChatColor.GREEN,       "<glyph:uncommon>"),
    RARE     (1.5,  ChatColor.BLUE,        "<glyph:rare>"),
    EPIC     (2.0,  ChatColor.LIGHT_PURPLE,"<glyph:epic>"),
    LEGENDARY(2.5,  ChatColor.GOLD,        "<glyph:legendary>"),
    MYTHIC   (3.0,  ChatColor.RED,         "<glyph:mythic>");

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
