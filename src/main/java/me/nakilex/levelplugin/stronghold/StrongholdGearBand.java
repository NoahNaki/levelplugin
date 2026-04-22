package me.nakilex.levelplugin.stronghold;

import org.bukkit.ChatColor;

public enum StrongholdGearBand {
    INITIATE("Initiate", 0, 199, ChatColor.GRAY),
    VANGUARD("Vanguard", 200, 399, ChatColor.GREEN),
    MYTHIC("Mythic", 400, Integer.MAX_VALUE, ChatColor.GOLD);

    private final String displayName;
    private final int minScore;
    private final int maxScore;
    private final ChatColor color;

    StrongholdGearBand(String displayName, int minScore, int maxScore, ChatColor color) {
        this.displayName = displayName;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.color = color;
    }

    public static StrongholdGearBand fromAverageGear(int score) {
        for (StrongholdGearBand band : values()) {
            if (score >= band.minScore && score <= band.maxScore) {
                return band;
            }
        }
        return MYTHIC;
    }

    public String display() {
        return color + displayName;
    }
}
