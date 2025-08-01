package me.nakilex.levelplugin.dungeon;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.TemplateType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import me.nakilex.levelplugin.utils.FileUtil;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.WorldType;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.entity.TextDisplay;
import com.destroystokyo.paper.profile.PlayerProfile;
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
    private final Map<String, String> layoutDisplay = new HashMap<>();
    private final Map<String, Integer> layoutThreat = new HashMap<>();
    private java.io.File layoutFile;
    private org.bukkit.configuration.file.FileConfiguration layoutConfig;
    /** Guard asynchronous layout saves. */
    private final Object saveLock = new Object();
    private final DungeonBuilder builder;
    private final me.nakilex.levelplugin.lootchests.managers.LootChestManager lootChestManager;

    /**
     * Normalize a dungeon name for storage/lookup.
     * <p>
     * Spaces become underscores and the result is lower case so
     * names typed as "Frost Palace" map to a key of
     * {@code frost_palace}.
     */
    public static String normalizeKey(String name) {
        if (name == null) return "";
        String cleaned = org.bukkit.ChatColor.stripColor(name);
        cleaned = cleaned.replaceAll("\\s+", "_");
        cleaned = cleaned.toLowerCase();
        return cleaned;
    }

    private RoomTemplate deadEnd;
    private RoomTemplate straight;
    private RoomTemplate cornerLeft;
    private RoomTemplate cornerRight;
    private RoomTemplate tJunction;
    private RoomTemplate tJunctionLeft;
    private RoomTemplate tJunctionRight;
    private RoomTemplate crossroad;
    private RoomTemplate hallway;
    private RoomTemplate treasureLeft;
    private RoomTemplate treasureTRight;
    private RoomTemplate decorStone;
    private RoomTemplate decorChest;
    private RoomTemplate entrance;
    private RoomTemplate boss;
    private RoomTemplate combatLeft;
    private RoomTemplate combatRight;
    private RoomTemplate library;
    private RoomTemplate exit;

    /** spacing between cell centers */
    private int step;

    private final Map<World, Instance> instances = new HashMap<>();

    /** Return true if the given world is an active dungeon instance. */
    public boolean isInstanceWorld(World world) {
        return instances.containsKey(world);
    }

    public DungeonManager(Main plugin, me.nakilex.levelplugin.lootchests.managers.LootChestManager lootChestManager) {
        this.plugin = plugin;
        this.lootChestManager = lootChestManager;
        loadTemplates();
        loadLayouts();
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
    public RoomTemplate getTreasureLeft() { return treasureLeft; }
    public RoomTemplate getTreasureTRight() { return treasureTRight; }
    public RoomTemplate getDecorStone() { return decorStone; }
    public RoomTemplate getDecorChest() { return decorChest; }
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
            case TREASURE_LEFT -> treasureLeft;
            case TREASURE_T_RIGHT -> treasureTRight;
            case DECOR_STONE -> decorStone;
            case DECOR_CHEST -> decorChest;
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
        if (t == treasureLeft) return TemplateType.TREASURE_LEFT;
        if (t == treasureTRight) return TemplateType.TREASURE_T_RIGHT;
        if (t == decorStone) return TemplateType.DECOR_STONE;
        if (t == decorChest) return TemplateType.DECOR_CHEST;
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
        deadEnd = RoomTemplate.capture(world, -29, -60, -5198, 11, -28, -5238, false);
        straight = RoomTemplate.capture(world, 11, -28, -5114, -29, -60, -5154, false);
        cornerLeft = RoomTemplate.capture(world, 11, -28, -5156, -29, -60, -5196, false);
        cornerRight = RoomTemplate.capture(world, 11, -28, -5156, -29, -60, -5196, false);
        tJunction = RoomTemplate.capture(world, -29, -60, -5072, 11, -28, -5112, false);
        // create distinct instances so we can differentiate left/right variants
        tJunctionLeft = RoomTemplate.capture(world, -29, -60, -5072, 11, -28, -5112, false);
        tJunctionRight = RoomTemplate.capture(world, -29, -60, -5072, 11, -28, -5112, false);
        crossroad = RoomTemplate.capture(world, 11, -28, -5030, -29, -60, -5070, false);
        // entrance template with exit portals and marker hologram
        // updated region as provided by the builder
        entrance = RoomTemplate.capture(world, -212, 77, -5334, -125, -36, -5227, true);
        // new boss room region provided by the map builder
        boss = RoomTemplate.capture(world, 43, -14, -5006, -23, -54, -4922, false);
        RoomTemplate combat = RoomTemplate.capture(world, 65, -42, -5059, 105, -13, -5100, false);
        combatRight = combat;
        combatLeft = flipEntrances(combat);
        exit = RoomTemplate.capture(world, 91, -45, -5222, 56, -24, -5199, false);
        library = RoomTemplate.capture(world, 74, -11, -5172, 112, -39, -5122, false);
        hallway = RoomTemplate.capture(world, 63, -44, -5021, 89, -21, -5003, false);
        // Provided by the map builder: 107,-57,-4991 to 69,-28,-4952
        treasureLeft = RoomTemplate.capture(world, 107, -57, -4991, 69, -28, -4952, false);
        treasureTRight = RoomTemplate.capture(world, 85, -51, -5292, 45, -22, -5253, false);
        decorStone = RoomTemplate.capture(world, 89, -29, -4921, 71, -55, -4906, false);
        decorChest = RoomTemplate.capture(world, 119, -52, -4900, 104, -29, -4918, false);

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
        String key = normalizeKey(name);
        if (dungeons.containsKey(key)) return false;
        if (crossroad == null) return false;
        Location origin = player.getLocation();
        Dungeon dungeon = new Dungeon(player.getWorld(), name);

        long debugStart = System.currentTimeMillis();

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

        player.sendMessage(ChatColor.GRAY + "[Debug] Graph in "
                + (System.currentTimeMillis() - debugStart) + "ms");
        long buildStart = System.currentTimeMillis();

        for (var entry : graph.entrySet()) {
            Point p = entry.getKey();
            Set<Direction> dirs = entry.getValue();
            RoomTemplate templ = chooseTemplate(RoomType.HALLWAY, dirs);
            int rotation = findRotation(templ, dirs);
            Location center = origin.clone().add(p.x * step, 0, p.z * step);
            pasteRoom(dungeon, templ, rotation, center, null, false);
        }

        dungeons.put(key, dungeon);
        player.sendMessage(ChatColor.GRAY + "[Debug] Built in "
                + (System.currentTimeMillis() - buildStart) + "ms");
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
        return pasteRoom(dungeon, template, rotation, center, null, false);
    }

    public PasteResult pasteRoom(Dungeon dungeon, RoomTemplate template, int rotation, Location center, String mob) {
        return pasteRoom(dungeon, template, rotation, center, mob, false);
    }

    public PasteResult pasteRoom(Dungeon dungeon, RoomTemplate template, int rotation, Location center, String mob, boolean preview) {
        World world = center.getWorld();
        if (world == null) return new PasteResult(false, 1.0, Map.<Location, BlockData>of(), null);

        int baseY = center.getBlockY();
        int connectorY = template.getConnectorMinY();
        int collisions = 0;
        int total = 0;
        double overlap = 0.0;

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        if (preview) {
            for (RoomTemplate.BlockDef b : template.getBlocks()) {
                Material mat = b.data.getMaterial();
                if (mat == Material.REDSTONE_BLOCK || mat == Material.PINK_WOOL || mat == Material.LIME_WOOL) continue;
                int[] vec = RoomTemplate.rotate(b.x - (int) Math.round(template.getCenterX()),
                        b.z - (int) Math.round(template.getCenterZ()), rotation);
                int wx = center.getBlockX() + vec[0];
                int wy = baseY + (b.y - connectorY);
                int wz = center.getBlockZ() + vec[1];
                if (world.getBlockAt(wx, wy, wz).getType() != Material.AIR) {
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
            overlap = total == 0 ? 0.0 : (double) collisions / total;
            if (overlap > 0.10) {
                return new PasteResult(false, overlap, Map.of(), null);
            }
        } else {
            // bounds only
            for (RoomTemplate.BlockDef b : template.getBlocks()) {
                Material mat = b.data.getMaterial();
                if (mat == Material.REDSTONE_BLOCK || mat == Material.PINK_WOOL || mat == Material.LIME_WOOL) continue;
                int[] vec = RoomTemplate.rotate(b.x - (int) Math.round(template.getCenterX()),
                        b.z - (int) Math.round(template.getCenterZ()), rotation);
                int wx = center.getBlockX() + vec[0];
                int wy = baseY + (b.y - connectorY);
                int wz = center.getBlockZ() + vec[1];
                if (wx < minX) minX = wx;
                if (wy < minY) minY = wy;
                if (wz < minZ) minZ = wz;
                if (wx > maxX) maxX = wx;
                if (wy > maxY) maxY = wy;
                if (wz > maxZ) maxZ = wz;
            }
        }

        Map<Location, BlockData> replaced = preview ? new HashMap<>() : Map.of();
        java.util.List<Location> chestLocs = new java.util.ArrayList<>();
        Location bossLoc = null;

        for (RoomTemplate.BlockDef b : template.getBlocks()) {
            Material mat = b.data.getMaterial();
            if (mat == Material.REDSTONE_BLOCK || mat == Material.PINK_WOOL || mat == Material.LIME_WOOL) continue;
            int[] vec = RoomTemplate.rotate(b.x - (int) Math.round(template.getCenterX()),
                    b.z - (int) Math.round(template.getCenterZ()), rotation);
            int wx = center.getBlockX() + vec[0];
            int wy = baseY + (b.y - connectorY);
            int wz = center.getBlockZ() + vec[1];
            Location l = new Location(world, wx, wy, wz);
            if (preview) replaced.put(l, world.getBlockAt(wx, wy, wz).getBlockData());
            BlockData data = RoomTemplate.rotateBlockData(b.data, rotation);
            world.getBlockAt(wx, wy, wz).setBlockData(data, false);
            Material placed = data.getMaterial();
            if (placed == Material.CHEST || placed == Material.TRAPPED_CHEST) {
                chestLocs.add(l);
            }
            if (b.profile != null) {
                var state = world.getBlockAt(wx, wy, wz).getState();
                if (state instanceof Skull skull) {
                    skull.setPlayerProfile(b.profile);
                    skull.update(false, false);
                }
            }
        }
        if (template.getBossSpawn() != null) {
            int[] vec = RoomTemplate.rotate(template.getBossSpawn().x - (int) Math.round(template.getCenterX()),
                    template.getBossSpawn().z - (int) Math.round(template.getCenterZ()), rotation);
            int wx = center.getBlockX() + vec[0];
            int wy = baseY + (template.getBossSpawn().y - connectorY);
            int wz = center.getBlockZ() + vec[1];
            bossLoc = new Location(world, wx + 0.5, wy, wz + 0.5);
        }
        // place nether portals and exit holograms
        for (RoomTemplate.Marker m : template.getPortals()) {
            int[] vec = RoomTemplate.rotate(m.x - (int) Math.round(template.getCenterX()),
                    m.z - (int) Math.round(template.getCenterZ()), rotation);
            int wx = center.getBlockX() + vec[0];
            int wy = baseY + (m.y - connectorY);
            int wz = center.getBlockZ() + vec[1];
            Location loc = new Location(world, wx, wy, wz);
            if (preview) replaced.put(loc, world.getBlockAt(wx, wy, wz).getBlockData());
            world.getBlockAt(wx, wy, wz).setType(Material.NETHER_PORTAL, false);
        }
        if (template == entrance) {
            for (RoomTemplate.Marker m : template.getExitMarkers()) {
                int[] vec = RoomTemplate.rotate(m.x - (int) Math.round(template.getCenterX()),
                        m.z - (int) Math.round(template.getCenterZ()), rotation);
                Location loc = center.clone().add(vec[0] + 0.5, m.y - connectorY + 1.2, vec[1] + 0.5);
                TextDisplay td = (TextDisplay) world.spawn(loc, TextDisplay.class);
                td.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
                td.setText(ChatColor.RED.toString() + ChatColor.BOLD + "EXIT");
                td.setShadowRadius(0);
                td.setShadowStrength(0);
                td.addScoreboardTag("dungeon_exit");
            }
        }
        Dungeon.RoomInstance inst = new Dungeon.RoomInstance(template, rotation, center.clone(),
                minX, minY, minZ, maxX, maxY, maxZ, mob, chestLocs, bossLoc);
        dungeon.addRoom(inst);
        return new PasteResult(true, overlap, replaced, inst);
    }

    public boolean deleteDungeon(String name) {
        String key = normalizeKey(name);
        boolean removed = false;
        Dungeon d = dungeons.remove(key);
        if (d != null) {
            d.delete();
            removed = true;
        }
        if (layouts.remove(key) != null) {
            layoutDisplay.remove(key);
            saveLayouts();
            removed = true;
        }
        return removed;
    }

    public boolean layoutExists(String name) {
        String key = normalizeKey(name);
        if (layouts.containsKey(key)) return true;
        for (String disp : layoutDisplay.values()) {
            if (disp.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public void saveLayout(String key, String displayName, DungeonLayout layout) {
        saveLayout(null, key, displayName, layout);
    }

    /** Save a layout and report timing to the player if provided. */
    public void saveLayout(Player player, String key, String displayName, DungeonLayout layout) {
        String lower = normalizeKey(key);
        layouts.put(lower, layout);
        layoutDisplay.put(lower, displayName);

        if (player == null) {
            saveLayouts();
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long start = System.currentTimeMillis();
            synchronized (saveLock) {
                saveLayoutsInternal();
            }
            long total = System.currentTimeMillis() - start;
            plugin.getLogger().info("[Dungeon] Layouts saved in " + total + "ms");
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(ChatColor.GRAY + "[Debug] Layout saved in " + total + "ms"));
        });
    }

    public DungeonLayout getLayout(String name) {
        String key = normalizeKey(name);
        DungeonLayout layout = layouts.get(key);
        if (layout == null) {
            layout = layouts.get(name.toLowerCase());
        }
        if (layout == null) {
            for (var entry : layoutDisplay.entrySet()) {
                if (entry.getValue().equalsIgnoreCase(name)) {
                    layout = layouts.get(entry.getKey());
                    break;
                }
            }
        }
        return layout;
    }

    public Set<Map.Entry<String, String>> getLayoutEntries() {
        return layoutDisplay.entrySet();
    }

    public String getDisplayName(String key) {
        return layoutDisplay.getOrDefault(normalizeKey(key), key);
    }

    public int getThreatLevel(String key) {
        return layoutThreat.getOrDefault(normalizeKey(key), 1);
    }

    private void loadLayouts() {
        layoutFile = new java.io.File(plugin.getDataFolder(), "dungeons.yml");
        if (!layoutFile.exists()) {
            try { layoutFile.createNewFile(); } catch (Exception ignored) {}
        }
        layoutConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(layoutFile);
        org.bukkit.configuration.ConfigurationSection root = layoutConfig.getConfigurationSection("layouts");
        if (root == null) return;
        for (String rawKey : root.getKeys(false)) {
            org.bukkit.configuration.ConfigurationSection sec = root.getConfigurationSection(rawKey);
            if (sec == null) continue;
            String display = sec.getString("display", rawKey);
            int stepVal = sec.getInt("step", 0);
            DungeonLayout layout = new DungeonLayout();
            layout.setStep(stepVal);
            org.bukkit.configuration.ConfigurationSection cells = sec.getConfigurationSection("cells");
            if (cells != null) {
                for (String coord : cells.getKeys(false)) {
                    org.bukkit.configuration.ConfigurationSection cell = cells.getConfigurationSection(coord);
                    if (cell == null) continue;
                    String[] parts = coord.split(",");
                    if (parts.length != 2) continue;
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    RoomType type = RoomType.valueOf(cell.getString("type", "NONE"));
                    TemplateType t = TemplateType.valueOf(cell.getString("template", "NONE"));
                    int rot = cell.getInt("rotation", 0);
                    String mob = cell.getString("mob", null);
                    int offX = cell.getInt("offsetX", 0);
                    int offZ = cell.getInt("offsetZ", 0);
                    layout.set(x, y, type);
                    layout.setTemplate(x, y, t);
                    layout.setRotation(x, y, rot);
                    layout.setMob(x, y, mob);
                    int threat = cell.getInt("threat", 0);
                    layout.setThreat(x, y, threat);
                    layout.setOffset(x, y, offX, offZ);
                }
            }
            String key = normalizeKey(rawKey);
            layouts.put(key, layout);
            layoutDisplay.put(key, display);
            layoutThreat.put(key, layout.getMaxThreat());
        }
    }

    public void saveLayouts() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long start = System.currentTimeMillis();
            synchronized (saveLock) {
                saveLayoutsInternal();
            }
            long total = System.currentTimeMillis() - start;
            plugin.getLogger().info("[Dungeon] Layouts saved in " + total + "ms");
        });
    }

    /** Synchronously save layouts, used on shutdown. */
    public void saveLayoutsSync() {
        long start = System.currentTimeMillis();
        synchronized (saveLock) {
            saveLayoutsInternal();
        }
        plugin.getLogger().info("[Dungeon] Layouts saved in " + (System.currentTimeMillis() - start) + "ms");
    }

    private void saveLayoutsInternal() {
        long start = System.currentTimeMillis();

        if (layoutFile == null) {
            layoutFile = new java.io.File(plugin.getDataFolder(), "dungeons.yml");
        }
        if (layoutConfig == null) {
            layoutConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(layoutFile);
        }

        long init = System.currentTimeMillis();

        layoutConfig.set("layouts", null);
        org.bukkit.configuration.ConfigurationSection root = layoutConfig.createSection("layouts");
        for (String key : layouts.keySet()) {
            DungeonLayout layout = layouts.get(key);
            org.bukkit.configuration.ConfigurationSection sec = root.createSection(key);
            sec.set("display", layoutDisplay.getOrDefault(key, key));
            sec.set("step", layout.getStep());
            org.bukkit.configuration.ConfigurationSection cells = sec.createSection("cells");
            for (int x = 0; x < DungeonLayout.WIDTH; x++) {
                for (int y = 0; y < DungeonLayout.HEIGHT; y++) {
                    RoomType type = layout.get(x, y);
                    if (type == RoomType.NONE) continue;
                    TemplateType t = layout.getTemplate(x, y);
                    int rot = layout.getRotation(x, y);
                    String mob = layout.getMob(x, y);
                    int offX = layout.getOffsetX(x, y);
                    int offZ = layout.getOffsetZ(x, y);
                    org.bukkit.configuration.ConfigurationSection cell = cells.createSection(x + "," + y);
                    cell.set("type", type.name());
                    cell.set("template", t.name());
                    cell.set("rotation", rot);
                    if (mob != null) cell.set("mob", mob);
                    cell.set("threat", layout.getThreat(x, y));
                    cell.set("offsetX", offX);
                    cell.set("offsetZ", offZ);
                }
            }
            layoutThreat.put(key, layout.getMaxThreat());
        }

        long built = System.currentTimeMillis();

        try {
            layoutConfig.save(layoutFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        long saved = System.currentTimeMillis();
        plugin.getLogger().info("[Dungeon] Save debug -> init:" + (init - start)
                + "ms, build:" + (built - init) + "ms, disk:" + (saved - built) + "ms");
    }

    public void startInstance(Player player, String name) {
        String keyName = normalizeKey(name);
        DungeonLayout layout = getLayout(name);
        if (layout == null) {
            player.sendMessage(ChatColor.RED + "Dungeon not found.");
            return;
        }
        long debugStart = System.currentTimeMillis();
        String worldName = "dgn_" + keyName + "_" + System.currentTimeMillis();
        org.bukkit.WorldCreator wc = new org.bukkit.WorldCreator(worldName);
        wc.generator(new VoidWorldGenerator());
        wc.type(WorldType.FLAT);
        wc.generateStructures(false);
        World world = Bukkit.createWorld(wc);
        if (world != null) {
            world.setKeepSpawnInMemory(false);
            world.setAutoSave(false);
        }
        if (world == null) return;
        player.sendMessage(ChatColor.GRAY + "[Debug] World created in "
                + (System.currentTimeMillis() - debugStart) + "ms");
        Location origin = new Location(world, 0, 64, 0);
        Dungeon dungeon = new Dungeon(world, keyName);

        long taskStart = System.currentTimeMillis();
        java.util.List<BuildTask> tasks = new java.util.ArrayList<>();
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
                tasks.add(new BuildTask(templ, rotation, center, mob));
            }
        }
        player.sendMessage(ChatColor.GRAY + "[Debug] Prepared tasks in "
                + (System.currentTimeMillis() - taskStart) + "ms");

        long pasteStart = System.currentTimeMillis();

        Instance inst = new Instance(dungeon, keyName);
        java.util.List<Player> participants = new java.util.ArrayList<>();
        me.nakilex.levelplugin.party.PartyManager pm = plugin.getPartyManager();
        me.nakilex.levelplugin.party.Party party = pm.getParty(player.getUniqueId());
        if (party != null && party.isLeader(player.getUniqueId())) {
            for (java.util.UUID id : party.getMembers()) {
                Player mem = Bukkit.getPlayer(id);
                if (mem != null && mem.isOnline()) {
                    participants.add(mem);
                    inst.returnLocations.put(id, mem.getLocation());
                }
            }
        } else {
            participants.add(player);
            inst.returnLocations.put(player.getUniqueId(), player.getLocation());
        }
        instances.put(world, inst);
        world.setDifficulty(org.bukkit.Difficulty.NORMAL);
        world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, true);

        class State { boolean allowFlight; boolean flying; boolean invul; State(Player p){allowFlight=p.getAllowFlight();flying=p.isFlying();invul=p.isInvulnerable();}}
        java.util.Map<Player, State> prev = new java.util.HashMap<>();
        for (Player p : participants) {
            prev.put(p, new State(p));
            p.setAllowFlight(true);
            p.setFlying(true);
            p.setInvulnerable(true);
            p.teleport(origin);
        }

        new org.bukkit.scheduler.BukkitRunnable() {
            int idx = 0;

            @Override
            public void run() {
                if (idx >= tasks.size()) {
                    cancel();
                    player.sendMessage(ChatColor.GRAY + "[Debug] Pasted rooms in "
                            + (System.currentTimeMillis() - pasteStart) + "ms");
                    int tier = getThreatLevel(keyName);
                    spawnLootChests(dungeon, tier, inst);

                    // restore player states once world is ready
                    for (Player p : participants) {
                        State st = prev.get(p);
                        if (st != null && p.isOnline()) {
                            p.setInvulnerable(st.invul);
                            p.setAllowFlight(st.allowFlight);
                            p.setFlying(st.allowFlight && st.flying);
                        }
                    }
                    return;
                }
                BuildTask t = tasks.get(idx++);
                pasteRoom(dungeon, t.template(), t.rotation(), t.center(), t.mob(), false);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    public void cleanupInstances() {
        for (World w : new java.util.ArrayList<>(instances.keySet())) {
            removeWorld(w);
        }
    }

    public void cleanupOldInstanceWorlds() {
        java.io.File folder = plugin.getServer().getWorldContainer();
        java.io.File[] dirs = folder.listFiles((f) -> f.isDirectory() && f.getName().startsWith("dgn_"));
        if (dirs != null) {
            for (java.io.File d : dirs) {
                if (Bukkit.getWorld(d.getName()) != null) continue;
                FileUtil.deleteDirectory(d);
            }
        }
    }

    public boolean playDungeon(Player player, String name) {
        String key = normalizeKey(name);
        DungeonLayout layout = getLayout(name);
        if (layout == null) return false;
        long debugStart = System.currentTimeMillis();
        Location origin = player.getLocation();
        Dungeon dungeon = new Dungeon(player.getWorld(), key);
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
                pasteRoom(dungeon, templ, rotation, center, mob, false);
            }
        }
        spawnLootChests(dungeon, getThreatLevel(key), null);
        dungeons.put(key, dungeon);
        player.sendMessage(ChatColor.GRAY + "[Debug] Spawned in "
                + (System.currentTimeMillis() - debugStart) + "ms");
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

    public Set<String> getAvailableBosses() {
        var sec = plugin.getBossConfig().getConfigurationSection("mobs");
        if (sec == null) return Set.of();
        return sec.getKeys(false);
    }

    public Set<String> getLayoutNames() {
        return new java.util.HashSet<>(layoutDisplay.values());
    }

    private static RoomTemplate flipEntrances(RoomTemplate src) {
        List<RoomTemplate.Connector> list = new ArrayList<>();
        for (RoomTemplate.Connector c : src.getConnectors()) {
            list.add(new RoomTemplate.Connector(c.x, c.z, c.bottomY, c.facing, !c.entrance));
        }
        return new RoomTemplate(src.getBlocks(), list, src.getPortals(), src.getExitMarkers(), src.getBossSpawn(),
                src.getWidth(), src.getHeight(), src.getDepth(), src.getMinY());
    }

    private static class Instance {
        final Dungeon dungeon;
        final String layout;
        final Map<java.util.UUID, Location> returnLocations = new HashMap<>();
        final java.util.List<Integer> chestIds = new java.util.ArrayList<>();
        Instance(Dungeon d, String layout) { this.dungeon = d; this.layout = layout; }
    }

        private class InstanceListener implements org.bukkit.event.Listener {

            @org.bukkit.event.EventHandler(priority = EventPriority.HIGHEST)
            public void onPortal(PlayerPortalEvent e) {
                Instance inst = instances.get(e.getFrom().getWorld());
                if (inst == null) return;
                e.setCancelled(true);
                handleExit(e.getPlayer(), inst, e.getFrom().getWorld());
            }

            @org.bukkit.event.EventHandler
            public void onMove(org.bukkit.event.player.PlayerMoveEvent e) {
                if (e.getTo() == null) return;
                if (e.getTo().getBlock().getType() != Material.NETHER_PORTAL) return;
                if (e.getFrom().getBlock().getType() == Material.NETHER_PORTAL) return;
                Instance inst = instances.get(e.getTo().getWorld());
                if (inst == null) return;
                handleExit(e.getPlayer(), inst, e.getTo().getWorld());
            }

            private void handleExit(Player player, Instance inst, World world) {
                java.util.UUID id = player.getUniqueId();
                Location back = inst.returnLocations.remove(id);
                if (back != null) {
                    Dungeon.RoomInstance room = inst.dungeon.getRoomContaining(player.getLocation());
                    boolean completed = room != null && room.template == exit;
                    if (completed) {
                        sendCompleteMessage(player, getDisplayName(inst.layout));
                    } else {
                        sendExitMessage(player, getDisplayName(inst.layout));
                    }
                    player.teleport(back);
                }
                checkInstance(world);
            }

        @org.bukkit.event.EventHandler
        public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) {
            Instance inst = instances.get(e.getPlayer().getWorld());
            if (inst == null) return;
            inst.returnLocations.remove(e.getPlayer().getUniqueId());
            checkInstance(e.getPlayer().getWorld());
        }

        private void sendCompleteMessage(Player player, String layout) {
            me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "§a§l-", 45);
            me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§a§lDUNGEON COMPLETE!");
            me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§7You finished the §a" + layout + "§7 dungeon.");
            net.md_5.bungee.api.chat.TextComponent comp = new net.md_5.bungee.api.chat.TextComponent("§e§lCLICK-HERE §ato rate the dungeon!");
            comp.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.SUGGEST_COMMAND,
                    "/dungeon rate " + layout + " "));
            player.spigot().sendMessage(comp);
            me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "§a§l-", 45);
        }

        private void sendExitMessage(Player player, String layout) {
            me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "§c§l-", 45);
            me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§c§lDUNGEON EXITED");
            me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§7You left the §5" + layout + "§7 dungeon.");
            me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "§c§l-", 45);
        }
    }

    private void checkInstance(World world) {
        Instance inst = instances.get(world);
        if (inst != null && inst.returnLocations.isEmpty()) {
            removeWorld(world);
        }
    }

    private void removeWorld(World world) {
        Instance inst = instances.remove(world);
        if (inst != null) {
            for (int id : inst.chestIds) {
                lootChestManager.removeChest(id);
            }
        }
        Bukkit.unloadWorld(world, false);
        FileUtil.deleteDirectory(world.getWorldFolder());
    }

    private void spawnLootChests(Dungeon dungeon, int tier, Instance inst) {
        for (Dungeon.RoomInstance r : dungeon.getRooms()) {
            for (Location l : r.chests) {
                BlockData data = l.getBlock().getBlockData();
                BlockFace face = BlockFace.NORTH;
                if (data instanceof org.bukkit.block.data.Directional dir) {
                    face = dir.getFacing();
                }
                int id = lootChestManager.createAndSpawnChest(l, tier, face);
                if (inst != null) inst.chestIds.add(id);
            }
        }
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

    private record BuildTask(RoomTemplate template, int rotation, Location center, String mob) {}
}
