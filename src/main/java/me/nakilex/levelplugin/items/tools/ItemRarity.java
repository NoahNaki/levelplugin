package me.nakilex.levelplugin.items.tools;

import org.bukkit.ChatColor;

public enum ItemRarity {
    // Colors aligned with main item rarity scheme
    COMMON(ChatColor.GRAY),
    UNCOMMON(ChatColor.GREEN),
    RARE(ChatColor.BLUE),
    EPIC(ChatColor.LIGHT_PURPLE),
    LEGENDARY(ChatColor.GOLD);

    private final ChatColor color;

    ItemRarity(ChatColor color){
        this.color = color;
    }

    public ChatColor getColor(){
        return color;
    }
}
