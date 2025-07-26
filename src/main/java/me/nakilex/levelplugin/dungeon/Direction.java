package me.nakilex.levelplugin.dungeon;

public enum Direction {
    NORTH, EAST, SOUTH, WEST;

    public Direction opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
        };
    }

    public static Direction random(java.util.Random rand) {
        return values()[rand.nextInt(values().length)];
    }

    public static Direction fromDelta(int dx, int dz) {
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? EAST : WEST;
        } else {
            return dz > 0 ? SOUTH : NORTH;
        }
    }
}
