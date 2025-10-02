package me.nakilex.levelplugin.arena;

import me.nakilex.levelplugin.arena.rating.ArenaRatingManager;
import org.bukkit.ChatColor;

/**
 * Supported arena match configurations.
 */
public enum ArenaMode {
    ONE_VS_ONE(1, ChatColor.GOLD + "1v1", ArenaRatingManager.RatingCategory.DUEL),
    TWO_VS_TWO(2, ChatColor.AQUA + "2v2", ArenaRatingManager.RatingCategory.TEAM_2V2);

    private final int teamSize;
    private final String displayName;
    private final ArenaRatingManager.RatingCategory ratingCategory;

    ArenaMode(int teamSize, String displayName, ArenaRatingManager.RatingCategory ratingCategory) {
        this.teamSize = teamSize;
        this.displayName = displayName;
        this.ratingCategory = ratingCategory;
    }

    public int teamSize() {
        return teamSize;
    }

    public String displayName() {
        return displayName;
    }

    public String shortName() {
        return name().replace('_', ' ');
    }

    public ArenaRatingManager.RatingCategory ratingCategory() {
        return ratingCategory;
    }
}

