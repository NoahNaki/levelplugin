package me.nakilex.levelplugin.animatedlb;

import org.bukkit.ChatColor;

import java.util.function.BiFunction;

public enum BoardType {
    STRONGHOLD_STAGE("STRONGHOLD PROGRESSION", "⚔", ChatColor.LIGHT_PURPLE,
            (e, type) -> "S" + (int) e.primaryValue() + "-W" + (int) e.secondaryValue()),
    POWER("POWER RANKING", "✦", ChatColor.AQUA,
            (e, type) -> "GS " + (int) e.primaryValue() + " • LV " + (int) e.secondaryValue()),
    MINING("MINING XP", "⛏", ChatColor.GRAY, BoardType::formatLifeSkill),
    FARMING("FARMING XP", "✿", ChatColor.GREEN, BoardType::formatLifeSkill),
    FISHING("FISHING XP", "≈", ChatColor.AQUA, BoardType::formatLifeSkill);

    private final String title;
    private final String icon;
    private final ChatColor color;
    private final BiFunction<LeaderboardEntry, BoardType, String> formatter;

    BoardType(String title, String icon, ChatColor color, BiFunction<LeaderboardEntry, BoardType, String> formatter) {
        this.title = title;
        this.icon = icon;
        this.color = color;
        this.formatter = formatter;
    }

    public String title() { return title; }
    public String icon() { return icon; }
    public ChatColor color() { return color; }
    public String format(LeaderboardEntry entry) { return formatter.apply(entry, this); }

    private static String formatLifeSkill(LeaderboardEntry entry, BoardType ignored) {
        return String.format("%,.0f XP", entry.primaryValue());
    }

}
