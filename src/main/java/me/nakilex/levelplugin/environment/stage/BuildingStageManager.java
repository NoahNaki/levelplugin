package me.nakilex.levelplugin.environment.stage;

import me.nakilex.levelplugin.Main;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.math.BlockVector3;

import me.nakilex.levelplugin.utils.SchematicUtil;

import java.io.File;
import java.util.*;

/**
 * Stores building stage data and handles spawning the stage structures/NPCs.
 */
public class BuildingStageManager {
    private final Main plugin;
    /** Map of building -> stage -> data */
    private final Map<String, Map<Integer, BuildingStage>> stages = new HashMap<>();
    /** Map of town -> building -> placement offset */
    private final Map<String, Map<String, Placement>> placements = new HashMap<>();
    private final Map<java.util.UUID, Map<String, List<StageNpc.Instance>>> spawnedNPCs = new HashMap<>();
    /** Folder containing FAWE schematics for each stage. */
    private final File schemFolder;
    private File file;
    private FileConfiguration config;

    public BuildingStageManager(Main plugin) {
        this.plugin = plugin;
        this.schemFolder = new File(plugin.getDataFolder(), "building_schematics");
        if (!schemFolder.exists()) schemFolder.mkdirs();
        loadConfig();
    }

    /** All defined building names. */
    public Set<String> getStageNames() {
        return new HashSet<>(stages.keySet());
    }

    public BuildingStage getStage(String building, int stage) {
        var buildMap = stages.get(building.toLowerCase());
        if (buildMap == null) return null;
        return buildMap.get(stage);
    }

    /** Return all building names defined for a town. */
    public Set<String> getBuildings(String town) {
        var map = placements.get(town.toLowerCase());
        if (map == null) return Collections.emptySet();
        return new HashSet<>(map.keySet());
    }

    /** Create a new stage from the selected area. */
    public void createStage(String building, int stage,
                            Location pos1, Location pos2, Location standLoc,
                            Location origin, int priority) {
        List<StageNpc.Definition> npcs = captureNPCs(pos1, pos2);
        List<BlockDef> blocks = captureBlocks(pos1, pos2);

        // Save a schematic of the selected area using FAWE
        String fileName = building.toLowerCase() + "_" + stage + ".schem";
        File schematic = new File(schemFolder, fileName);
        SchematicUtil.saveSchematic(pos1, pos2, schematic, plugin.getLogger());

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());

        int hx = standLoc.getBlockX() - minX;
        int hy = standLoc.getBlockY() - minY;
        int hz = standLoc.getBlockZ() - minZ;

        int ox = origin.getBlockX() - minX;
        int oy = origin.getBlockY() - minY;
        int oz = origin.getBlockZ() - minZ;

