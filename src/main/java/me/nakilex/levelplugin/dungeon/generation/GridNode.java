package me.nakilex.levelplugin.dungeon.generation;

import me.nakilex.levelplugin.dungeon.Direction;

import java.util.EnumMap;
import java.util.EnumSet;

/**
 * Generic generated graph node used by dungeon/stronghold layout planners.
 */
public class GridNode {
    private final int id;
    private final int gx;
    private final int gz;
    private final EnumSet<Direction> directions = EnumSet.noneOf(Direction.class);
    private final EnumMap<Direction, Integer> neighbors = new EnumMap<>(Direction.class);

    public GridNode(int id, int gx, int gz) {
        this.id = id;
        this.gx = gx;
        this.gz = gz;
    }

    public int id() {
        return id;
    }

    public int gx() {
        return gx;
    }

    public int gz() {
        return gz;
    }

    public EnumSet<Direction> directions() {
        return EnumSet.copyOf(directions);
    }

    public EnumMap<Direction, Integer> neighbors() {
        return new EnumMap<>(neighbors);
    }

    public void link(Direction dir, int neighborId) {
        neighbors.put(dir, neighborId);
        directions.add(dir);
    }
}
