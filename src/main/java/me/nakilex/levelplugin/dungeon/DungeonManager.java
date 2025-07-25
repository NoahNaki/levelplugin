package me.nakilex.levelplugin.dungeon;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Loads room templates from the flatland world and allows generating
 * simple dungeons using hallway pieces.
 */
public class DungeonManager {
    private final Main plugin;
    private final Map<String, Dungeon> dungeons = new HashMap<>();

    private RoomTemplate deadEnd;
    private RoomTemplate straight;
    private RoomTemplate corner;
    private RoomTemplate tJunction;
    private RoomTemplate crossroad;

    /** spacing between cell centers */
    private int step;

    public DungeonManager(Main plugin) {
        this.plugin = plugin;
        loadTemplates();
    }

    private void loadTemplates() {
        World world = Bukkit.getWorld("flatland");
        if (world == null) {
            plugin.getLogger().warning("Flatland world not found for dungeon templates.");
            return;
        }
        deadEnd = RoomTemplate.capture(world, 145, -58, -4781, 125, -60, -4761);
        straight = RoomTemplate.capture(world, 125, -58, -4849, -145, -60, -4869);
        corner = RoomTemplate.capture(world, 145, -58, -4803, 125, -60, -4783);
        tJunction = RoomTemplate.capture(world, 145, -60, -4825, 125, -58, -4805);
        crossroad = RoomTemplate.capture(world, 145, -58, -4827, 125, -60, -4847);

        // Determine spacing using crossroad connectors
        List<RoomTemplate.Connector> con = crossroad.getConnectors();
        int eastX = 0, westX = 0, northZ = 0, southZ = 0;
        for (RoomTemplate.Connector c : con) {
            switch (c.facing) {
                case EAST -> eastX = c.x;
                case WEST -> westX = c.x;
                case NORTH -> northZ = c.z;
                case SOUTH -> southZ = c.z;
            }
        }
        step = Math.max(Math.abs(eastX - westX), Math.abs(southZ - northZ));
        if (step <= 0) step = crossroad.getWidth() - 1;
    }

    public boolean createDungeon(Player player, String name, int rooms) {
        if (dungeons.containsKey(name.toLowerCase())) return false;
        if (crossroad == null) return false;
        Location origin = player.getLocation();
        Dungeon dungeon = new Dungeon(player.getWorld(), name);

        Random rand = new Random();
        Map<Point, Set<Direction>> graph = new HashMap<>();
        Point cur = new Point(0, 0);
        graph.putIfAbsent(cur, new HashSet<>());

        for (int i = 1; i < rooms; i++) {
            Direction dir = Direction.random(rand);
            Point next = cur.move(dir);
            graph.putIfAbsent(next, new HashSet<>());
            graph.get(cur).add(dir);
            graph.get(next).add(dir.opposite());
            cur = next;
        }

        for (var entry : graph.entrySet()) {
            Point p = entry.getKey();
            Set<Direction> dirs = entry.getValue();
            RoomTemplate templ = chooseTemplate(dirs);
            int rotation = findRotation(templ, dirs);
            Location center = origin.clone().add(p.x * step, 0, p.z * step);
            pasteRoom(dungeon, templ, rotation, center);
        }

        dungeons.put(name.toLowerCase(), dungeon);
        return true;
    }

    private RoomTemplate chooseTemplate(Set<Direction> dirs) {
        switch (dirs.size()) {
            case 1 -> { return deadEnd; }
            case 2 -> {
                boolean opp = dirs.contains(Direction.NORTH) && dirs.contains(Direction.SOUTH)
                        || dirs.contains(Direction.EAST) && dirs.contains(Direction.WEST);
                return opp ? straight : corner;
            }
            case 3 -> { return tJunction; }
            case 4 -> { return crossroad; }
            default -> { return straight; }
        }
    }

    private int findRotation(RoomTemplate template, Set<Direction> target) {
        for (int r = 0; r < 4; r++) {
            if (template.getRotatedDirections(r).equals(target)) return r;
        }
        return 0;
    }

    private void pasteRoom(Dungeon dungeon, RoomTemplate template, int rotation, Location center) {
        World world = center.getWorld();
        if (world == null) return;
        for (RoomTemplate.BlockDef b : template.getBlocks()) {
            int[] vec = RoomTemplate.rotate(b.x - (int)Math.round(template.getCenterX()),
                    b.z - (int)Math.round(template.getCenterZ()), rotation);
            int wx = center.getBlockX() + vec[0];
            int wy = center.getBlockY() + (b.y - template.getMinY());
            int wz = center.getBlockZ() + vec[1];
            world.getBlockAt(wx, wy, wz).setBlockData(b.data, false);
        }
        dungeon.addRoom(new Dungeon.RoomInstance(template, rotation, center.clone()));
    }

    public boolean deleteDungeon(String name) {
        Dungeon d = dungeons.remove(name.toLowerCase());
        if (d == null) return false;
        d.delete();
        return true;
    }

    private record Point(int x, int z) {
        Point move(Direction dir) {
            return switch (dir) {
                case NORTH -> new Point(x, z - 1);
                case SOUTH -> new Point(x, z + 1);
                case EAST -> new Point(x + 1, z);
                case WEST -> new Point(x - 1, z);
            };
        }
    }
}
