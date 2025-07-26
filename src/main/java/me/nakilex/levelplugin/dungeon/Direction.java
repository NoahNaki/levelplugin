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

    /** Convert a yaw angle to the nearest cardinal direction. */
    public static Direction fromYaw(float yaw) {
        int idx = Math.floorMod(Math.round(yaw / 90f), 4);
        return switch (idx) {
            case 0 -> SOUTH;
            case 1 -> WEST;
            case 2 -> NORTH;
            default -> EAST;
        };
    }
}
