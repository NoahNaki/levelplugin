package me.nakilex.levelplugin.items.data;

import org.bukkit.ChatColor;

/**
 * Add a 'symbol' field to each rarity, so we can render ⽆ / ⚋ / ⋀ / … instead of the word.
 */
public enum ItemRarity {
    COMMON   (ChatColor.GRAY,        "<glyph:common>"),
    UNCOMMON (ChatColor.GREEN,       "<glyph:uncommon>"),
    RARE     (ChatColor.BLUE,        "<glyph:rare>"),
    EPIC     (ChatColor.LIGHT_PURPLE,"<glyph:epic>"),
    LEGENDARY(ChatColor.GOLD,        "<glyph:legendary>"),
    MYTHIC   (ChatColor.RED,         "<glyph:mythic>"),
    FABLED   (ChatColor.AQUA,        "<glyph:common>"); // or whatever you want for 'FABLED'

    private final ChatColor color;
    private final String  symbol;

    ItemRarity(ChatColor color, String symbol) {
        this.color  = color;
        this.symbol = symbol;
    }

    public ChatColor getColor() {
        return color;
    }

    /** The one‐character glyph (or string) to show instead of the name. */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Convert a numeric tier (starting at 1) into an ItemRarity.
     * Tiers beyond the defined range default to COMMON.
     */
    public static ItemRarity fromTier(int tier) {
        return switch (tier) {
            case 1 -> COMMON;
            case 2 -> UNCOMMON;
            case 3 -> RARE;
            case 4 -> EPIC;
            case 5 -> LEGENDARY;
            case 6 -> MYTHIC;
            case 7 -> FABLED;
            default -> COMMON;
        };
    }
}
