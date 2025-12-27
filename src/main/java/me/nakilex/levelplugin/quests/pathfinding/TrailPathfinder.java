package me.nakilex.levelplugin.quests.pathfinding;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Simple 2.5D A* pathfinder for walkable terrain trails.
 */
public class TrailPathfinder {

    private static final int[][] NEIGHBORS = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1},
            {1, 1},
            {1, -1},
            {-1, 1},
            {-1, -1}
    };

    public StandablePath findPath(Location start, Location goal, PathSettings settings) {
        if (start == null || goal == null || settings == null) {
            return new StandablePath(Collections.emptyList(), null, null);
        }
        World world = start.getWorld();
        if (world == null || !world.equals(goal.getWorld())) {
            return new StandablePath(Collections.emptyList(), null, null);
        }

        Location targetGoal = clampGoalDistance(start, goal, settings.maxTargetDistance());

        Node startNode = resolveNode(world, start.getBlockX(), start.getBlockZ(), start.getBlockY(), settings);
        Node goalNode = resolveNode(world, targetGoal.getBlockX(), targetGoal.getBlockZ(), targetGoal.getBlockY(), settings);
        if (startNode == null || goalNode == null) {
            return new StandablePath(Collections.emptyList(), null, null);
        }

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(node -> node.f));
        Map<NodeKey, Node> all = new HashMap<>();
        Set<NodeKey> closed = new HashSet<>();

        startNode.g = 0;
        startNode.h = heuristic(startNode, goalNode);
        startNode.f = startNode.g + startNode.h;
        open.add(startNode);
        all.put(startNode.key(), startNode);

        int expansions = 0;
        while (!open.isEmpty() && expansions++ < settings.maxExpansions()) {
            Node current = open.poll();
            if (isClose(current, goalNode)) {
                List<Location> path = reconstructPath(current, world);
                return new StandablePath(path, start, targetGoal);
            }
            closed.add(current.key());

            for (Node neighbor : neighbors(world, current, settings)) {
                if (neighbor == null) {
                    continue;
                }
                if (closed.contains(neighbor.key())) {
                    continue;
                }
                if (Math.abs(neighbor.x - startNode.x) > settings.maxRadius()
                        || Math.abs(neighbor.z - startNode.z) > settings.maxRadius()) {
                    continue;
                }

                double tentativeG = current.g + stepCost(world, current, neighbor);
                Node known = all.get(neighbor.key());
                if (known == null || tentativeG < known.g) {
                    if (known == null) {
                        known = neighbor;
                        all.put(known.key(), known);
                    }
                    known.parent = current;
                    known.g = tentativeG;
                    known.h = heuristic(known, goalNode);
                    known.f = known.g + known.h;
                    open.add(known);
                }
            }
        }

        return new StandablePath(Collections.emptyList(), start, targetGoal);
    }

    private Location clampGoalDistance(Location start, Location goal, int maxDistance) {
        if (start.getWorld() == null || goal.getWorld() == null) {
            return goal;
        }
        double distance = start.distance(goal);
        if (distance <= maxDistance || distance <= 0.1) {
            return goal;
        }
        Vector dir = goal.toVector().subtract(start.toVector()).normalize();
        return start.clone().add(dir.multiply(maxDistance));
    }

    private boolean isClose(Node current, Node goal) {
        return Math.abs(current.x - goal.x) <= 1 && Math.abs(current.z - goal.z) <= 1;
    }

    private double heuristic(Node a, Node b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private List<Location> reconstructPath(Node node, World world) {
        List<Location> path = new ArrayList<>();
        Node current = node;
        while (current != null) {
            Location loc = new Location(world, current.x + 0.5, current.y, current.z + 0.5);
            path.add(loc);
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private List<Node> neighbors(World world, Node current, PathSettings settings) {
        List<Node> nodes = new ArrayList<>(NEIGHBORS.length);
        for (int[] dir : NEIGHBORS) {
            int nx = current.x + dir[0];
            int nz = current.z + dir[1];
            Node neighbor = resolveNode(world, nx, nz, current.y, settings);
            if (neighbor == null) {
                continue;
            }
            int deltaY = neighbor.y - current.y;
            if (deltaY > settings.maxStepUp() || deltaY < -settings.maxStepDown()) {
                continue;
            }
            nodes.add(neighbor);
        }
        return nodes;
    }

    private Node resolveNode(World world, int x, int z, int preferredY, PathSettings settings) {
        Integer standY = findStandY(world, x, z, preferredY, settings);
        if (standY == null) {
            return null;
        }
        return new Node(x, standY, z);
    }

    private Integer findStandY(World world, int x, int z, int preferredY, PathSettings settings) {
        int minY = world.getMinHeight() + 1;
        int maxY = world.getMaxHeight() - 2;
        int downLimit = Math.min(settings.scanDown(), preferredY - minY);
        int upLimit = Math.min(settings.scanUp(), maxY - preferredY);
        int maxDelta = Math.max(downLimit, upLimit);
        for (int i = 0; i <= maxDelta; i++) {
            if (i == 0) {
                if (isStandable(world, x, preferredY, z)) {
                    return preferredY;
                }
                continue;
            }
            if (i <= downLimit) {
                int y = preferredY - i;
                if (isStandable(world, x, y, z)) {
                    return y;
                }
            }
            if (i <= upLimit) {
                int y = preferredY + i;
                if (isStandable(world, x, y, z)) {
                    return y;
                }
            }
        }
        return null;
    }

    private boolean isStandable(World world, int x, int y, int z) {
        if (y <= world.getMinHeight() || y >= world.getMaxHeight() - 1) {
            return false;
        }
        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        Block ground = world.getBlockAt(x, y - 1, z);
        if (!feet.isPassable() || !head.isPassable()) {
            return false;
        }
        if (!ground.getType().isSolid()) {
            return false;
        }
        Material groundType = ground.getType();
        return groundType != Material.MAGMA_BLOCK && groundType != Material.CACTUS && groundType != Material.LAVA;
    }

    private double stepCost(World world, Node from, Node to) {
        double base = (from.x == to.x || from.z == to.z) ? 1.0 : 1.4;
        int deltaY = to.y - from.y;
        if (deltaY > 0) {
            base += 0.35;
        } else if (deltaY < 0) {
            base += 0.2;
        }
        Block ground = world.getBlockAt(to.x, to.y - 1, to.z);
        if (ground.getType() == Material.SOUL_SAND) {
            base += 0.4;
        }
        return base;
    }

    public static List<Location> simplify(List<Location> path) {
        if (path == null || path.size() < 3) {
            return path == null ? Collections.emptyList() : new ArrayList<>(path);
        }
        List<Location> simplified = new ArrayList<>();
        simplified.add(path.get(0));
        for (int i = 1; i < path.size() - 1; i++) {
            Location prev = simplified.get(simplified.size() - 1);
            Location current = path.get(i);
            Location next = path.get(i + 1);
            if (!isCollinear(prev, current, next)) {
                simplified.add(current);
            }
        }
        simplified.add(path.get(path.size() - 1));
        return simplified;
    }

    private static boolean isCollinear(Location a, Location b, Location c) {
        Vector ab = b.toVector().subtract(a.toVector());
        Vector bc = c.toVector().subtract(b.toVector());
        return Math.signum(ab.getX()) == Math.signum(bc.getX())
                && Math.signum(ab.getZ()) == Math.signum(bc.getZ())
                && Math.abs(ab.getX()) + Math.abs(ab.getZ()) > 0;
    }

    public static List<Location> interpolate(List<Location> path, double spacing) {
        if (path == null || path.size() < 2) {
            return path == null ? Collections.emptyList() : new ArrayList<>(path);
        }
        List<Location> points = new ArrayList<>();
        for (int i = 0; i < path.size() - 1; i++) {
            Location start = path.get(i);
            Location end = path.get(i + 1);
            double dist = start.distance(end);
            int steps = Math.max(1, (int) Math.ceil(dist / spacing));
            Vector delta = end.toVector().subtract(start.toVector()).multiply(1d / steps);
            for (int step = 0; step < steps; step++) {
                Location loc = start.clone().add(delta.clone().multiply(step));
                points.add(loc);
            }
        }
        points.add(path.get(path.size() - 1));
        return points;
    }

    public record PathSettings(int maxRadius,
                               int maxExpansions,
                               int maxTargetDistance,
                               int maxStepUp,
                               int maxStepDown,
                               int scanDown,
                               int scanUp) {
    }

    public record PathTarget(Location location, PathTargetKey key) {
        public static PathTarget of(Location location) {
            if (location == null || location.getWorld() == null) {
                return null;
            }
            return new PathTarget(location, new PathTargetKey(location));
        }
    }

    public record PathTargetKey(String worldName, int x, int y, int z) {
        public PathTargetKey(Location location) {
            this(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }
    }

    public record StandablePath(List<Location> locations, Location start, Location goal) {
    }

    private static class Node {
        private final int x;
        private final int y;
        private final int z;
        private double g;
        private double h;
        private double f;
        private Node parent;

        Node(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        NodeKey key() {
            return new NodeKey(x, y, z);
        }
    }

    private record NodeKey(int x, int y, int z) {
    }
}
