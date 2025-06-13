package me.nakilex.levelplugin.items.tools;

import org.bukkit.ChatColor;

public enum ItemRarity {
    COMMON(ChatColor.WHITE),
    UNCOMMON(ChatColor.GREEN),
    RARE(ChatColor.BLUE),
    EPIC(ChatColor.DARK_PURPLE),
    LEGENDARY(ChatColor.GOLD);

    private final ChatColor color;

    ItemRarity(ChatColor color){
        this.color = color;
    }

    public ChatColor getColor(){
        return color;
    }
}
