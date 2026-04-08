package me.nakilex.levelplugin.dungeon.stronghold;

import me.nakilex.levelplugin.dungeon.Direction;
import me.nakilex.levelplugin.dungeon.generation.BranchingRandomGraphGenerator;
import me.nakilex.levelplugin.dungeon.generation.GridNode;
import me.nakilex.levelplugin.dungeon.generation.SnakeGraphGenerator;
import me.nakilex.levelplugin.dungeon.stronghold.StrongholdEnums.GraphMode;

import java.util.*;

/** Graph-first topology model with deterministic generation modes. */
public final class StrongholdGraph {
    private StrongholdGraph() {}

    public record Node(int id, Set<Direction> requiredDirections) {
        public Node {
            requiredDirections = requiredDirections.isEmpty()
                    ? Collections.unmodifiableSet(EnumSet.noneOf(Direction.class))
                    : Collections.unmodifiableSet(EnumSet.copyOf(requiredDirections));
        }

        public int requiredDegree() {
            return requiredDirections.size();
        }
    }

    public record Edge(int fromNodeId, int toNodeId) {}

    public record Graph(List<Node> nodes, List<Edge> edges) {}

    public static Graph generate(GraphMode mode, int size, long seed) {
        return generate(mode, size, seed, 4);
    }

    public static Graph generate(GraphMode mode, int size, long seed, int maxDegree) {
        Random random = new Random(seed);
        int degreeCap = Math.max(1, Math.min(4, maxDegree));
        List<GridNode> raw = switch (mode) {
            case SNAKE -> new SnakeGraphGenerator().generate(size, random);
            case BRANCHING -> new BranchingRandomGraphGenerator(degreeCap).generate(size, random);
            case TEST -> new SnakeGraphGenerator().generate(Math.max(2, Math.min(4, size)), random);
        };
        return fromGrid(raw);
    }

    private static Graph fromGrid(List<GridNode> grid) {
        Map<Integer, Set<Direction>> dirs = new HashMap<>();
        Set<String> edgeSet = new LinkedHashSet<>();
        for (GridNode node : grid) {
            dirs.put(node.id(), node.directions());
            for (Map.Entry<Direction, Integer> link : node.neighbors().entrySet()) {
                int a = Math.min(node.id(), link.getValue());
                int b = Math.max(node.id(), link.getValue());
                edgeSet.add(a + ":" + b);
            }
        }

        List<Node> nodes = new ArrayList<>(grid.size());
        for (GridNode node : grid) {
            nodes.add(new Node(node.id(), dirs.getOrDefault(node.id(), Set.of())));
        }

        List<Edge> edges = new ArrayList<>(edgeSet.size());
        for (String entry : edgeSet) {
            String[] parts = entry.split(":");
            edges.add(new Edge(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
        }

        return new Graph(List.copyOf(nodes), List.copyOf(edges));
    }
}
