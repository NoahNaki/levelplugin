package me.nakilex.levelplugin.npc.system.trait;

public class LookCloseTrait implements NpcTrait {
    private boolean enabled;
    private double range = 5.0;

    public void lookClose(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setRange(double range) {
        this.range = range;
    }

    public double getRange() {
        return range;
    }
}
