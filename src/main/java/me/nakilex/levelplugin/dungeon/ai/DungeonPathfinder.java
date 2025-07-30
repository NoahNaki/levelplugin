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

    public static List<Location> computePath(Dungeon dungeon, DungeonManager manager) {
        Dungeon.RoomInstance start = null;
        Dungeon.RoomInstance end = null;
        for (Dungeon.RoomInstance r : dungeon.getRooms()) {
            if (r.template == manager.getEntrance()) start = r;
            if (r.template == manager.getExit()) end = r;
        }
        if (start == null || end == null) return List.of();

        Map<Dungeon.RoomInstance, List<Dungeon.RoomInstance>> graph = buildGraph(dungeon);

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
        if (!prev.containsKey(end)) return List.of();

        List<Location> route = new ArrayList<>();
        // start at the entrance center
        route.add(start.center.clone().add(0.5, 0, 0.5));
        // build connector path from start->end
        java.util.LinkedList<Location> connectors = new java.util.LinkedList<>();
        for (Dungeon.RoomInstance cur = end; prev.get(cur) != null; cur = prev.get(cur)) {
            Dungeon.RoomInstance from = prev.get(cur);
            Location conn = connectionPointBetween(from, cur);
            connectors.addFirst(conn);
        }
        route.addAll(connectors);
        // finally move to the exit center
        route.add(end.center.clone().add(0.5, 0, 0.5));
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
