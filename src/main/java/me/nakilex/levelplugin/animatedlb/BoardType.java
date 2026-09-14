package me.nakilex.levelplugin.animatedlb;

import org.bukkit.ChatColor;

import java.util.function.BiFunction;

public enum BoardType {
    STRONGHOLD_STAGE("STRONGHOLD PROGRESSION", "⚔", ChatColor.LIGHT_PURPLE,
            (e, type) -> "S" + (int) e.primaryValue() + "-W" + (int) e.secondaryValue()),
    POWER("POWER RANKING", "✦", ChatColor.AQUA,
            (e, type) -> "GS " + (int) e.primaryValue() + " • LV " + (int) e.secondaryValue()),
    MINING("PICKAXE LEVEL", "⛏", ChatColor.GRAY, BoardType::formatPickaxeLevel),
    XPRISON_RANK("PRISON RANK", "⚑", ChatColor.YELLOW,
            (e, type) -> "RK " + (int) e.primaryValue()),
    XPRISON_PRESTIGE("PRESTIGE", "★", ChatColor.DARK_PURPLE,
            (e, type) -> "PR " + (int) e.primaryValue()),
    XPRISON_REBIRTH("REBIRTH", "♻", ChatColor.RED,
            (e, type) -> "RB " + (int) e.primaryValue()),
    XPRISON_TOKENS("TOP TOKENS", "⛃", ChatColor.GOLD, BoardType::formatXPrisonTokens),
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

    private static String formatPickaxeLevel(LeaderboardEntry entry, BoardType ignored) {
        return "LV " + (int) entry.primaryValue();
    }

    /** Uses X-Prison's own currency formatting (short-format, prefix/suffix) so this matches what players see elsewhere. */
    private static String formatXPrisonTokens(LeaderboardEntry entry, BoardType ignored) {
        try {
            dev.drawethree.xprison.api.currency.model.XPrisonCurrency currency =
                    dev.drawethree.xprison.api.XPrisonAPI.getInstance().getCurrencyApi().getCurrency("tokens");
            if (currency != null) {
                return currency.format(entry.primaryValue());
            }
        } catch (RuntimeException | LinkageError ignored2) {
            // fall through to the plain number below
        }
        return String.format("%,.0f Tokens", entry.primaryValue());
    }

}
