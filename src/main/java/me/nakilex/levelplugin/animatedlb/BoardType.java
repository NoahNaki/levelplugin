package me.nakilex.levelplugin.animatedlb;

import org.bukkit.ChatColor;

import java.util.function.BiFunction;

public enum BoardType {
    STRONGHOLD_STAGE("STRONGHOLD PROGRESSION", "⚔", ChatColor.LIGHT_PURPLE,
            (e, type) -> "S" + (int) e.primaryValue() + "-W" + (int) e.secondaryValue()),
    POWER("POWER RANKING", "✦", ChatColor.AQUA,
            (e, type) -> "GS " + (int) e.primaryValue() + " • LV " + (int) e.secondaryValue());

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

    public BoardType next() {
        BoardType[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
