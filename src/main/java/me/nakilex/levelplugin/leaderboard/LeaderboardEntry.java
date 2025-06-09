package me.nakilex.levelplugin.leaderboard;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardEntry {
    public final String id;
    public final World world;
    public final Location location;
    public final LeaderboardType type;
    public final List<ArmorStand> holograms = new ArrayList<>();

    public LeaderboardEntry(String id, World world, Location location, LeaderboardType type) {
        this.id = id;
        this.world = world;
        this.location = location;
        this.type = type;
    }
}
