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

    public FastTravelPoint(String name,
                           ChatColor color,
                           String description,
                           Location loc,
                           double radius,
                           boolean town,
                           int expReward) {
        this.name = name;
        this.color = color;
        this.description = description;
        this.location = loc;
        this.radius = radius;
        this.town = town;
        this.expReward = expReward;
    }

    public FastTravelPoint(String name, ChatColor color, String description, Location loc, double radius, boolean town) {
        this(name, color, description, loc, radius, town, 0);
    }

    public String getName() { return name; }
    public ChatColor getColor() { return color; }
    public String getDescription() { return description; }
    public Location getLocation() { return location; }
    public double getRadius() { return radius; }
    public boolean isTown() { return town; }
    public int getExpReward() { return expReward; }

    public void setLocation(Location loc) { this.location = loc; }
    public void setExpReward(int expReward) { this.expReward = expReward; }
}
