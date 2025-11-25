package me.nakilex.levelplugin.mercenary;

/**
 * Lightweight value object tracking a player's friendship progress with
 * a specific mercenary NPC. Friendship points are compared against
 * configured thresholds to derive the current level (1-5).
 */
public final class MercenaryFriendship {
    private int points;
    private int level;

    public MercenaryFriendship(int points, int level) {
        this.points = points;
        this.level = level;
    }

    public int getPoints() {
        return points;
    }

    public int getLevel() {
        return level;
    }

    public void addPoints(int amount) {
        this.points = Math.max(0, this.points + amount);
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
