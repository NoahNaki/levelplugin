package me.nakilex.levelplugin.player.profile;

public class PlayerProfile {
    private final int slot;
    private final String name;

    public PlayerProfile(int slot, String name) {
        this.slot = slot;
        this.name = name;
    }

    public int getSlot() { return slot; }
    public String getName() { return name; }
}
