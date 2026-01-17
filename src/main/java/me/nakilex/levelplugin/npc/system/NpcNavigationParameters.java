package me.nakilex.levelplugin.npc.system;

public class NpcNavigationParameters {
    private float baseSpeed = 1.0f;
    private float range = 32.0f;

    public float baseSpeed() {
        return baseSpeed;
    }

    public void baseSpeed(float baseSpeed) {
        this.baseSpeed = baseSpeed;
    }

    public float range() {
        return range;
    }

    public void range(float range) {
        this.range = range;
    }

    public void stuckAction(Object ignored) {
        // no-op placeholder
    }
}
