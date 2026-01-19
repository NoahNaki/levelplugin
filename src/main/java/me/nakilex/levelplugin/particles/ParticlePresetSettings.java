package me.nakilex.levelplugin.particles;

public final class ParticlePresetSettings {
    private final int points;
    private final int ticks;
    private final ParticleCenter center;
    private final double lookDistance;

    public ParticlePresetSettings(int points, int ticks, ParticleCenter center, double lookDistance) {
        this.points = Math.max(1, points);
        this.ticks = Math.max(1, ticks);
        this.center = center == null ? ParticleCenter.SELF : center;
        this.lookDistance = lookDistance <= 0 ? 6.0 : lookDistance;
    }

    public int points() {
        return points;
    }

    public int ticks() {
        return ticks;
    }

    public ParticleCenter center() {
        return center;
    }

    public double lookDistance() {
        return lookDistance;
    }
}
