package me.nakilex.levelplugin.dungeon;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.TemplateType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerPortalEvent;

import java.util.*;

/**
 * Loads room templates from the flatland world and allows generating
 * simple dungeons using hallway pieces.
 */
public class DungeonManager {
    private final Main plugin;
    private final Map<String, Dungeon> dungeons = new HashMap<>();
    private final Map<String, DungeonLayout> layouts = new HashMap<>();
    private final DungeonBuilder builder;

    private RoomTemplate deadEnd;
    private RoomTemplate straight;
    private RoomTemplate cornerLeft;
    private RoomTemplate cornerRight;
    private RoomTemplate tJunction;
    private RoomTemplate tJunctionLeft;
    private RoomTemplate tJunctionRight;
    private RoomTemplate crossroad;
    private RoomTemplate hallway;
    private RoomTemplate entrance;
    private RoomTemplate boss;
    private RoomTemplate combatLeft;
    private RoomTemplate combatRight;
    private RoomTemplate library;
    private RoomTemplate exit;

    /** spacing between cell centers */
    private int step;

    private final Map<World, Instance> instances = new HashMap<>();

    public DungeonManager(Main plugin) {
        this.plugin = plugin;
        loadTemplates();
        this.builder = new DungeonBuilder(this);
        Bukkit.getPluginManager().registerEvents(builder, plugin);
        Bukkit.getPluginManager().registerEvents(new InstanceListener(), plugin);
    }

    public RoomTemplate getEntrance() { return entrance; }
    public RoomTemplate getDeadEnd() { return deadEnd; }
    public RoomTemplate getStraight() { return straight; }
    public RoomTemplate getCornerLeft() { return cornerLeft; }
    public RoomTemplate getCornerRight() { return cornerRight; }
    public RoomTemplate getTJunction() { return tJunction; }
    public RoomTemplate getTJunctionLeft() { return tJunctionLeft; }
    public RoomTemplate getTJunctionRight() { return tJunctionRight; }
    public RoomTemplate getCrossroad() { return crossroad; }
    public RoomTemplate getHallway() { return hallway; }
    public RoomTemplate getBoss() { return boss; }
    public RoomTemplate getCombatLeft() { return combatLeft; }
    public RoomTemplate getCombatRight() { return combatRight; }
    public RoomTemplate getLibrary() { return library; }
    public RoomTemplate getExit() { return exit; }
    public int getStep() { return step; }
    public Main getPlugin() { return plugin; }

    /** Return the template instance for the given identifier. */
    public RoomTemplate getTemplate(TemplateType type) {
        return switch (type) {
            case ENTRANCE -> entrance;
            case DEAD_END -> deadEnd;
            case STRAIGHT -> straight;
            case CORNER_LEFT -> cornerLeft;
            case CORNER_RIGHT -> cornerRight;
            case TJUNCTION -> tJunction;
            case TJUNCTION_LEFT -> tJunctionLeft;
            case TJUNCTION_RIGHT -> tJunctionRight;
            case CROSSROAD -> crossroad;
            case HALLWAY -> hallway;
            case BOSS -> boss;
            case COMBAT_LEFT -> combatLeft;
            case COMBAT_RIGHT -> combatRight;
            case LIBRARY -> library;
            case EXIT -> exit;
            default -> null;
        };
    }

    /** Identify which TemplateType maps to the given template instance. */
    public TemplateType identifyTemplate(RoomTemplate t) {
        if (t == entrance) return TemplateType.ENTRANCE;
        if (t == deadEnd) return TemplateType.DEAD_END;
        if (t == straight) return TemplateType.STRAIGHT;
        if (t == cornerLeft) return TemplateType.CORNER_LEFT;
        if (t == cornerRight) return TemplateType.CORNER_RIGHT;
        if (t == tJunction) return TemplateType.TJUNCTION;
        if (t == tJunctionLeft) return TemplateType.TJUNCTION_LEFT;
        if (t == tJunctionRight) return TemplateType.TJUNCTION_RIGHT;
        if (t == crossroad) return TemplateType.CROSSROAD;
        if (t == hallway) return TemplateType.HALLWAY;
        if (t == boss) return TemplateType.BOSS;
        if (t == combatLeft) return TemplateType.COMBAT_LEFT;
        if (t == combatRight) return TemplateType.COMBAT_RIGHT;
        if (t == library) return TemplateType.LIBRARY;
        if (t == exit) return TemplateType.EXIT;
        return TemplateType.NONE;
    }

