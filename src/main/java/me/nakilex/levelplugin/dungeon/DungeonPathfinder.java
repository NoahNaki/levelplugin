package me.nakilex.levelplugin.dungeon;


import java.util.*;
import java.util.function.Predicate;

/**
 * Simple BFS-based pathfinding for dungeon rooms.
 */
public class DungeonPathfinder {

    /**
     * Find a path of connected rooms starting from {@code start} that
     * satisfies the {@code goal} predicate.
     *
     * @param dungeon dungeon instance to search
     * @param step distance between room centers
     * @param start starting room
     * @param goal predicate returning true for the target room
     * @return ordered list of rooms from start to goal or empty if none found
     */
    public static List<Dungeon.RoomInstance> findPath(Dungeon dungeon, int step,
                                                      Dungeon.RoomInstance start,
                                                      Predicate<Dungeon.RoomInstance> goal) {
        if (start == null) return List.of();

        Map<Loc, Dungeon.RoomInstance> byPos = new HashMap<>();
        for (Dungeon.RoomInstance r : dungeon.getRooms()) {
            byPos.put(new Loc(r.center.getBlockX(), r.center.getBlockZ()), r);
        }

        Map<Dungeon.RoomInstance, List<Dungeon.RoomInstance>> graph = new HashMap<>();
        for (Dungeon.RoomInstance r : dungeon.getRooms()) {
            List<Dungeon.RoomInstance> list = new ArrayList<>();
            Set<Direction> dirs = r.template.getRotatedDirections(r.rotation);
            int x = r.center.getBlockX();
            int z = r.center.getBlockZ();
            for (Direction d : dirs) {
                int nx = x;
                int nz = z;
                switch (d) {
                    case NORTH -> nz -= step;
                    case SOUTH -> nz += step;
                    case EAST -> nx += step;
                    case WEST -> nx -= step;
                }
                Dungeon.RoomInstance other = byPos.get(new Loc(nx, nz));
                if (other != null &&
                    other.template.getRotatedDirections(other.rotation).contains(d.opposite())) {
                    list.add(other);
                }
            }
            graph.put(r, list);
        }

        Set<Dungeon.RoomInstance> visited = new HashSet<>();
        Map<Dungeon.RoomInstance, Dungeon.RoomInstance> parent = new HashMap<>();
        Deque<Dungeon.RoomInstance> deque = new ArrayDeque<>();
        deque.add(start);
        visited.add(start);

        while (!deque.isEmpty()) {
            Dungeon.RoomInstance cur = deque.removeFirst();
            if (goal.test(cur)) {
                List<Dungeon.RoomInstance> path = new ArrayList<>();
                for (Dungeon.RoomInstance r = cur; r != null; r = parent.get(r)) {
                    path.add(0, r);
                }
                return path;
            }
            for (Dungeon.RoomInstance n : graph.getOrDefault(cur, List.of())) {
                if (visited.add(n)) {
                    parent.put(n, cur);
                    deque.add(n);
                }
            }
        }
        return List.of();
    }

    private record Loc(int x, int z) {}
}
