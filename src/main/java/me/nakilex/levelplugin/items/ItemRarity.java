package me.nakilex.levelplugin.items;

import org.bukkit.ChatColor;

public enum ItemRarity {
    COMMON(ChatColor.GRAY),
    UNCOMMON(ChatColor.GREEN),
    RARE(ChatColor.BLUE),
    EPIC(ChatColor.LIGHT_PURPLE),
    LEGENDARY(ChatColor.GOLD),
    MYTHIC(ChatColor.RED),
    FABLED(ChatColor.AQUA);

    private final ChatColor color;

    ItemRarity(ChatColor color) {
        this.color = color;
    }

    public ChatColor getColor() {
        return color;
    }
}
