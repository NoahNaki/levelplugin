package me.nakilex.levelplugin.dungeon;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.TemplateType;
import me.nakilex.levelplugin.mob.utils.MobNameUtil;
import me.nakilex.levelplugin.lootchests.utils.LocationUtils;
import me.nakilex.levelplugin.dungeon.verified.VerifiedDungeonDefinition;
import me.nakilex.levelplugin.dungeon.verified.CrimsonReliquaryDungeon;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import me.nakilex.levelplugin.utils.FileUtil;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.WorldType;
import org.bukkit.WorldCreator;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.entity.TextDisplay;
import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerPortalEvent;

import java.util.*;

/**
 * Loads room templates from the flatland world and allows generating
 * simple dungeons using hallway pieces.
 */
public class DungeonManager {
    private final Main plugin;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private final Map<String, Dungeon> dungeons = new HashMap<>();
    private final Map<String, DungeonLayout> layouts = new HashMap<>();
    private final Map<String, String> layoutDisplay = new HashMap<>();
    private final Map<String, Integer> layoutThreat = new HashMap<>();
    private final Map<String, Boolean> layoutVerified = new HashMap<>();
    private final java.util.List<VerifiedDungeonDefinition> verifiedDungeons = new java.util.ArrayList<>();
    private java.io.File verifiedLayoutFile;
    private org.bukkit.configuration.file.FileConfiguration verifiedLayoutConfig;
    private java.io.File layoutFile;
    private org.bukkit.configuration.file.FileConfiguration layoutConfig;
    /** Guard asynchronous layout saves. */
    private final Object saveLock = new Object();
    private final DungeonBuilder builder;
    private final me.nakilex.levelplugin.lootchests.managers.LootChestManager lootChestManager;
    private final Map<java.util.UUID, RunStats> activeRuns = new HashMap<>();
    private final java.util.Set<java.util.UUID> pendingRespawns = new java.util.HashSet<>();
    private final java.util.Map<java.util.UUID, Location> pendingRespawnLocations = new java.util.HashMap<>();

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
        cleaned = cleaned.trim();
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
    /** Players who recently completed a dungeon and can rate it. */
    private final java.util.Map<java.util.UUID, String> pendingRatings = new java.util.HashMap<>();

    /** Return true if the given world is an active dungeon instance. */
    public boolean isInstanceWorld(World world) {
        return instances.containsKey(world);
    }

    /** Retrieve the layout key for an active dungeon instance world, if any. */
    public String getInstanceLayout(World world) {
        Instance inst = instances.get(world);
        return inst == null ? null : inst.getLayout();
    }

    /**
     * Handle a player exiting a dungeon instance, returning them to their stored
     * location and sending the appropriate messaging.
     */
    public void handleInstanceExit(World world, Player player) {
        handleInstanceExit(world, player, false);
    }

    public void handleInstanceExit(World world, Player player, boolean treatAsCompletion) {
        if (world == null || player == null) {
            return;
        }
        Instance inst = instances.get(world);
        if (inst == null) {
            return;
        }
        handleExit(player, inst, world, treatAsCompletion);
    }

    private void handleExit(Player player, Instance inst, World world, boolean treatAsCompletion) {
        java.util.UUID id = player.getUniqueId();
        Location back = inst.returnLocations.remove(id);
        if (back != null) {
            Dungeon.RoomInstance room = inst.dungeon.getRoomContaining(player.getLocation());
            boolean completed = inst.dungeon.isBossDefeated()
                    && (treatAsCompletion || (room != null && room.template == exit));
            if (completed) {
                sendCompleteMessage(player, getDisplayName(inst.layout));
                awardCompletionRewards(player, inst.layout, -1L, true);
                plugin.getPlayerConfig().addClearedDungeon(id, inst.layout);
            } else {
                sendExitMessage(player, getDisplayName(inst.layout));
            }
            player.teleport(back);
        }
        checkInstance(world);
        activeRuns.remove(id);
    }

    public DungeonManager(Main plugin, me.nakilex.levelplugin.lootchests.managers.LootChestManager lootChestManager) {
        this.plugin = plugin;
        this.lootChestManager = lootChestManager;
        loadTemplates();
        loadLayouts();
        registerVerifiedDungeons();
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
    public me.nakilex.levelplugin.lootchests.managers.LootChestManager getLootChestManager() { return lootChestManager; }

    public void disableInstanceFlight(Player player) {
        if (player == null) {
            return;
        }
        player.setFlying(false);
        player.setAllowFlight(false);
    }

    /** Create a void world used for temporary dungeon sessions. */
    public World createVoidWorld(String worldName) {
        return createVoidWorld(worldName, org.bukkit.Difficulty.PEACEFUL);
    }

    public World createVoidWorld(String worldName, org.bukkit.Difficulty difficulty) {
        WorldCreator wc = new WorldCreator(worldName);
        wc.generator(new VoidWorldGenerator());
        wc.type(WorldType.FLAT);
        wc.generateStructures(false);
        World world = Bukkit.createWorld(wc);
        if (world != null) {
            world.setKeepSpawnInMemory(false);
            world.setAutoSave(false);
            world.setDifficulty(difficulty);
            plugin.getWorldManager().applyBooleanGameRulesFromPrimary(world);
            world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
        }
        return world;
    }

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
        entrance = RoomTemplate.capture(world, -212, 74, -5334, -125, -36, -5227, true);
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
        return pasteRoom(dungeon, template, rotation, center, mob, preview, java.util.Set.of());
    }

