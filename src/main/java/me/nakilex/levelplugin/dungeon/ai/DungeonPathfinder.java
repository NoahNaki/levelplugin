package me.nakilex.levelplugin.dungeon.ai;

import me.nakilex.levelplugin.dungeon.Dungeon;
import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.dungeon.RoomTemplate;
import me.nakilex.levelplugin.dungeon.Direction;
import org.bukkit.Location;

import java.util.*;

/**
 * Utility to build a simple path of room centers from the dungeon entrance
 * to the exit. This uses connector locations to determine adjacency between
 * rooms.
 */
public class DungeonPathfinder {
    private DungeonPathfinder() {}

    public static List<Location> computePath(Dungeon dungeon, DungeonManager manager,
                                             java.util.function.Consumer<String> debug) {
        if (debug == null) debug = s -> {};
        Dungeon.RoomInstance start = null;
        Dungeon.RoomInstance end = null;
        for (Dungeon.RoomInstance r : dungeon.getRooms()) {
            if (r.template == manager.getEntrance()) start = r;
            if (r.template == manager.getExit()) end = r;
        }
        if (start == null || end == null) {
            debug.accept("Missing entrance or exit when computing path");
            return List.of();
        }

        Map<Dungeon.RoomInstance, List<Dungeon.RoomInstance>> graph = buildGraph(dungeon);
        List<Location> route = bfsPath(start, end, graph);
        if (route == null) {
            debug.accept("No connector path found, falling back to center adjacency");
            graph = buildAdjacencyGraph(dungeon, manager.getStep());
            route = bfsPath(start, end, graph);
            if (route == null) {
                debug.accept("No path found between entrance and exit");
                return List.of();
            }
        }

        debug.accept("Computed route with " + route.size() + " waypoints");
        return route;
    }

    private static Map<Dungeon.RoomInstance, List<Dungeon.RoomInstance>> buildGraph(Dungeon dungeon) {
        Map<Dungeon.RoomInstance, List<Dungeon.RoomInstance>> graph = new HashMap<>();
        List<Dungeon.RoomInstance> rooms = dungeon.getRooms();
        for (Dungeon.RoomInstance a : rooms) {
            graph.putIfAbsent(a, new ArrayList<>());
            for (Dungeon.RoomInstance b : rooms) {
                if (a == b) continue;
                if (connected(a, b)) {
                    graph.get(a).add(b);
                }
            }
        }
        return graph;
    }

    private static Map<Dungeon.RoomInstance, List<Dungeon.RoomInstance>> buildAdjacencyGraph(Dungeon dungeon, int step) {
        Map<Dungeon.RoomInstance, List<Dungeon.RoomInstance>> graph = new HashMap<>();
        List<Dungeon.RoomInstance> rooms = dungeon.getRooms();
        for (Dungeon.RoomInstance a : rooms) {
            graph.putIfAbsent(a, new ArrayList<>());
            for (Dungeon.RoomInstance b : rooms) {
                if (a == b) continue;
                if (adjacentCenters(a, b, step)) {
                    graph.get(a).add(b);
                }
            }
        }
        return graph;
    }

    private static boolean adjacentCenters(Dungeon.RoomInstance a, Dungeon.RoomInstance b, int step) {
        if (!a.center.getWorld().equals(b.center.getWorld())) return false;
        double dx = Math.abs(a.center.getBlockX() - b.center.getBlockX());
        double dz = Math.abs(a.center.getBlockZ() - b.center.getBlockZ());
        return (dx == step && dz == 0) || (dz == step && dx == 0);
    }

    private static List<Location> bfsPath(Dungeon.RoomInstance start, Dungeon.RoomInstance end,
                                          Map<Dungeon.RoomInstance, List<Dungeon.RoomInstance>> graph) {
        Map<Dungeon.RoomInstance, Dungeon.RoomInstance> prev = new HashMap<>();
        Deque<Dungeon.RoomInstance> queue = new ArrayDeque<>();
        queue.add(start);
        prev.put(start, null);
        while (!queue.isEmpty()) {
            Dungeon.RoomInstance cur = queue.removeFirst();
            if (cur.equals(end)) break;
            for (Dungeon.RoomInstance n : graph.getOrDefault(cur, List.of())) {
                if (!prev.containsKey(n)) {
                    prev.put(n, cur);
                    queue.addLast(n);
                }
            }
        }
        if (!prev.containsKey(end)) return null;

        List<Location> route = new ArrayList<>();
        route.add(start.center.clone().add(0.5, 0, 0.5));
        java.util.LinkedList<Location> path = new java.util.LinkedList<>();
        for (Dungeon.RoomInstance cur = end; prev.get(cur) != null; cur = prev.get(cur)) {
            Dungeon.RoomInstance from = prev.get(cur);
            Location conn = connectionPointBetween(from, cur);
            path.addFirst(conn);
        }
        route.addAll(path);
        route.add(end.center.clone().add(0.5, 0, 0.5));
        return route;
    }

    private static boolean connected(Dungeon.RoomInstance a, Dungeon.RoomInstance b) {
        for (RoomTemplate.Connector ca : a.template.getConnectors()) {
            Direction da = rotate(ca.facing, a.rotation);
            Location wa = connectorWorld(a, ca);
            for (RoomTemplate.Connector cb : b.template.getConnectors()) {
                Direction db = rotate(cb.facing, b.rotation);
                if (da.opposite() != db) continue;
                Location wb = connectorWorld(b, cb);
                if (wa.getWorld().equals(wb.getWorld()) &&
                        wa.getBlockX() == wb.getBlockX() &&
                        wa.getBlockZ() == wb.getBlockZ()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Location connectionPointBetween(Dungeon.RoomInstance a, Dungeon.RoomInstance b) {
        for (RoomTemplate.Connector ca : a.template.getConnectors()) {
            Direction da = rotate(ca.facing, a.rotation);
            Location wa = connectorWorld(a, ca);
            for (RoomTemplate.Connector cb : b.template.getConnectors()) {
                Direction db = rotate(cb.facing, b.rotation);
                if (da.opposite() != db) continue;
                Location wb = connectorWorld(b, cb);
                if (wa.getWorld().equals(wb.getWorld()) && wa.getBlockX() == wb.getBlockX() && wa.getBlockZ() == wb.getBlockZ()) {
                    return wa.clone().add(0.5, 0, 0.5);
                }
            }
        }
        return a.center.clone();
    }

    private static Location connectorWorld(Dungeon.RoomInstance r, RoomTemplate.Connector c) {
        int[] vec = RoomTemplate.rotate(c.x - (int)Math.round(r.template.getCenterX()),
                c.z - (int)Math.round(r.template.getCenterZ()), r.rotation);
        return r.center.clone().add(vec[0], c.bottomY - r.template.getConnectorMinY(), vec[1]);
    }

    private static Direction rotate(Direction dir, int rotation) {
        int ord = (dir.ordinal() + rotation) & 3;
        return Direction.values()[ord];
    }
}
