package me.nakilex.levelplugin.npc.system.trait;

import org.bukkit.Location;

public class CurrentLocationTrait implements NpcTrait {
    private Location location;

    public void setLocation(Location location) {
        this.location = location == null ? null : location.clone();
    }

    public Location getLocation() {
        return location == null ? null : location.clone();
    }
}