    public PasteResult pasteRoom(Dungeon dungeon, RoomTemplate template, int rotation, Location center, String mob, boolean preview,
                                 java.util.Set<Material> ignoredMaterials) {
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
                if (shouldSkipTemplateMaterial(mat, ignoredMaterials)) continue;
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
                if (shouldSkipTemplateMaterial(mat, ignoredMaterials)) continue;
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

        for (RoomTemplate.ChestMarker c : template.getChests()) {
            int[] vec = RoomTemplate.rotate(c.x - (int) Math.round(template.getCenterX()),
                    c.z - (int) Math.round(template.getCenterZ()), rotation);
            int wx = center.getBlockX() + vec[0];
            int wy = baseY + (c.y - connectorY);
            int wz = center.getBlockZ() + vec[1];
            if (wx < minX) minX = wx;
            if (wy < minY) minY = wy;
            if (wz < minZ) minZ = wz;
            if (wx > maxX) maxX = wx;
            if (wy > maxY) maxY = wy;
            if (wz > maxZ) maxZ = wz;
        }

        Map<Location, BlockData> replaced = preview ? new HashMap<>() : Map.of();
        java.util.List<Dungeon.Chest> chestLocs = new java.util.ArrayList<>();
        Location bossLoc = null;

        for (RoomTemplate.BlockDef b : template.getBlocks()) {
            Material mat = b.data.getMaterial();
            if (shouldSkipTemplateMaterial(mat, ignoredMaterials)) continue;
            int[] vec = RoomTemplate.rotate(b.x - (int) Math.round(template.getCenterX()),
                    b.z - (int) Math.round(template.getCenterZ()), rotation);
            int wx = center.getBlockX() + vec[0];
            int wy = baseY + (b.y - connectorY);
            int wz = center.getBlockZ() + vec[1];
            Location l = new Location(world, wx, wy, wz);
            if (preview) replaced.put(l, world.getBlockAt(wx, wy, wz).getBlockData());
            BlockData data = RoomTemplate.rotateBlockData(b.data, rotation);
            world.getBlockAt(wx, wy, wz).setBlockData(data, false);
            if (b.profile != null) {
                var state = world.getBlockAt(wx, wy, wz).getState();
                if (state instanceof Skull skull) {
                    skull.setPlayerProfile(b.profile);
                    skull.update(false, false);
                }
            }
        }

        for (RoomTemplate.ChestMarker c : template.getChests()) {
            int[] vec = RoomTemplate.rotate(c.x - (int) Math.round(template.getCenterX()),
                    c.z - (int) Math.round(template.getCenterZ()), rotation);
            int wx = center.getBlockX() + vec[0];
            int wy = baseY + (c.y - connectorY);
            int wz = center.getBlockZ() + vec[1];
            Location l = new Location(world, wx, wy, wz);
            BlockData rotated = RoomTemplate.rotateBlockData(c.data, rotation);
            BlockFace face = rotated instanceof org.bukkit.block.data.Directional dir ? dir.getFacing() : BlockFace.NORTH;
            chestLocs.add(new Dungeon.Chest(l, face));
            world.getBlockAt(wx, wy, wz).setType(Material.AIR, false);
        }
        if (template.getBossSpawn() != null) {
            int[] vec = RoomTemplate.rotate(template.getBossSpawn().x - (int) Math.round(template.getCenterX()),
                    template.getBossSpawn().z - (int) Math.round(template.getCenterZ()), rotation);
            int wx = center.getBlockX() + vec[0];
            int wy = baseY + (template.getBossSpawn().y - connectorY);
            int wz = center.getBlockZ() + vec[1];
            Location l = new Location(world, wx, wy, wz);
            if (preview) replaced.put(l, world.getBlockAt(wx, wy, wz).getBlockData());
            world.getBlockAt(wx, wy, wz).setType(Material.BLACK_WOOL, false);
            // Spawn one block above the black wool marker so bosses stand on
            // the floor rather than inside the placeholder block.
            bossLoc = new Location(world, wx + 0.5, wy + 1, wz + 0.5);
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
        if (dungeon != null) {
            dungeon.addRoom(inst);
        }
        return new PasteResult(true, overlap, replaced, inst);
    }

    private boolean shouldSkipTemplateMaterial(Material material, java.util.Set<Material> ignoredMaterials) {
        if (material == Material.REDSTONE_BLOCK || material == Material.PINK_WOOL || material == Material.LIME_WOOL) {
            return true;
        }
        return ignoredMaterials != null && ignoredMaterials.contains(material);
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
            layoutVerified.remove(key);
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
        layoutVerified.putIfAbsent(lower, false);

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

    public enum LayoutFilter { ALL, VERIFIED, COMMUNITY }

    public Set<Map.Entry<String, String>> getLayoutEntries() {
        return getLayoutEntries(LayoutFilter.ALL);
    }

    public Set<Map.Entry<String, String>> getLayoutEntries(LayoutFilter filter) {
        if (filter == LayoutFilter.ALL) {
            return layoutDisplay.entrySet();
        }
        java.util.LinkedHashSet<Map.Entry<String, String>> filtered = new java.util.LinkedHashSet<>();
        for (var entry : layoutDisplay.entrySet()) {
            boolean verified = isVerified(entry.getKey());
            if (filter == LayoutFilter.VERIFIED && verified) {
                filtered.add(entry);
            } else if (filter == LayoutFilter.COMMUNITY && !verified) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    public String getDisplayName(String key) {
        return layoutDisplay.getOrDefault(normalizeKey(key), key);
    }

    public int getThreatLevel(String key) {
        return layoutThreat.getOrDefault(normalizeKey(key), 1);
    }

    public boolean isVerified(String key) {
        return layoutVerified.getOrDefault(normalizeKey(key), false);
    }

    public void setVerified(String key, boolean verified) {
        layoutVerified.put(normalizeKey(key), verified);
    }

    private org.bukkit.configuration.file.FileConfiguration getVerifiedLayoutConfig() {
        if (verifiedLayoutFile == null) {
            verifiedLayoutFile = new java.io.File(plugin.getDataFolder(), "verified_dungeons.yml");
        }
        if (!verifiedLayoutFile.exists()) {
            plugin.saveResource("verified_dungeons.yml", false);
        }
        if (verifiedLayoutConfig == null) {
            verifiedLayoutConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(verifiedLayoutFile);
        }
        return verifiedLayoutConfig;
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
                boolean verified = sec.getBoolean("verified", false);
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
                    int x;
                    int y;
                    try {
                        x = Integer.parseInt(parts[0].trim());
                        y = Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException ex) {
                        plugin.getLogger().warning("[Dungeon] Invalid cell coordinates '" + coord
                                + "' in layout '" + rawKey + "'. Skipping.");
                        continue;
                    }
                    String typeName = cell.getString("type", "NONE");
                    RoomType type;
                    try {
                        type = RoomType.valueOf(typeName);
                    } catch (IllegalArgumentException ex) {
                        plugin.getLogger().warning("[Dungeon] Unknown room type '" + typeName
                                + "' at cell '" + coord + "' in layout '" + rawKey + "'. Using NONE.");
                        type = RoomType.NONE;
                    }
                    String templateName = cell.getString("template", "NONE");
                    TemplateType t;
                    try {
                        t = TemplateType.valueOf(templateName);
                    } catch (IllegalArgumentException ex) {
                        plugin.getLogger().warning("[Dungeon] Unknown template type '" + templateName
                                + "' at cell '" + coord + "' in layout '" + rawKey + "'. Using NONE.");
                        t = TemplateType.NONE;
                    }
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
            layoutVerified.put(key, verified);
        }
    }

    private void registerVerifiedDungeons() {
        verifiedDungeons.clear();

        java.util.Map<String, VerifiedDungeonDefinition> available = new java.util.LinkedHashMap<>();
        VerifiedDungeonDefinition reliquary = new CrimsonReliquaryDungeon(plugin);
        available.put(reliquary.getKey(), reliquary);

        org.bukkit.configuration.file.FileConfiguration cfg = getVerifiedLayoutConfig();
        plugin.getLogger().info("[Dungeon] Loading verified dungeon config from " + verifiedLayoutFile.getAbsolutePath());
        boolean changed = false;
        for (var entry : available.entrySet()) {
            VerifiedDungeonDefinition def = entry.getValue();
            String base = "verified." + normalizeKey(def.getKey());
            if (!cfg.contains(base + ".enabled")) {
                cfg.set(base + ".enabled", true);
                changed = true;
            }
            if (!cfg.contains(base + ".display")) {
                cfg.set(base + ".display", def.getDisplayName());
                changed = true;
            }

            if (!cfg.getBoolean(base + ".enabled", true)) {
                plugin.getLogger().info("[Dungeon] Verified dungeon '" + def.getKey() + "' disabled in config");
                continue;
            }

            String display = cfg.getString(base + ".display", def.getDisplayName());
            def.register(this);
            layoutDisplay.put(normalizeKey(def.getKey()), display);
            verifiedDungeons.add(def);
            plugin.getLogger().info("[Dungeon] Registered verified dungeon '" + def.getKey() + "' with display '" + display + "'");
        }

        if (changed && verifiedLayoutFile != null) {
            try {
                cfg.save(verifiedLayoutFile);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save verified dungeon config: " + e.getMessage());
            }
        }
    }

    public void registerVerifiedLayout(String key, String displayName, int defaultThreat, DungeonLayout fallbackLayout) {
        String normalized = normalizeKey(key);
        if (!layouts.containsKey(normalized) && fallbackLayout != null) {
            fallbackLayout.setStep(0);
            layouts.put(normalized, fallbackLayout);
        }
        layoutDisplay.putIfAbsent(normalized, displayName);
        layoutVerified.put(normalized, true);
        layoutThreat.putIfAbsent(normalized, defaultThreat);
    }

    private VerifiedDungeonDefinition getVerifiedDungeon(String key) {
        String normalized = normalizeKey(key);
        for (VerifiedDungeonDefinition def : verifiedDungeons) {
            if (def.matches(normalized)) return def;
        }
        return null;
    }

    public VerifiedDungeonDefinition getVerifiedDefinition(String key) {
        return getVerifiedDungeon(key);
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
            sec.set("verified", layoutVerified.getOrDefault(key, false));
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
        me.nakilex.levelplugin.party.PartyManager partyManager = plugin.getPartyManager();
        me.nakilex.levelplugin.party.Party party = partyManager == null ? null : partyManager.getParty(player.getUniqueId());
        if (party != null && partyHasActiveInstance(party)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Your party is already inside a dungeon. Finish that run before starting another.");
            return;
        }
        String keyName = normalizeKey(name);
        VerifiedDungeonDefinition verified = getVerifiedDungeon(keyName);
        if (verified != null) {
            verified.startInstance(this, player);
            return;
        }
        DungeonLayout layout = getLayout(name);
        if (layout == null) {
            player.sendMessage(ChatColor.RED + "Dungeon not found.");
            debugMissingDungeon(keyName);
            return;
        }
        long debugStart = System.currentTimeMillis();
        String worldName = "dgn_" + keyName + "_" + System.currentTimeMillis();
        World world = createVoidWorld(worldName, org.bukkit.Difficulty.HARD);
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
                TemplateType templateType = identifyTemplate(templ);
                if (mob != null && !mob.isBlank()) {
                    plugin.getLogger().info(String.format(
                            "[DungeonSpawn] Queued mob '%s' (canonical='%s') for %s at %s (rotation=%d, cell=%d,%d)",
                            mob,
                            MobNameUtil.canonicalMobKey(mob),
                            templateType != null ? templateType.name() : "UNKNOWN",
                            LocationUtils.blockLocationString(center),
                            rotation,
                            x,
                            y));
                }
                tasks.add(new BuildTask(templ, rotation, center, mob));
            }
        }
        player.sendMessage(ChatColor.GRAY + "[Debug] Prepared tasks in "
                + (System.currentTimeMillis() - taskStart) + "ms");

        long pasteStart = System.currentTimeMillis();

        Instance inst = new Instance(dungeon, keyName);
        world.setSpawnLocation(origin);
        inst.setSpawnLocation(origin);
        java.util.List<Player> participants = new java.util.ArrayList<>();
        me.nakilex.levelplugin.party.PartyManager pm = plugin.getPartyManager();
        me.nakilex.levelplugin.party.Party activeParty = pm.getParty(player.getUniqueId());
        if (activeParty != null) {
            for (java.util.UUID id : activeParty.getMembers()) {
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
        world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);

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

                    spawnLootChests(dungeon);

                    // restore player states once world is ready
                    for (Player p : participants) {
                        State st = prev.get(p);
                        if (st != null && p.isOnline()) {
                            p.setInvulnerable(st.invul);
                            p.setAllowFlight(st.allowFlight);
                            p.setFlying(st.allowFlight && st.flying);
                            disableInstanceFlight(p);
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
        VerifiedDungeonDefinition verified = getVerifiedDungeon(key);
        if (verified != null) {
            verified.startInstance(this, player);
            return true;
        }
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
        dungeons.put(key, dungeon);
        spawnLootChests(dungeon);
        player.sendMessage(ChatColor.GRAY + "[Debug] Spawned in "
                + (System.currentTimeMillis() - debugStart) + "ms");
        startRun(java.util.Collections.singleton(player.getUniqueId()), key, System.currentTimeMillis());
        return true;
    }

    private boolean partyHasActiveInstance(me.nakilex.levelplugin.party.Party party) {
        if (party == null) {
            return false;
        }
        for (java.util.UUID id : party.getMembers()) {
            Player member = Bukkit.getPlayer(id);
            if (member != null && member.isOnline() && isInstanceWorld(member.getWorld())) {
                return true;
            }
        }
        return false;
    }

    public void startRun(java.util.Collection<java.util.UUID> participants, String layoutKey, long startMillis) {
        if (participants == null || layoutKey == null) return;
        long start = startMillis > 0 ? startMillis : System.currentTimeMillis();
        for (java.util.UUID id : participants) {
            if (id == null) continue;
            activeRuns.put(id, new RunStats(layoutKey, start));
        }
    }

    public void addCombatPowerContribution(java.util.UUID playerId, int combatPower) {
        if (playerId == null || combatPower <= 0) return;
        RunStats stats = activeRuns.get(playerId);
        if (stats != null) {
            stats.combatPower += combatPower;
        }
    }

    public void markPuzzleComplete(java.util.Collection<java.util.UUID> participants) {
        if (participants == null) return;
        for (java.util.UUID id : participants) {
            RunStats stats = activeRuns.get(id);
            if (stats != null) {
                stats.puzzleComplete = true;
            }
        }
    }

    public CompletionXp awardCompletionRewards(Player player, String layoutKey) {
        return awardCompletionRewards(player, layoutKey, -1L, true);
    }

    public CompletionXp awardCompletionRewards(Player player, String layoutKey, long durationSeconds,
                                               boolean sendDefaultMessage) {
        me.nakilex.levelplugin.player.level.managers.LevelManager lm = me.nakilex.levelplugin.player.level.managers.LevelManager.getInstance();
        RunStats stats = activeRuns.remove(player.getUniqueId());
        if (lm == null || stats == null || !layoutKey.equalsIgnoreCase(stats.layoutKey)) {
            return null;
        }
        long durationSecondsResolved = durationSeconds > 0
                ? durationSeconds
                : Math.max(1, (System.currentTimeMillis() - stats.startMillis) / 1000);
        double timeMultiplier = 1.0;
        if (durationSecondsResolved <= 600) {
            timeMultiplier += Math.min(0.5, (600 - durationSecondsResolved) / 1200.0);
        } else {
            timeMultiplier -= Math.min(0.5, (durationSecondsResolved - 600) / 1200.0);
        }
        double puzzleMultiplier = stats.puzzleComplete ? 1.25 : 1.0;
        int baseXp = Math.round((float) stats.combatPower / 10);
        int timeAdjustedXp = (int) Math.round(baseXp * timeMultiplier);
        int timeBonus = timeAdjustedXp - baseXp;
        int puzzleBonus = (int) Math.round(timeAdjustedXp * (puzzleMultiplier - 1));
        int flawlessBonus = stats.deathCount <= 0 ? (int) Math.round((timeAdjustedXp + puzzleBonus) * 0.15) : 0;
        int totalXp = Math.max(0, timeAdjustedXp + puzzleBonus + flawlessBonus);
        int coinsAward = totalXp > 0 ? Math.max(25, Math.round(totalXp * 0.35f)) : 0;
        CompletionXp breakdown = new CompletionXp(baseXp, timeBonus, puzzleBonus, totalXp, coinsAward,
                timeMultiplier, puzzleMultiplier, flawlessBonus, stats.deathCount);
        if (totalXp > 0) {
            lm.addXP(player, totalXp);
        }
        if (coinsAward > 0 && plugin.getEconomyManager() != null) {
            plugin.getEconomyManager().addCoins(player, coinsAward);
        }
        if (sendDefaultMessage && (totalXp > 0 || coinsAward > 0)) {
            String expLabel = me.nakilex.levelplugin.utils.ChatFormatter.experienceLabel();
            String expColor = me.nakilex.levelplugin.utils.ChatFormatter.experienceColor();
            StringBuilder msg = new StringBuilder(ChatColor.GREEN + "You gained ");
            if (totalXp > 0) {
                msg.append(expColor).append(totalXp).append(ChatColor.GRAY)
                        .append(" <glyph:experience_orb_icon> ").append(expLabel);
            }
            if (coinsAward > 0) {
                if (totalXp > 0) msg.append(ChatColor.GREEN).append(" and ");
                msg.append(ChatColor.GOLD).append(coinsAward).append(ChatColor.GRAY).append(" <glyph:coins_icon> coins");
            }
            if (flawlessBonus > 0) {
                msg.append(ChatColor.GREEN).append(" ").append(ChatColor.AQUA)
                        .append("(+").append(flawlessBonus).append(" flawless bonus)");
            }
            msg.append(ChatColor.GREEN).append(" for clearing the dungeon.");
            player.sendMessage(msg.toString());
        }
        return breakdown;
    }

    private static final class RunStats {
        private final String layoutKey;
        private final long startMillis;
        private int combatPower;
        private boolean puzzleComplete;
        private int deathCount;

        private RunStats(String layoutKey, long startMillis) {
            this.layoutKey = layoutKey;
            this.startMillis = startMillis;
            this.combatPower = 0;
            this.puzzleComplete = false;
            this.deathCount = 0;
        }
    }

    public record CompletionXp(int mobXp, int timeBonus, int puzzleBonus, int totalXp, int coins,
                               double timeMultiplier, double puzzleMultiplier, int flawlessBonus, int deaths) {
        public int timeAdjustedXp() {
            return mobXp + timeBonus;
        }
    }

    public Instance createTrackedInstance(Dungeon dungeon, String layoutKey, World world) {
        Instance inst = new Instance(dungeon, layoutKey);
        if (world != null) {
            inst.setSpawnLocation(world.getSpawnLocation());
        }
        instances.put(world, inst);
        return inst;
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
        java.util.LinkedHashMap<String, String> uniqueByIdentity = new java.util.LinkedHashMap<>();
        for (String key : sec.getKeys(false)) {
            if (key == null || key.isBlank()) continue;
            String identity = normalizeMobIdentity(key);
            if (!identity.isEmpty()) {
                uniqueByIdentity.putIfAbsent(identity, key);
            } else {
                uniqueByIdentity.put(UUID.randomUUID().toString(), key);
            }
        }
        var bossSec = plugin.getBossConfig().getConfigurationSection("mobs");
        if (bossSec != null) {
            Set<String> normalizedBosses = new java.util.LinkedHashSet<>();
            for (String bossKey : bossSec.getKeys(false)) {
                if (bossKey == null || bossKey.isBlank()) continue;
                String identity = normalizeMobIdentity(bossKey);
                if (!identity.isEmpty()) {
                    normalizedBosses.add(identity);
                }
                org.bukkit.configuration.ConfigurationSection bossEntry = bossSec.getConfigurationSection(bossKey);
                if (bossEntry != null) {
                    String mobKey = bossEntry.getString("mob");
                    String mobIdentity = normalizeMobIdentity(mobKey);
                    if (!mobIdentity.isEmpty()) {
                        normalizedBosses.add(mobIdentity);
                    }
                }
            }
            if (!normalizedBosses.isEmpty()) {
                uniqueByIdentity.entrySet().removeIf(entry -> normalizedBosses.contains(entry.getKey()));
            }
        }
        return java.util.Collections.unmodifiableSet(new java.util.LinkedHashSet<>(uniqueByIdentity.values()));
    }

    public Set<String> getAvailableMobs(java.util.UUID playerId) {
        return filterUnlocked(getAvailableMobs(), playerId);
    }

    public Set<String> getAvailableBosses() {
        var sec = plugin.getBossConfig().getConfigurationSection("mobs");
        if (sec == null) return Set.of();
        return sec.getKeys(false);
    }

    public Set<String> getAvailableBosses(java.util.UUID playerId) {
        return filterUnlocked(getAvailableBosses(), playerId);
    }

    public Set<String> getLayoutNames() {
        return new java.util.HashSet<>(layoutDisplay.values());
    }

    private void debugMissingDungeon(String requestedKey) {
        String normalized = normalizeKey(requestedKey);
        plugin.getLogger().warning("[Dungeon] Dungeon lookup failed for '" + requestedKey + "' (normalized='" + normalized + "')");

        java.util.List<String> verifiedKeys = new java.util.ArrayList<>();
        for (VerifiedDungeonDefinition def : verifiedDungeons) {
            verifiedKeys.add(def.getKey());
        }
        plugin.getLogger().warning("[Dungeon] Verified definitions loaded: " + joinOrNone(verifiedKeys));

        java.util.List<String> layoutKeys = new java.util.ArrayList<>();
        for (String key : layouts.keySet()) {
            layoutKeys.add(key);
        }
        plugin.getLogger().warning("[Dungeon] Layout keys loaded: " + joinOrNone(layoutKeys));

        java.util.List<String> layoutDisplays = new java.util.ArrayList<>();
        for (var entry : layoutDisplay.entrySet()) {
            layoutDisplays.add(entry.getKey() + "=" + entry.getValue());
        }
        plugin.getLogger().warning("[Dungeon] Layout display map: " + joinOrNone(layoutDisplays));

        if (verifiedLayoutFile != null) {
            plugin.getLogger().warning("[Dungeon] Verified config path: " + verifiedLayoutFile.getAbsolutePath()
                    + " (exists=" + verifiedLayoutFile.exists() + ")");
        }
    }

    private String joinOrNone(java.util.Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return "<none>";
        }
        return String.join(", ", values);
    }

    private Set<String> filterUnlocked(Set<String> keys, java.util.UUID playerId) {
        if (keys == null || keys.isEmpty()) return java.util.Collections.emptySet();
        me.nakilex.levelplugin.codex.CodexManager codex = plugin.getCodexManager();
        if (codex == null || playerId == null) {
            return java.util.Collections.unmodifiableSet(new java.util.LinkedHashSet<>(keys));
        }
        java.util.LinkedHashSet<String> unlocked = new java.util.LinkedHashSet<>();
        for (String key : keys) {
            if (key == null || key.isBlank()) continue;
            if (codex.hasDiscoveredIdentity(playerId, key)) {
                unlocked.add(key);
            }
        }
        return java.util.Collections.unmodifiableSet(unlocked);
    }

    private String normalizeMobIdentity(String key) {
        return MobNameUtil.canonicalMobKey(key);
    }

    /** Store that the given player may rate the specified dungeon. */
    public void markPendingRating(java.util.UUID id, String layoutKey) {
        pendingRatings.put(id, layoutKey);
    }

    /** Check which dungeon the player can rate without consuming the entry. */
    public String getPendingRating(java.util.UUID id) {
        return pendingRatings.get(id);
    }

    /** Remove and return the dungeon key the player is allowed to rate. */
    public String consumePendingRating(java.util.UUID id) {
        return pendingRatings.remove(id);
    }

    /** Clear any pending rating entry without returning it. */
    public void clearPendingRating(java.util.UUID id) {
        pendingRatings.remove(id);
    }

    private static RoomTemplate flipEntrances(RoomTemplate src) {
        List<RoomTemplate.Connector> list = new ArrayList<>();
        for (RoomTemplate.Connector c : src.getConnectors()) {
            list.add(new RoomTemplate.Connector(c.x, c.z, c.bottomY, c.facing, !c.entrance));
        }
        return new RoomTemplate(src.getBlocks(), list, src.getPortals(), src.getExitMarkers(), src.getChests(),
                src.getBossSpawn(), src.getWidth(), src.getHeight(), src.getDepth(), src.getMinY());
    }

    public static class Instance {
        private final Dungeon dungeon;
        private final String layout;
        private final Map<java.util.UUID, Location> returnLocations = new HashMap<>();
        private final java.util.List<Integer> chestIds = new java.util.ArrayList<>();
        private Location spawnLocation;
        org.bukkit.scheduler.BukkitTask removalTask;
        Instance(Dungeon d, String layout) { this.dungeon = d; this.layout = layout; }

        public Dungeon getDungeon() { return dungeon; }
        public String getLayout() { return layout; }
        public Map<java.util.UUID, Location> getReturnLocations() { return returnLocations; }
        public java.util.List<Integer> getChestIds() { return chestIds; }
        public Location getSpawnLocation() { return spawnLocation; }
        public void setSpawnLocation(Location spawnLocation) { this.spawnLocation = spawnLocation; }
        public void addReturnLocation(java.util.UUID id, Location loc) { returnLocations.put(id, loc); }
        public void addChestId(int id) { chestIds.add(id); }
    }

        private class InstanceListener implements org.bukkit.event.Listener {

            @org.bukkit.event.EventHandler(priority = EventPriority.HIGHEST)
            public void onPortal(PlayerPortalEvent e) {
                Instance inst = instances.get(e.getFrom().getWorld());
                if (inst == null) return;
                e.setCancelled(true);
                handleExit(e.getPlayer(), inst, e.getFrom().getWorld(), false);
            }

            @org.bukkit.event.EventHandler
            public void onMove(org.bukkit.event.player.PlayerMoveEvent e) {
                if (e.getTo() == null) return;
                if (e.getTo().getBlock().getType() != Material.NETHER_PORTAL) return;
                if (e.getFrom().getBlock().getType() == Material.NETHER_PORTAL) return;
                Instance inst = instances.get(e.getTo().getWorld());
                if (inst == null) return;
                handleExit(e.getPlayer(), inst, e.getTo().getWorld(), false);
            }

        @org.bukkit.event.EventHandler
        public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) {
            World w = e.getPlayer().getWorld();
            Instance inst = instances.get(w);
            if (inst == null) return;
            if (w.getPlayers().size() <= 1) {
                scheduleRemoval(w, inst);
            }
        }

        @org.bukkit.event.EventHandler
        public void onDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
            Player player = event.getEntity();
            Instance inst = instances.get(player.getWorld());
            if (inst == null) return;
            RunStats runStats = activeRuns.get(player.getUniqueId());
            if (runStats != null) {
                runStats.deathCount++;
            }
            pendingRespawns.add(player.getUniqueId());
            Location spawn = resolveInstanceSpawn(inst, player.getWorld());
            if (spawn != null) {
                pendingRespawnLocations.put(player.getUniqueId(), spawn.clone());
            }
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.spigot().respawn();
                }
            }, 1L);
        }

        @org.bukkit.event.EventHandler
        public void onRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
            Player player = event.getPlayer();
            if (!pendingRespawns.remove(player.getUniqueId())) {
                return;
            }
            Location spawn = pendingRespawnLocations.remove(player.getUniqueId());
            if (spawn != null) {
                event.setRespawnLocation(spawn);
                return;
            }
            Instance inst = instances.get(player.getWorld());
            if (inst == null) return;
            Location fallback = resolveInstanceSpawn(inst, player.getWorld());
            if (fallback != null) {
                event.setRespawnLocation(fallback);
            }
        }

        @org.bukkit.event.EventHandler
        public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                World w = e.getPlayer().getWorld();
                Instance inst = instances.get(w);
                if (inst != null && inst.removalTask != null) {
                    inst.removalTask.cancel();
                    inst.removalTask = null;
                }
            }, 1L);
        }

    }

    private Location resolveInstanceSpawn(Instance inst, World world) {
        if (inst == null) return null;
        if (inst.getSpawnLocation() != null) {
            return inst.getSpawnLocation().clone();
        }
        if (world != null) {
            return world.getSpawnLocation();
        }
        return null;
    }

    private void sendCompleteMessage(Player player, String layout) {
        me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "§a§l-", 45);
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§a§lDUNGEON COMPLETE!");
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§7You finished the §a" + layout + "§7 dungeon.");
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "");
        String msg = me.nakilex.levelplugin.utils.ChatFormatter.getCenteredText("§e§lCLICK-HERE §7to rate the dungeon!");
        Component comp = LEGACY.deserialize(msg)
                .clickEvent(ClickEvent.runCommand("/dungeon rate " + layout));
        player.sendMessage(comp);
        me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "§a§l-", 45);
        markPendingRating(player.getUniqueId(), normalizeKey(layout));
    }

    private void sendExitMessage(Player player, String layout) {
        me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "§c§l-", 45);
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§c§lDUNGEON EXITED");
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§7You left the §5" + layout + "§7 dungeon.");
        me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "§c§l-", 45);
    }

    private void checkInstance(World world) {
        Instance inst = instances.get(world);
        if (inst != null && inst.returnLocations.isEmpty()) {
            removeWorld(world);
        }
    }

    public void runInstanceHeartbeat() {
        if (instances.isEmpty()) {
            return;
        }
        for (Map.Entry<World, Instance> entry : new java.util.ArrayList<>(instances.entrySet())) {
            World world = entry.getKey();
            Instance inst = entry.getValue();
            if (world == null || inst == null) {
                continue;
            }
            pruneExitedPlayers(world, inst);
            refreshInstancePortals(inst);
            if (world.getPlayers().isEmpty()) {
                removeWorld(world);
            }
        }
    }

    private void pruneExitedPlayers(World world, Instance inst) {
        if (world == null || inst == null) {
            return;
        }
        java.util.Iterator<Map.Entry<java.util.UUID, Location>> iterator = inst.returnLocations.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<java.util.UUID, Location> entry = iterator.next();
            java.util.UUID id = entry.getKey();
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline()) {
                continue;
            }
            if (!world.equals(player.getWorld())) {
                iterator.remove();
                activeRuns.remove(id);
            }
        }
    }

    private void refreshInstancePortals(Instance inst) {
        Dungeon dungeon = inst == null ? null : inst.getDungeon();
        if (dungeon == null) {
            return;
        }
        for (Dungeon.RoomInstance room : dungeon.getRooms()) {
            if (room == null || room.template == null) {
                continue;
            }
            for (RoomTemplate.Marker marker : room.template.getPortals()) {
                int[] vec = RoomTemplate.rotate(marker.x - (int) Math.round(room.template.getCenterX()),
                        marker.z - (int) Math.round(room.template.getCenterZ()), room.rotation);
                int wx = room.center.getBlockX() + vec[0];
                int wy = room.center.getBlockY() + (marker.y - room.template.getConnectorMinY());
                int wz = room.center.getBlockZ() + vec[1];
                World world = room.center.getWorld();
                if (world == null) {
                    continue;
                }
                if (!world.isChunkLoaded(wx >> 4, wz >> 4)) {
                    continue;
                }
                if (world.getBlockAt(wx, wy, wz).getType() != Material.NETHER_PORTAL) {
                    world.getBlockAt(wx, wy, wz).setType(Material.NETHER_PORTAL, false);
                }
            }
        }
    }

    private void removeWorld(World world) {
        Instance inst = instances.remove(world);
        if (inst != null) {
            if (inst.removalTask != null) inst.removalTask.cancel();
            me.nakilex.levelplugin.player.profile.ProfileManager pm =
                    me.nakilex.levelplugin.player.profile.ProfileManager.getInstance();
            me.nakilex.levelplugin.player.config.PlayerConfig cfg =
                    Main.getInstance().getPlayerConfig();
            for (var e : inst.returnLocations.entrySet()) {
                java.util.UUID id = e.getKey();
                Location back = e.getValue();
                org.bukkit.entity.Player online = Bukkit.getPlayer(id);
                if (online != null && online.isOnline()) {
                    online.teleport(back);
                }
                Integer slot = pm.getActiveSlot(id);
                if (slot != null) {
                    cfg.setProfileLocation(id, slot, back);
                    cfg.savePlayer(id);
                }
            }
            for (int id : inst.chestIds) {
                lootChestManager.removeChest(id);
            }
        }
        Bukkit.unloadWorld(world, false);
        FileUtil.deleteDirectory(world.getWorldFolder());
    }

    private void scheduleRemoval(World world, Instance inst) {
        if (inst.removalTask != null) inst.removalTask.cancel();
        inst.removalTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (world.getPlayers().isEmpty()) {
                removeWorld(world);
            }
        }, 5 * 60 * 20L);
    }

    /**
     * Spawn loot chests for a dungeon when it is created.
     * Chest IDs are tracked for instance worlds so they can be cleaned up
     * when the dungeon is removed.
     */
    public void spawnLootChests(Dungeon dungeon) {
        World world = dungeon.getRooms().isEmpty() ? null : dungeon.getRooms().get(0).center.getWorld();
        Instance inst = world == null ? null : instances.get(world);
        spawnLootChests(dungeon, inst);
    }

    private void spawnLootChests(Dungeon dungeon, Instance inst) {
        for (Dungeon.RoomInstance r : dungeon.getRooms()) {
            for (Dungeon.Chest c : r.chests) {
                Location l = c.loc();
                if (!l.getChunk().isLoaded()) {
                    l.getChunk().load();
                }
                l.getBlock().setType(Material.AIR, false);
                int id = lootChestManager.createAndSpawnChest(l.clone(), c.facing());
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
