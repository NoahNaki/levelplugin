package me.nakilex.levelplugin.dungeon;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Loads room templates from the flatland world and allows generating
 * simple dungeons using hallway pieces.
 */
public class DungeonManager {
    private final Main plugin;
    private final Map<String, Dungeon> dungeons = new HashMap<>();
    private final Map<String, DungeonLayout> layouts = new HashMap<>();
    private final DungeonEditor editor;
    private final DungeonBuilder builder;

    private RoomTemplate deadEnd;
    private RoomTemplate straight;
    private RoomTemplate corner;
    private RoomTemplate tJunction;
    private RoomTemplate crossroad;
    private RoomTemplate entrance;

    /** spacing between cell centers */
    private int step;

    public DungeonManager(Main plugin) {
        this.plugin = plugin;
        loadTemplates();
        this.editor = new DungeonEditor(this);
        this.builder = new DungeonBuilder(this);
        Bukkit.getPluginManager().registerEvents(editor, plugin);
        Bukkit.getPluginManager().registerEvents(builder, plugin);
    }

    private void loadTemplates() {
        World world = Bukkit.getWorld("flatland");
        if (world == null) {
            plugin.getLogger().warning("Flatland world not found for dungeon templates.");
            return;
        }
        deadEnd = RoomTemplate.capture(world, -29, -60, -5198, 11, -28, -5238);
        straight = RoomTemplate.capture(world, 11, -28, -5114, -29, -60, -5154);
        corner = RoomTemplate.capture(world, 11, -28, -5156, -29, -60, -5196);
        tJunction = RoomTemplate.capture(world, -29, -60, -5072, 11, -28, -5112);
        crossroad = RoomTemplate.capture(world, 11, -28, -5030, -29, -60, -5070);
        entrance = RoomTemplate.capture(world, 151, -60, -4849, 171, -58, -4869);

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
        Set<Point> placed = new HashSet<>();

        Point start = new Point(0, 0);
        graph.put(start, new HashSet<>());
        placed.add(start);

        while (placed.size() < rooms) {
            // pick random existing room to branch from
            Point[] arr = placed.toArray(new Point[0]);
            Point cur = arr[rand.nextInt(arr.length)];

            Direction dir = Direction.random(rand);
            Point next = cur.move(dir);

            graph.putIfAbsent(cur, new HashSet<>());
            graph.putIfAbsent(next, new HashSet<>());

            graph.get(cur).add(dir);
            graph.get(next).add(dir.opposite());

            placed.add(next);
        }

        for (var entry : graph.entrySet()) {
            Point p = entry.getKey();
            Set<Direction> dirs = entry.getValue();
            RoomTemplate templ = chooseTemplate(RoomType.HALLWAY, dirs);
            int rotation = findRotation(templ, dirs);
            Location center = origin.clone().add(p.x * step, 0, p.z * step);
            pasteRoom(dungeon, templ, rotation, center);
        }

        dungeons.put(name.toLowerCase(), dungeon);
        return true;
    }

    private RoomTemplate chooseTemplate(RoomType type, Set<Direction> dirs) {
        if (type == RoomType.ENTRANCE) return entrance;
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

    public Dungeon.RoomInstance pasteRoom(Dungeon dungeon, RoomTemplate template, int rotation, Location center) {
        World world = center.getWorld();
        if (world == null) return null;

        int baseY = center.getBlockY();
        int connectorY = template.getConnectorMinY();

        // Check overlaps using our own placed blocks instead of relying solely on
        // world state. Blocks on connector layers may overlap, while all others
        // must be free both in the world and in the dungeon occupancy set.
        for (RoomTemplate.BlockDef b : template.getBlocks()) {
            if (b.data.getMaterial() == Material.PINK_WOOL ||
                    b.data.getMaterial() == Material.REDSTONE_BLOCK) continue;
            int[] vec = RoomTemplate.rotate(b.x - (int) Math.round(template.getCenterX()),
                    b.z - (int) Math.round(template.getCenterZ()), rotation);
            int wx = center.getBlockX() + vec[0];
            int wy = baseY + (b.y - connectorY);
            int wz = center.getBlockZ() + vec[1];

            boolean connector = template.isConnectorBlock(b.x, b.y, b.z);
            Material worldMat = world.getBlockAt(wx, wy, wz).getType();

            if (connector) {
                if (!worldMat.isAir() && !dungeon.isConnectorOccupied(wx, wy, wz)) return null;
                if (dungeon.isOccupied(wx, wy, wz) && !dungeon.isConnectorOccupied(wx, wy, wz)) return null;
            } else {
                if (!worldMat.isAir()) return null;
                if (dungeon.isOccupied(wx, wy, wz)) return null;
            }
        }

        for (RoomTemplate.BlockDef b : template.getBlocks()) {
            if (b.data.getMaterial() == Material.PINK_WOOL ||
                    b.data.getMaterial() == Material.REDSTONE_BLOCK) continue;
            int[] vec = RoomTemplate.rotate(b.x - (int) Math.round(template.getCenterX()),
                    b.z - (int) Math.round(template.getCenterZ()), rotation);
            int wx = center.getBlockX() + vec[0];
            int wy = baseY + (b.y - connectorY);
            int wz = center.getBlockZ() + vec[1];
            BlockData data = RoomTemplate.rotateBlockData(b.data, rotation);
            world.getBlockAt(wx, wy, wz).setBlockData(data, false);
        }
        Dungeon.RoomInstance inst = new Dungeon.RoomInstance(template, rotation, center.clone());
        dungeon.addRoom(inst);
        return inst;
    }

    public boolean deleteDungeon(String name) {
        Dungeon d = dungeons.remove(name.toLowerCase());
        if (d == null) return false;
        d.delete();
        return true;
    }

    public void saveLayout(String name, DungeonLayout layout) {
        layouts.put(name.toLowerCase(), layout);
    }

    public boolean playDungeon(Player player, String name) {
        DungeonLayout layout = layouts.get(name.toLowerCase());
        if (layout == null) return false;
        Location origin = player.getLocation();
        Dungeon dungeon = new Dungeon(player.getWorld(), name);

        for (int x = 0; x < DungeonLayout.WIDTH; x++) {
            for (int y = 0; y < DungeonLayout.HEIGHT; y++) {
                RoomType type = layout.get(x, y);
                if (type == RoomType.NONE) continue;

                Set<Direction> dirs = new HashSet<>();
                if (layout.get(x + 1, y) != RoomType.NONE) dirs.add(Direction.EAST);
                if (layout.get(x - 1, y) != RoomType.NONE) dirs.add(Direction.WEST);
                if (layout.get(x, y + 1) != RoomType.NONE) dirs.add(Direction.SOUTH);
                if (layout.get(x, y - 1) != RoomType.NONE) dirs.add(Direction.NORTH);

                RoomTemplate templ = chooseTemplate(type, dirs);
                int rotation = findRotation(templ, dirs);
                Location center = origin.clone().add(x * step, 0, y * step);
                pasteRoom(dungeon, templ, rotation, center);
            }
        }

        dungeons.put(name.toLowerCase(), dungeon);
        return true;
    }

    public DungeonEditor getEditor() { return editor; }
    public DungeonBuilder getBuilder() { return builder; }

    public RoomTemplate getEntranceTemplate() { return entrance; }
    public RoomTemplate getDeadEndTemplate() { return deadEnd; }
    public RoomTemplate getStraightTemplate() { return straight; }
    public RoomTemplate getCornerTemplate() { return corner; }
    public RoomTemplate getTJunctionTemplate() { return tJunction; }
    public RoomTemplate getCrossroadTemplate() { return crossroad; }

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
