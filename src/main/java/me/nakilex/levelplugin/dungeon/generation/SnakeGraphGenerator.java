package me.nakilex.levelplugin.dungeon.generation;

import me.nakilex.levelplugin.dungeon.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Deterministic snake-style topology that fills rows back-and-forth. */
public class SnakeGraphGenerator implements DungeonGraphGenerator {
    @Override
    public List<GridNode> generate(int size, Random random) {
        if (size <= 0) {
            return List.of();
        }
        int width = Math.max(2, (int) Math.ceil(Math.sqrt(size)));
        List<int[]> path = new ArrayList<>();
        int z = 0;
        while (path.size() < size) {
            if ((z & 1) == 0) {
                for (int x = 0; x < width && path.size() < size; x++) {
                    path.add(new int[]{x, z});
                }
            } else {
                for (int x = width - 1; x >= 0 && path.size() < size; x--) {
                    path.add(new int[]{x, z});
                }
            }
            z++;
        }
        List<GridNode> nodes = new ArrayList<>();
        for (int i = 0; i < path.size(); i++) {
            nodes.add(new GridNode(i, path.get(i)[0], path.get(i)[1]));
        }
        for (int i = 1; i < nodes.size(); i++) {
            GridNode a = nodes.get(i - 1);
            GridNode b = nodes.get(i);
            Direction dirAB = Direction.fromDelta(b.gx() - a.gx(), b.gz() - a.gz());
            a.link(dirAB, b.id());
            b.link(dirAB.opposite(), a.id());
        }
        return nodes;
    }
}