        stages
            .computeIfAbsent(building.toLowerCase(), k -> new HashMap<>())
            .put(stage, new BuildingStage(building.toLowerCase(), stage, pos1, pos2,
                    npcs, blocks, schematic, fileName, priority, hx, hy, hz, ox, oy, oz,
                    null, 0));
        saveConfig();
    }

    public boolean removeStage(String building, int stage) {
        var buildMap = stages.get(building.toLowerCase());
        if (buildMap == null) return false;
        if (buildMap.remove(stage) != null) {
            if (buildMap.isEmpty()) stages.remove(building.toLowerCase());
            saveConfig();
            return true;
        }
        return false;
    }

    /** Link a building to a town at the given offset. */
    public void linkBuilding(String town, String building, int x, int y, int z) {
        placements
            .computeIfAbsent(town.toLowerCase(), k -> new HashMap<>())
            .put(building.toLowerCase(), new Placement(x, y, z));
        saveConfig();
    }

    public Placement getPlacement(String town, String building) {
        var map = placements.get(town.toLowerCase());
        if (map == null) return null;
        return map.get(building.toLowerCase());
    }

    /** Absolute origin location recorded when the building stage was created. */
    public Location getStageOrigin(String building) {
        BuildingStage st = getStage(building, 1);
        if (st == null) return null;
        int minX = Math.min(st.pos1.getBlockX(), st.pos2.getBlockX());
        int minY = Math.min(st.pos1.getBlockY(), st.pos2.getBlockY());
        int minZ = Math.min(st.pos1.getBlockZ(), st.pos2.getBlockZ());
        return new Location(st.pos1.getWorld(), minX + st.ox, minY + st.oy, minZ + st.oz);
    }

    // Offset for spawning NPCs. Use zero so they stand directly on the ground.
    private static final double NPC_SPAWN_Y_OFFSET = 0.0;

    public void spawnForStage(Player viewer, String building,
                              int stage, Location origin) {
        BuildingStage st = getStage(building, stage);
        if (st == null || origin == null || viewer == null) return;
        UUID id = viewer.getUniqueId();
        var map = spawnedNPCs.computeIfAbsent(id, k -> new HashMap<>());
        String key = building.toLowerCase() + ":" + stage;
        List<StageNpc.Instance> list = map.computeIfAbsent(key, k -> new ArrayList<>());
        for (StageNpc.Instance instance : list) {
            instance.despawn();
        }
        list.clear();
        for (StageNpc.Definition def : st.npcs) {
            Location loc = origin.clone().add(
                    def.x() - st.ox + 0.5,
                    def.y() - st.oy + NPC_SPAWN_Y_OFFSET,
                    def.z() - st.oz + 0.5
            );
            loc.setYaw(def.yaw());
            loc.setPitch(def.pitch());
            StageNpc.Instance instance = def.spawn(loc, plugin.getLogger(),
                    "building=" + building + ",stage=" + stage);
            if (instance != null) {
                list.add(instance);
            }
        }
    }

    public void despawnForStage(UUID viewerId, String building, int stage) {
        var map = spawnedNPCs.get(viewerId);
        if (map == null) return;
        String key = building.toLowerCase() + ":" + stage;
        List<StageNpc.Instance> list = map.remove(key);
        if (list != null) list.forEach(StageNpc.Instance::despawn);
        if (map.isEmpty()) spawnedNPCs.remove(viewerId);
    }

    public void despawnAll() {
        for (var map : spawnedNPCs.values()) {
            for (var list : map.values()) {
                list.forEach(StageNpc.Instance::despawn);
            }
        }
        spawnedNPCs.clear();
    }

    public void hideNPCsFrom(Player viewer) {
        if (viewer == null) return;
        for (var map : spawnedNPCs.values()) {
            for (var list : map.values()) {
                for (StageNpc.Instance ignored : list) {
                    // NPCs are now public; do nothing
                }
            }
        }
    }

    private List<StageNpc.Definition> captureNPCs(Location p1, Location p2) {
        List<StageNpc.Definition> list = new ArrayList<>();
        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            Location l = npc.isSpawned() ? npc.getEntity().getLocation() : npc.getStoredLocation();
            if (l == null || !l.getWorld().equals(p1.getWorld())) continue;
            int x = l.getBlockX();
            int y = l.getBlockY();
            int z = l.getBlockZ();
            if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ) {
                list.add(new StageNpc.Definition(
                        StageNpc.Type.CITIZENS,
                        npc.getId(),
                        null,
                        npc.getId(),
                        x - minX,
                        y - minY,
                        z - minZ,
                        l.getYaw(),
                        l.getPitch()
                ));
            }
        }
        return list;
    }

    private List<BlockDef> captureBlocks(Location p1, Location p2) {
        List<BlockDef> blocks = new ArrayList<>();
        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());
        World world = p1.getWorld();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    var block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.AIR) continue;
                    blocks.add(new BlockDef(x - minX, y - minY, z - minZ, block.getBlockData()));
                }
            }
        }
        return blocks;
    }

    private void loadConfig() {
        file = new File(plugin.getDataFolder(), "buildingstages.yml");
        if (!file.exists()) {
            plugin.saveResource("buildingstages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        if (config.isConfigurationSection("stages")) {
            for (String building : config.getConfigurationSection("stages").getKeys(false)) {
                var buildSec = config.getConfigurationSection("stages." + building);
                if (buildSec == null) continue;
                for (String key : buildSec.getKeys(false)) {
                    var stageSec = config.getConfigurationSection("stages." + building + "." + key);
                    if (stageSec == null) continue;
                    // Determine if this child represents a stage directly (new format)
                    boolean direct = stageSec.isConfigurationSection("pos1");
                    if (direct) {
                        int stage;
                        try {
                            stage = Integer.parseInt(key);
                        } catch (NumberFormatException ex) {
                            continue;
                        }
                        loadStage(building, stage, "stages." + building + "." + key + ".");
                    } else {
                        // legacy format: levels contain stage sections
                        int level;
                        try {
                            level = Integer.parseInt(key);
                        } catch (NumberFormatException ex) {
                            continue;
                        }
                        for (String stageKey : stageSec.getKeys(false)) {
                            int stage;
                            try {
                                stage = Integer.parseInt(stageKey);
                            } catch (NumberFormatException ex) {
                                continue;
                            }
                            String base = "stages." + building + "." + key + "." + stageKey + ".";
                            loadStage(building, stage, base);
                        }
                    }
                }
            }
        }

        if (config.isConfigurationSection("links")) {
            for (String town : config.getConfigurationSection("links").getKeys(false)) {
                var tSec = config.getConfigurationSection("links." + town);
                if (tSec == null) continue;
                for (String building : tSec.getKeys(false)) {
                    int x = tSec.getInt(building + ".x", 0);
                    int y = tSec.getInt(building + ".y", 0);
                    int z = tSec.getInt(building + ".z", 0);
                    placements
                        .computeIfAbsent(town.toLowerCase(), k -> new HashMap<>())
                        .put(building.toLowerCase(), new Placement(x, y, z));
                }
            }
        }
    }

    private Location readLocation(World world, String path) {
        if (!config.isConfigurationSection(path)) return null;
        int x = config.getInt(path + ".x");
        int y = config.getInt(path + ".y");
        int z = config.getInt(path + ".z");
        return new Location(world, x, y, z);
    }

    /**
     * Helper to load a single stage definition from configuration.
     */
    private void loadStage(String building, int stage, String base) {
        World world = Bukkit.getWorld(config.getString(base + "world"));
        if (world == null) return;
        Location pos1 = readLocation(world, base + "pos1");
        Location pos2 = readLocation(world, base + "pos2");
        if (pos1 == null || pos2 == null) return;

        List<StageNpc.Definition> npcList = new ArrayList<>();
        if (config.isList(base + "npcs")) {
            for (Object o : config.getList(base + "npcs")) {
                if (!(o instanceof String s)) continue;
                try {
                    npcList.add(StageNpc.Definition.parse(s));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("[BuildingStages] Failed to parse NPC entry '" + s + "' for " + building + " stage " + stage + ": " + ex.getMessage());
                }
            }
        }
        String fileName = config.getString(base + "schematic", building.toLowerCase() + "_" + stage + ".schem");
        File schematic = new File(schemFolder, fileName);
        Map<BlockVector3, BlockData> relMap = SchematicUtil.loadSchematic(schematic, plugin.getLogger());
        List<BlockDef> blockList = new ArrayList<>();
        for (var entry : relMap.entrySet()) {
            BlockVector3 vec = entry.getKey();
            blockList.add(new BlockDef(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ(), entry.getValue()));
        }
        int hx = config.getInt(base + "holo.x", 0);
        int hy = config.getInt(base + "holo.y", 0);
        int hz = config.getInt(base + "holo.z", 0);
        int priority = config.getInt(base + "priority", 0);
        int ox = config.getInt(base + "origin.x", 0);
        int oy = config.getInt(base + "origin.y", 0);
        int oz = config.getInt(base + "origin.z", 0);

        java.util.Map<org.bukkit.Material, Integer> matCost = new java.util.HashMap<>();
        int coinCost = 0;
        if (config.isConfigurationSection(base + "upgrade_cost")) {
            var uSec = config.getConfigurationSection(base + "upgrade_cost");
            if (uSec != null) {
                coinCost = uSec.getInt("coins", 0);
                var mSec = uSec.getConfigurationSection("materials");
                if (mSec == null) {
                    // fallback to legacy key
                    mSec = uSec.getConfigurationSection("items");
                }
                if (mSec != null) {
                    for (String key : mSec.getKeys(false)) {
                        try {
                            org.bukkit.Material mat = org.bukkit.Material.valueOf(key.toUpperCase());
                            int amt = mSec.getInt(key, 0);
                            if (amt > 0) matCost.put(mat, amt);
                        } catch (IllegalArgumentException ignore) {
                        }
                    }
                }
            }
        }

        stages
            .computeIfAbsent(building.toLowerCase(), k -> new HashMap<>())
            .put(stage, new BuildingStage(building.toLowerCase(), stage,
                    pos1, pos2, npcList, blockList, schematic, fileName, priority,
                    hx, hy, hz, ox, oy, oz, matCost, coinCost));
    }

    private void saveConfig() {
        config.set("stages", null);
        for (var buildEntry : stages.entrySet()) {
            String building = buildEntry.getKey();
            for (var stageEntry : buildEntry.getValue().entrySet()) {
                int stage = stageEntry.getKey();
                BuildingStage st = stageEntry.getValue();
                String base = "stages." + building + "." + stage + ".";
                    Location p1 = st.pos1;
                    Location p2 = st.pos2;
                    config.set(base + "world", p1.getWorld().getName());
                        config.set(base + "pos1.x", p1.getBlockX());
                        config.set(base + "pos1.y", p1.getBlockY());
                        config.set(base + "pos1.z", p1.getBlockZ());
                        config.set(base + "pos2.x", p2.getBlockX());
                        config.set(base + "pos2.y", p2.getBlockY());
                        config.set(base + "pos2.z", p2.getBlockZ());
                        List<String> npcLines = new ArrayList<>();
                        for (StageNpc.Definition npc : st.npcs) {
                            npcLines.add(npc.serialize());
                        }
                        config.set(base + "npcs", npcLines);
                        config.set(base + "blocks", null); // blocks stored as schematic
                        config.set(base + "schematic", st.fileName);
                        config.set(base + "holo.x", st.hx);
                        config.set(base + "holo.y", st.hy);
                        config.set(base + "holo.z", st.hz);
                        config.set(base + "priority", st.priority);
                        config.set(base + "origin.x", st.ox);
                        config.set(base + "origin.y", st.oy);
                        config.set(base + "origin.z", st.oz);
                        if (!st.materialCost.isEmpty() || st.coinCost > 0) {
                            String ucBase = base + "upgrade_cost.";
                            config.set(ucBase + "coins", st.coinCost);
                            if (!st.materialCost.isEmpty()) {
                                for (var me : st.materialCost.entrySet()) {
                                    config.set(ucBase + "materials." + me.getKey().name().toLowerCase(), me.getValue());
                                }
                            } else {
                                config.set(ucBase + "materials", null);
                            }
                        }
            }
        }

        config.set("links", null);
        for (var townEntry : placements.entrySet()) {
            String town = townEntry.getKey();
            for (var entry : townEntry.getValue().entrySet()) {
                String building = entry.getKey();
                Placement pl = entry.getValue();
                String base = "links." + town + "." + building + ".";
                config.set(base + "x", pl.x);
                config.set(base + "y", pl.y);
                config.set(base + "z", pl.z);
            }
        }
        try {
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Data for an individual building stage area. */
    public static class BuildingStage {
        public final String name;
        public final int stage;
        public final Location pos1;
        public final Location pos2;
        public final List<StageNpc.Definition> npcs;
        public final List<BlockDef> blocks;
        public final File schematic;
        public final String fileName;
        /** Priority used when placing blocks for this stage. Higher wins. */
        public final int priority;
        public final int hx, hy, hz;
        public final int ox, oy, oz;
        public final java.util.Map<org.bukkit.Material, Integer> materialCost;
        public final int coinCost;

        public BuildingStage(String name, int stage, Location pos1, Location pos2,
                             List<StageNpc.Definition> npcs, List<BlockDef> blocks,
                             File schematic, String fileName, int priority,
                             int hx, int hy, int hz,
                             int ox, int oy, int oz,
                             java.util.Map<org.bukkit.Material, Integer> materialCost,
                             int coinCost) {
            this.name = name;
            this.stage = stage;
            this.pos1 = pos1;
            this.pos2 = pos2;
            this.npcs = npcs == null ? Collections.emptyList() : npcs;
            this.blocks = blocks == null ? Collections.emptyList() : blocks;
            this.schematic = schematic;
            this.fileName = fileName;
            this.priority = priority;
            this.hx = hx;
            this.hy = hy;
            this.hz = hz;
            this.ox = ox;
            this.oy = oy;
            this.oz = oz;
            this.materialCost = materialCost == null ? java.util.Collections.emptyMap() : materialCost;
            this.coinCost = coinCost;
        }
    }

    /** Simple block placement definition for a stage structure. */
    public static class BlockDef {
        public final int x, y, z;
        public final BlockData data;
        public BlockDef(int x, int y, int z, BlockData data) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.data = data;
        }
    }

    /** Placement offset of a building relative to the town origin. */
    public static class Placement {
        public final int x, y, z;
        public Placement(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
