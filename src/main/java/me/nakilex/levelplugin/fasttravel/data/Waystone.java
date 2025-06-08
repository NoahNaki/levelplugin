package me.nakilex.levelplugin.fasttravel.data;

import org.bukkit.Location;

public class Waystone {
    private final String name;
    private final Location location;
    private final WaystoneType type;

    public Waystone(String name, Location location, WaystoneType type) {
        this.name = name;
        this.location = location;
        this.type = type;
    }

    public String getName() { return name; }
    public Location getLocation() { return location; }
    public WaystoneType getType() { return type; }
}
