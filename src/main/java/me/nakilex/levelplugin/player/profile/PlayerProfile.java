package me.nakilex.levelplugin.player.profile;

public class PlayerProfile {
    private final int slot;
    private final String name;
    private org.bukkit.Location lastLocation;

    public PlayerProfile(int slot, String name) {
        this.slot = slot;
        this.name = name;
    }

    public int getSlot() { return slot; }
    public String getName() { return name; }

    public org.bukkit.Location getLastLocation() { return lastLocation; }
    public void setLastLocation(org.bukkit.Location loc) { this.lastLocation = loc; }
}
