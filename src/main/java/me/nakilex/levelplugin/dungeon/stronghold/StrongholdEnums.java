package me.nakilex.levelplugin.dungeon.stronghold;

import me.nakilex.levelplugin.dungeon.Direction;

/** Shared stronghold enums used by graphing, placement, and validation. */
public final class StrongholdEnums {
    private StrongholdEnums() {}

    public enum GraphMode {
        SNAKE,
        BRANCHING,
        TEST
    }

    public enum ConnectorType {
        CORRIDOR,
        GATE
    }

    public enum TemplateTag {
        STRAIGHT,
        CORNER,
        DEADEND,
        CONNECTOR,
        TOWER,
        GATE,
        LARGE,
        LANDMARK,
        FLAT,
        VERTICAL
    }

    public enum Rotation {
        R0(0),
        R90(1),
        R180(2),
        R270(3);

        private final int quarterTurns;

        Rotation(int quarterTurns) {
            this.quarterTurns = quarterTurns;
        }

        public int quarterTurns() {
            return quarterTurns;
        }

        public Rotation compose(Rotation other) {
            return fromQuarterTurns(this.quarterTurns + other.quarterTurns);
        }

        public Rotation inverse() {
            return fromQuarterTurns(4 - quarterTurns);
        }

        public Direction rotate(Direction dir) {
            int ord = (dir.ordinal() + quarterTurns) & 3;
            return Direction.values()[ord];
        }

        public static Rotation fromQuarterTurns(int turns) {
            return switch (turns & 3) {
                case 0 -> R0;
                case 1 -> R90;
                case 2 -> R180;
                default -> R270;
            };
        }
    }
}
