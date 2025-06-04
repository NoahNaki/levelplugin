package me.nakilex.levelplugin.quests.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;

public enum QuestState {
    LOCKED(Material.BARRIER, ChatColor.DARK_GRAY),
    AVAILABLE(Material.YELLOW_STAINED_GLASS_PANE, ChatColor.GOLD),
    ACCEPTED(Material.PAPER, ChatColor.GOLD),
    IN_PROGRESS(Material.COMPASS, ChatColor.YELLOW),
    TURN_IN_READY(Material.EMERALD, ChatColor.GREEN),
    COMPLETED(Material.ENCHANTED_BOOK, ChatColor.DARK_GREEN);

    private final Material material;
    private final ChatColor color;

    QuestState(Material material, ChatColor color) {
        this.material = material;
        this.color = color;
    }

    public Material getMaterial() {
        return material;
    }

    public ChatColor getColor() {
        return color;
    }
}
