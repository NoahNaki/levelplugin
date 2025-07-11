package me.nakilex.levelplugin.quests.gui;

import org.bukkit.ChatColor;
public enum QuestState {
    LOCKED("info", ChatColor.DARK_GRAY),
    AVAILABLE("pack1_scroll2", ChatColor.GOLD),
    ACCEPTED("pack1_scroll2", ChatColor.GOLD),
    IN_PROGRESS("pack1_scroll4", ChatColor.YELLOW),
    TURN_IN_READY("pack1_scroll4", ChatColor.GREEN),
    COMPLETED("check", ChatColor.DARK_GREEN);

    private final String iconId;
    private final ChatColor color;

    QuestState(String iconId, ChatColor color) {
        this.iconId = iconId;
        this.color = color;
    }

    public String getIconId() {
        return iconId;
    }

    public ChatColor getColor() {
        return color;
    }
}