    private void loadTemplates() {
        World world = Bukkit.getWorld("flatland");
        if (world == null) {
            plugin.getLogger().warning("Flatland world not found for dungeon templates.");
            return;
        }
        deadEnd = RoomTemplate.capture(world, -29, -60, -5198, 11, -28, -5238);
        straight = RoomTemplate.capture(world, 11, -28, -5114, -29, -60, -5154);
        cornerLeft = RoomTemplate.capture(world, 11, -28, -5156, -29, -60, -5196);
        cornerRight = RoomTemplate.capture(world, 11, -28, -5156, -29, -60, -5196);
        tJunction = RoomTemplate.capture(world, -29, -60, -5072, 11, -28, -5112);
        // create distinct instances so we can differentiate left/right variants
        tJunctionLeft = RoomTemplate.capture(world, -29, -60, -5072, 11, -28, -5112);
        tJunctionRight = RoomTemplate.capture(world, -29, -60, -5072, 11, -28, -5112);
        crossroad = RoomTemplate.capture(world, 11, -28, -5030, -29, -60, -5070);
        hallway = RoomTemplate.capture(world, 63, -44, -5021, 103, -21, -4981);
        entrance = deadEnd; // use the single-exit room as the entrance
        // new boss room region provided by the map builder
        boss = RoomTemplate.capture(world, 43, -14, -5006, -23, -54, -4922);
        RoomTemplate combat = RoomTemplate.capture(world, 65, -42, -5059, 105, -13, -5100);
        combatRight = combat;
        combatLeft = flipEntrances(combat);
        exit = RoomTemplate.capture(world, 91, -45, -5222, 56, -24, -5199);
        library = RoomTemplate.capture(world, 74, -11, -5172, 112, -39, -5122);

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
            pasteRoom(dungeon, templ, rotation, center, null);
        }

