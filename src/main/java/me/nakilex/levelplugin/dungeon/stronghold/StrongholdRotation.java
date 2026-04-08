package me.nakilex.levelplugin.dungeon.stronghold;

import me.nakilex.levelplugin.dungeon.Direction;

/** Cardinal 90-degree rotations used by the stronghold placement system. */
public enum StrongholdRotation {
    R0(0),
    R90(1),
    R180(2),
    R270(3);

    private final int quarterTurns;

    StrongholdRotation(int quarterTurns) {
        this.quarterTurns = quarterTurns;
    }

    public int quarterTurns() {
        return quarterTurns;
    }

    public Direction rotate(Direction direction) {
        int idx = (direction.ordinal() + quarterTurns) & 3;
        return Direction.values()[idx];
    }

    public static StrongholdRotation fromQuarterTurns(int quarterTurns) {
        return switch (quarterTurns & 3) {
            case 1 -> R90;
            case 2 -> R180;
            case 3 -> R270;
            default -> R0;
        };
    }
}
