package me.nakilex.levelplugin.spells.deck;

import org.bukkit.ChatColor;
import org.bukkit.Material;

public enum SpellCardCategory {
    OFFENSIVE("Offensive", ChatColor.RED, Material.IRON_SWORD),
    DEFENSIVE("Defensive", ChatColor.AQUA, Material.SHIELD),
    MOBILITY("Mobility", ChatColor.GREEN, Material.FEATHER),
    SUPPORT("Support", ChatColor.LIGHT_PURPLE, Material.GLISTERING_MELON_SLICE),
    UTILITY("Utility", ChatColor.YELLOW, Material.COMPASS);

    private final String displayName;
    private final ChatColor color;
    private final Material displayMaterial;

    SpellCardCategory(String displayName, ChatColor color, Material displayMaterial) {
        this.displayName = displayName;
        this.color = color;
        this.displayMaterial = displayMaterial;
    }

    public String displayName() {
        return displayName;
    }

    public ChatColor color() {
        return color;
    }

    public Material displayMaterial() {
        return displayMaterial;
    }
}