        dungeons.put(name.toLowerCase(), dungeon);
        return true;
    }

    public RoomTemplate chooseTemplate(RoomType type, Set<Direction> dirs) {
        if (type == RoomType.ENTRANCE) return entrance;
        if (type == RoomType.BOSS) return boss;
        if (type == RoomType.COMBAT) return combatRight;
        if (type == RoomType.LIBRARY) return library;
        if (type == RoomType.EXIT) return exit;
        if (type == RoomType.TJUNCTION_LEFT) return tJunctionLeft;
        if (type == RoomType.TJUNCTION_RIGHT) return tJunctionRight;
        switch (dirs.size()) {
            case 1 -> { return deadEnd; }
            case 2 -> {
                boolean opp = dirs.contains(Direction.NORTH) && dirs.contains(Direction.SOUTH)
                        || dirs.contains(Direction.EAST) && dirs.contains(Direction.WEST);
                return opp ? straight : cornerRight;
            }
            case 3 -> { return tJunction; }
            case 4 -> { return crossroad; }
            default -> { return straight; }
        }
    }

    public int findRotation(RoomTemplate template, Set<Direction> target) {
        for (int r = 0; r < 4; r++) {
            if (template.getRotatedDirections(r).equals(target)) return r;
        }
        return 0;
    }

    public record PasteResult(boolean success, double overlap, Map<Location, BlockData> replaced, Dungeon.RoomInstance instance) {}

    public PasteResult pasteRoom(Dungeon dungeon, RoomTemplate template, int rotation, Location center) {
        return pasteRoom(dungeon, template, rotation, center, null);
    }

    public PasteResult pasteRoom(Dungeon dungeon, RoomTemplate template, int rotation, Location center, String mob) {
        World world = center.getWorld();
        if (world == null) return new PasteResult(false, 1.0, Map.<Location, BlockData>of(), null);

        int baseY = center.getBlockY();
        int connectorY = template.getConnectorMinY();
        int total = 0;
        int collisions = 0;

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (RoomTemplate.BlockDef b : template.getBlocks()) {
            Material mat = b.data.getMaterial();
            if (mat == Material.REDSTONE_BLOCK || mat == Material.PINK_WOOL || mat == Material.LIME_WOOL) continue;
            int[] vec = RoomTemplate.rotate(b.x - (int) Math.round(template.getCenterX()),
                    b.z - (int) Math.round(template.getCenterZ()), rotation);
            int wx = center.getBlockX() + vec[0];
            int wy = baseY + (b.y - connectorY);
            int wz = center.getBlockZ() + vec[1];
            if (world.getBlockAt(wx, wy, wz).getType() != Material.AIR) {
                // ignore connector layers
                if (b.y - connectorY > 2) collisions++;
            }
            if (wx < minX) minX = wx;
            if (wy < minY) minY = wy;
            if (wz < minZ) minZ = wz;
            if (wx > maxX) maxX = wx;
            if (wy > maxY) maxY = wy;
            if (wz > maxZ) maxZ = wz;
            total++;
        }
        double overlap = total == 0 ? 0.0 : (double) collisions / total;
        if (overlap > 0.10) {
            return new PasteResult(false, overlap, Map.<Location, BlockData>of(), null);
        }

        Map<Location, BlockData> replaced = new HashMap<>();

        for (RoomTemplate.BlockDef b : template.getBlocks()) {
            Material mat = b.data.getMaterial();
            if (mat == Material.REDSTONE_BLOCK || mat == Material.PINK_WOOL || mat == Material.LIME_WOOL) continue;
            int[] vec = RoomTemplate.rotate(b.x - (int) Math.round(template.getCenterX()),
                    b.z - (int) Math.round(template.getCenterZ()), rotation);
            int wx = center.getBlockX() + vec[0];
            int wy = baseY + (b.y - connectorY);
            int wz = center.getBlockZ() + vec[1];
            Location l = new Location(world, wx, wy, wz);
            replaced.put(l, world.getBlockAt(wx, wy, wz).getBlockData());
            BlockData data = RoomTemplate.rotateBlockData(b.data, rotation);
            world.getBlockAt(wx, wy, wz).setBlockData(data, false);
        }
        Dungeon.RoomInstance inst = new Dungeon.RoomInstance(template, rotation, center.clone(),
                minX, minY, minZ, maxX, maxY, maxZ, mob);
        dungeon.addRoom(inst);
        return new PasteResult(true, overlap, replaced, inst);
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

    public DungeonLayout getLayout(String name) {
        return layouts.get(name.toLowerCase());
    }

    public void startInstance(Player player, String name) {
        DungeonLayout layout = layouts.get(name.toLowerCase());
        if (layout == null) {
            player.sendMessage(ChatColor.RED + "Dungeon not found.");
            return;
        }
        String worldName = "dgn_" + name.toLowerCase() + "_" + System.currentTimeMillis();
        java.io.File container = plugin.getServer().getWorldContainer();
        java.io.File tmpDir = new java.io.File(container, "tmp_dungeons");
        tmpDir.mkdirs();
        org.bukkit.WorldCreator wc = new org.bukkit.WorldCreator("tmp_dungeons/" + worldName);
        wc.generator(new VoidWorldGenerator());
        World world = Bukkit.createWorld(wc);
        if (world == null) return;
        Location origin = new Location(world, 0, 64, 0);
        Dungeon dungeon = new Dungeon(world, name);

        for (int x = 0; x < DungeonLayout.WIDTH; x++) {
            for (int y = 0; y < DungeonLayout.HEIGHT; y++) {
                RoomType type = layout.get(x, y);
                if (type == RoomType.NONE) continue;
                RoomTemplate templ = getTemplate(layout.getTemplate(x, y));
                int rotation = layout.getRotation(x, y);
                int diffX = layout.getOffsetX(x, y);
                int diffZ = layout.getOffsetZ(x, y);
                Location center = origin.clone().add(diffX, 0, diffZ);
                String mob = layout.getMob(x, y);
                pasteRoom(dungeon, templ, rotation, center, mob);
            }
        }

        Instance inst = new Instance(dungeon, name);
        inst.returnLocations.put(player.getUniqueId(), player.getLocation());
        instances.put(world, inst);
        world.setDifficulty(org.bukkit.Difficulty.NORMAL);
        world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, true);
        player.teleport(origin);
    }

    public void cleanupInstances() {
        for (World w : new java.util.ArrayList<>(instances.keySet())) {
            removeWorld(w);
        }
    }

    public void cleanupOldInstanceWorlds() {
        java.io.File container = plugin.getServer().getWorldContainer();
        java.io.File folder = new java.io.File(container, "tmp_dungeons");
        java.io.File[] dirs = folder.listFiles((f) -> f.isDirectory() && f.getName().startsWith("dgn_"));
        if (dirs != null) {
            for (java.io.File d : dirs) {
                if (Bukkit.getWorld(d.getName()) != null) continue;
                deleteDir(d);
            }
        }
    }

    private void deleteDir(java.io.File dir) {
        if (dir.isDirectory()) {
            for (java.io.File f : dir.listFiles()) deleteDir(f);
        }
        dir.delete();
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

                RoomTemplate templ = getTemplate(layout.getTemplate(x, y));
                int rotation = layout.getRotation(x, y);
                int diffX = layout.getOffsetX(x, y);
                int diffZ = layout.getOffsetZ(x, y);
                Location center = origin.clone().add(diffX, 0, diffZ);
                String mob = layout.getMob(x, y);
                pasteRoom(dungeon, templ, rotation, center, mob);
            }
        }

        dungeons.put(name.toLowerCase(), dungeon);
        return true;
    }

    public DungeonBuilder getBuilder() { return builder; }

    public Collection<Dungeon> getActiveDungeons() {
        java.util.List<Dungeon> list = new java.util.ArrayList<>(dungeons.values());
        for (Instance inst : instances.values()) {
            list.add(inst.dungeon);
        }
        return list;
    }

    public Set<String> getAvailableMobs() {
        var sec = plugin.getMobRewardsConfig().getConfig().getConfigurationSection("mobs");
        if (sec == null) return Set.of();
        return sec.getKeys(false);
    }

    public Set<String> getLayoutNames() {
        return layouts.keySet();
    }

    private static RoomTemplate flipEntrances(RoomTemplate src) {
        List<RoomTemplate.Connector> list = new ArrayList<>();
        for (RoomTemplate.Connector c : src.getConnectors()) {
            list.add(new RoomTemplate.Connector(c.x, c.z, c.bottomY, c.facing, !c.entrance));
        }
        return new RoomTemplate(src.getBlocks(), list, src.getWidth(), src.getHeight(), src.getDepth(), src.getMinY());
    }

    private static class Instance {
        final Dungeon dungeon;
        final String layout;
        final Map<java.util.UUID, Location> returnLocations = new HashMap<>();
        Instance(Dungeon d, String layout) { this.dungeon = d; this.layout = layout; }
    }

    private class InstanceListener implements org.bukkit.event.Listener {

        @org.bukkit.event.EventHandler(priority = EventPriority.HIGHEST)
        public void onPortal(PlayerPortalEvent e) {
            Instance inst = instances.get(e.getFrom().getWorld());
            if (inst == null) return;
            e.setCancelled(true);
            java.util.UUID id = e.getPlayer().getUniqueId();
            Location back = inst.returnLocations.remove(id);
            if (back != null) {
                e.getPlayer().sendMessage(ChatColor.GREEN + "Dungeon '" + inst.layout + "' completed!");
                e.getPlayer().teleport(back);
            }
            checkInstance(e.getFrom().getWorld());
        }

        @org.bukkit.event.EventHandler
        public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) {
            Instance inst = instances.get(e.getPlayer().getWorld());
            if (inst == null) return;
            inst.returnLocations.remove(e.getPlayer().getUniqueId());
            checkInstance(e.getPlayer().getWorld());
        }
    }

    private void checkInstance(World world) {
        Instance inst = instances.get(world);
        if (inst != null && inst.returnLocations.isEmpty()) {
            removeWorld(world);
        }
    }

    private void removeWorld(World world) {
        instances.remove(world);
        Bukkit.unloadWorld(world, false);
        deleteDir(world.getWorldFolder());
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
