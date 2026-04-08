package me.nakilex.levelplugin.dungeon.generation;

import me.nakilex.levelplugin.dungeon.Direction;

/** Grid coordinate used by dungeon graph generators. */
public record GridPoint(int x, int z) {
    public GridPoint move(Direction dir) {
        return switch (dir) {
            case NORTH -> new GridPoint(x, z - 1);
            case SOUTH -> new GridPoint(x, z + 1);
            case EAST -> new GridPoint(x + 1, z);
            case WEST -> new GridPoint(x - 1, z);
        };
    }
}
