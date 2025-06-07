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

    public FastTravelPoint(String name, ChatColor color, String description, Location loc, double radius, boolean town) {
        this.name = name;
        this.color = color;
        this.description = description;
        this.location = loc;
        this.radius = radius;
        this.town = town;
    }

    public String getName() { return name; }
    public ChatColor getColor() { return color; }
    public String getDescription() { return description; }
    public Location getLocation() { return location; }
    public double getRadius() { return radius; }
    public boolean isTown() { return town; }

    public void setLocation(Location loc) { this.location = loc; }
}
