package me.nakilex.levelplugin.fasttravel.data;

import org.bukkit.ChatColor;
import org.bukkit.Location;

public class FastTravelPoint {
    private String name;
    private ChatColor color;
    private String description;
    private Location location;
    private double radius;
    private boolean town;
    private int expReward;
    private String musicTrack;

    public FastTravelPoint(String name,
                           ChatColor color,
                           String description,
                           Location loc,
                           double radius,
                           boolean town,
                           int expReward,
                           String musicTrack) {
        this.name = name;
        this.color = color;
        this.description = description;
        this.location = loc;
        this.radius = radius;
        this.town = town;
        this.expReward = expReward;
        this.musicTrack = musicTrack;
    }

    public FastTravelPoint(String name, ChatColor color, String description, Location loc, double radius, boolean town) {
        this(name, color, description, loc, radius, town, 0, null);
    }

    public FastTravelPoint(String name,
                           ChatColor color,
                           String description,
                           Location loc,
                           double radius,
                           boolean town,
                           int expReward) {
        this(name, color, description, loc, radius, town, expReward, null);
    }

    public String getName() { return name; }
    public ChatColor getColor() { return color; }
    public String getDescription() { return description; }
    public Location getLocation() { return location; }
    public double getRadius() { return radius; }
    public boolean isTown() { return town; }
    public int getExpReward() { return expReward; }
    public String getMusicTrack() { return musicTrack; }

    public void setLocation(Location loc) { this.location = loc; }
    public void setExpReward(int expReward) { this.expReward = expReward; }
    public void setMusicTrack(String musicTrack) { this.musicTrack = musicTrack; }
}
