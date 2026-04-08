package me.nakilex.levelplugin.dungeon.generation;

import me.nakilex.levelplugin.dungeon.Direction;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Random branching topology used by debug dungeon generation. */
public class BranchingRandomGraphGenerator implements DungeonGraphGenerator {
    private final int maxDegree;

    public BranchingRandomGraphGenerator() {
        this(4);
    }

    /**
     * @param maxDegree Maximum connector count per node (clamped to [1, 4]).
     */
    public BranchingRandomGraphGenerator(int maxDegree) {
        this.maxDegree = Math.max(1, Math.min(4, maxDegree));
    }

    @Override
    public List<GridNode> generate(int size, Random random) {
        if (size <= 0) {
            return List.of();
        }
        if (maxDegree >= 4) {
            return generateUnbounded(size, random);
        }
        return generateWithDegreeCap(size, random);
    }

    private List<GridNode> generateUnbounded(int size, Random random) {
        Map<GridPoint, Set<Direction>> graph = new HashMap<>();
        Set<GridPoint> placed = new HashSet<>();

        GridPoint start = new GridPoint(0, 0);
        graph.put(start, EnumSet.noneOf(Direction.class));
        placed.add(start);

        while (placed.size() < size) {
            GridPoint[] arr = placed.toArray(new GridPoint[0]);
            GridPoint cur = arr[random.nextInt(arr.length)];

            Direction dir = Direction.random(random);
            GridPoint next = cur.move(dir);

            graph.putIfAbsent(cur, EnumSet.noneOf(Direction.class));
            graph.putIfAbsent(next, EnumSet.noneOf(Direction.class));

            graph.get(cur).add(dir);
            graph.get(next).add(dir.opposite());

            placed.add(next);
        }

        return toNodes(graph, start);
    }

    private List<GridNode> generateWithDegreeCap(int size, Random random) {
        Map<GridPoint, Set<Direction>> graph = new HashMap<>();
        Set<GridPoint> placed = new HashSet<>();

        GridPoint start = new GridPoint(0, 0);
        graph.put(start, EnumSet.noneOf(Direction.class));
        placed.add(start);

        while (placed.size() < size) {
            List<GridPoint> expandable = new ArrayList<>();
            for (GridPoint point : placed) {
                Set<Direction> dirs = graph.getOrDefault(point, Set.of());
                if (dirs.size() >= maxDegree) {
                    continue;
                }
                if (!unclaimedDirections(point, graph, placed).isEmpty()) {
                    expandable.add(point);
                }
            }
            if (expandable.isEmpty()) {
                break;
            }

            GridPoint cur = expandable.get(random.nextInt(expandable.size()));
            List<Direction> options = unclaimedDirections(cur, graph, placed);
            if (options.isEmpty()) {
                continue;
            }
            Direction dir = options.get(random.nextInt(options.size()));
            GridPoint next = cur.move(dir);

            graph.computeIfAbsent(cur, ignored -> EnumSet.noneOf(Direction.class));
            graph.computeIfAbsent(next, ignored -> EnumSet.noneOf(Direction.class));
            graph.get(cur).add(dir);
            graph.get(next).add(dir.opposite());
            placed.add(next);
        }

        return toNodes(graph, start);
    }

    private List<Direction> unclaimedDirections(GridPoint point,
                                                Map<GridPoint, Set<Direction>> graph,
                                                Set<GridPoint> placed) {
        List<Direction> out = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            GridPoint next = point.move(dir);
            if (placed.contains(next)) {
                continue;
            }
            Set<Direction> nextDirs = graph.get(next);
            if (nextDirs != null && nextDirs.size() >= maxDegree) {
                continue;
            }
            out.add(dir);
        }
        return out;
    }

    private List<GridNode> toNodes(Map<GridPoint, Set<Direction>> graph, GridPoint start) {
        List<GridPoint> ordered = new ArrayList<>();
        ordered.add(start);
        for (GridPoint point : graph.keySet()) {
            if (!point.equals(start)) {
                ordered.add(point);
            }
        }

        Map<GridPoint, Integer> idByPoint = new HashMap<>();
        List<GridNode> nodes = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            GridPoint point = ordered.get(i);
            idByPoint.put(point, i);
            nodes.add(new GridNode(i, point.x(), point.z()));
        }

        for (GridPoint point : ordered) {
            int id = idByPoint.get(point);
            GridNode node = nodes.get(id);
            for (Direction dir : graph.getOrDefault(point, Set.of())) {
                GridPoint neighborPoint = point.move(dir);
                Integer neighborId = idByPoint.get(neighborPoint);
                if (neighborId != null) {
                    node.link(dir, neighborId);
                }
            }
        }
        return nodes;
    }
}
