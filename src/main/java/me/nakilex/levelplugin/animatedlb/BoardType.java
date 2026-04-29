package me.nakilex.levelplugin.animatedlb;

import org.bukkit.ChatColor;

import java.text.DecimalFormat;
import java.util.function.Function;

public enum BoardType {
    KILLS("KILLS LEADERBOARD", "⚔", ChatColor.RED, v -> String.valueOf((int) v.doubleValue())),
    DEATHS("DEATHS LEADERBOARD", "☠", ChatColor.DARK_PURPLE, v -> String.valueOf((int) v.doubleValue())),
    MONEY("MONEY LEADERBOARD", "⛁", ChatColor.GOLD, v -> "$" + new DecimalFormat("#,###").format(v));

    private final String title;
    private final String icon;
    private final ChatColor color;
    private final Function<Double, String> formatter;

    BoardType(String title, String icon, ChatColor color, Function<Double, String> formatter) {
        this.title = title;
        this.icon = icon;
        this.color = color;
        this.formatter = formatter;
    }

    public String title() { return title; }
    public String icon() { return icon; }
    public ChatColor color() { return color; }
    public String format(double value) { return formatter.apply(value); }

    public BoardType next() {
        BoardType[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
