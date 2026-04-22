package me.nakilex.levelplugin.stronghold;

import org.bukkit.ChatColor;

public enum StrongholdQueueMode {
    SOLO("Solo", 1, ChatColor.GREEN),
    DUO("Duo", 2, ChatColor.AQUA),
    SQUAD("Squad", 4, ChatColor.LIGHT_PURPLE);

    private final String displayName;
    private final int teamSize;
    private final ChatColor color;

    StrongholdQueueMode(String displayName, int teamSize, ChatColor color) {
        this.displayName = displayName;
        this.teamSize = teamSize;
        this.color = color;
    }

    public String displayName() {
        return displayName;
    }

    public int teamSize() {
        return teamSize;
    }

    public ChatColor color() {
        return color;
    }
}
