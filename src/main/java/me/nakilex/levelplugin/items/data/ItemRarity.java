package me.nakilex.levelplugin.items.data;

import org.bukkit.ChatColor;

/**
 * Add a 'symbol' field to each rarity, so we can render ⽆ / ⚋ / ⋀ / … instead of the word.
 */
public enum ItemRarity {
    COMMON   (ChatColor.GRAY,        "⽆"),
    UNCOMMON (ChatColor.GREEN,       "⡽"),
    RARE     (ChatColor.BLUE,        "⚋"),
    EPIC     (ChatColor.LIGHT_PURPLE,"┨"),
    LEGENDARY(ChatColor.GOLD,        "✹"),
    MYTHIC   (ChatColor.RED,         "⋀"),
    FABLED   (ChatColor.AQUA,        "Ⲁ"); // or whatever you want for 'FABLED'

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
}
