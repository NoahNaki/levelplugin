package me.nakilex.levelplugin.leaderboards;

import org.bukkit.Location;
import me.nakilex.levelplugin.utils.MultiLineHologram;

/**
 * Represents a single holographic leaderboard display.
 */
public class Leaderboard extends MultiLineHologram {
    private final String id;
    private final LeaderboardType type;

    public Leaderboard(String id, Location location, LeaderboardType type) {
        super(location);
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public LeaderboardType getType() {
        return type;
    }
}
