package me.nakilex.levelplugin.environment.stage;

import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.lootchests.utils.LocationUtils;
import me.nakilex.levelplugin.utils.FurnitureCleanupUtil;
import me.nakilex.levelplugin.utils.NexoUtil;
import me.nakilex.levelplugin.npc.system.NpcApi;
import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.npc.system.trait.CurrentLocationTrait;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.math.BlockVector3;

import me.nakilex.levelplugin.utils.CuboidTemplate;
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
    private final Map<java.util.UUID, Map<String, List<NPC>>> spawnedNPCs = new HashMap<>();
    private final Map<java.util.UUID, Map<String, List<ItemDisplay>>> spawnedFurniture = new HashMap<>();
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

    /** Highest available stage number for the given building or 0 if none exist. */
    public int getMaxStage(String building) {
        var buildMap = stages.get(building.toLowerCase());
        if (buildMap == null || buildMap.isEmpty()) return 0;
        return buildMap.keySet().stream().max(Integer::compareTo).orElse(0);
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
        List<NPCSpawn> npcs = captureNPCs(pos1, pos2);
        List<BlockDef> blocks = captureBlocks(pos1, pos2);

        // Keep the original cuboid coordinates as the source template instead of
        // writing a schematic file. A schematic path is still retained for legacy
        // fallback if an older config provides one.
        String fileName = building.toLowerCase() + "_" + stage + ".schem";
        File schematic = new File(schemFolder, fileName);

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
                    null, 0, Collections.emptyList()));
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
        List<NPC> list = map.computeIfAbsent(key, k -> new ArrayList<>());
        for (NPC npc : list) {
            if (npc.isSpawned()) npc.despawn();
            npc.destroy();
        }
        list.clear();
        for (NPCSpawn spawn : st.npcs) {
            Location loc = origin.clone().add(
                    spawn.x - st.ox + 0.5,
                    spawn.y - st.oy + NPC_SPAWN_Y_OFFSET,
                    spawn.z - st.oz + 0.5
            );
            loc.setYaw(spawn.yaw);
            loc.setPitch(spawn.pitch);
            plugin.getLogger().info("[BuildingStageManager] Attempting to spawn stage NPC source=" + spawn.source
                    + " id=" + spawn.id + " at " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()
                    + " for building=" + building + " stage=" + stage + " viewer=" + viewer.getName());
            NPC clone = spawnFromTemplate(spawn, loc);
            if (clone == null) {
                plugin.getLogger().warning("[BuildingStageManager] Failed to spawn stage NPC source=" + spawn.source
                        + " id=" + spawn.id + " for building=" + building + " stage=" + stage);
                continue;
            }
            list.add(clone);
        }

        var furnMap = spawnedFurniture.computeIfAbsent(id, k -> new HashMap<>());
        List<ItemDisplay> furnList = furnMap.computeIfAbsent(key, k -> new ArrayList<>());
        for (ItemDisplay existing : furnList) {
            if (existing != null && !existing.isDead()) {
                NexoFurniture.remove(existing);
            }
        }
        furnList.clear();
        for (FurnitureSpawn spawn : st.furniture) {
            FurnitureMechanic mech = NexoFurniture.furnitureMechanic(spawn.id);
            if (mech == null) {
                plugin.getLogger().warning("[BuildingStageManager] Unknown furniture '" + spawn.id
                        + "' for building " + building + " stage " + stage);
                NexoUtil.logAvailableFurnitureIds(plugin.getLogger());
                continue;
            }
            Location loc = origin.clone().add(
                    spawn.x - st.ox,
                    spawn.y - st.oy,
                    spawn.z - st.oz
            );
            Location centered = LocationUtils.centerOnBlock(loc);
            if (centered == null) continue;
            FurnitureCleanupUtil.clearNearbyFurnitureEntities(plugin, centered, 4.0, "[BuildingStageManager]");
            centered.getBlock().setType(Material.AIR, false);
            ItemDisplay display = NexoFurniture.place(spawn.id, centered, 0f, spawn.facing);
            if (display != null) {
                furnList.add(display);
            }
        }
    }

    public void despawnForStage(UUID viewerId, String building, int stage) {
        var map = spawnedNPCs.get(viewerId);
        if (map == null) return;
        String key = building.toLowerCase() + ":" + stage;
        List<NPC> list = map.remove(key);
        if (list != null) {
            for (NPC npc : list) {
                if (npc.isSpawned()) npc.despawn();
                npc.destroy();
            }
        }
        if (map.isEmpty()) spawnedNPCs.remove(viewerId);

        var furnMap = spawnedFurniture.get(viewerId);
        if (furnMap == null) return;
        List<ItemDisplay> furnList = furnMap.remove(key);
        if (furnList != null) {
            for (ItemDisplay display : furnList) {
                if (display != null && !display.isDead()) {
                    NexoFurniture.remove(display);
                }
            }
        }
        if (furnMap.isEmpty()) spawnedFurniture.remove(viewerId);
    }

    public void despawnAll() {
        for (var map : spawnedNPCs.values()) {
            for (var list : map.values()) {
                for (NPC npc : list) {
                    if (npc.isSpawned()) npc.despawn();
                    npc.destroy();
                }
            }
        }
        spawnedNPCs.clear();
        for (var map : spawnedFurniture.values()) {
            for (var list : map.values()) {
                for (ItemDisplay display : list) {
                    if (display != null && !display.isDead()) {
                        NexoFurniture.remove(display);
                    }
                }
            }
        }
        spawnedFurniture.clear();
    }

    public void hideNPCsFrom(Player viewer) {
        if (viewer == null) return;
        for (var map : spawnedNPCs.values()) {
            for (var list : map.values()) {
                for (NPC npc : list) {
                    // NPCs are now public; do nothing
                }
            }
        }
    }

    private List<NPCSpawn> captureNPCs(Location p1, Location p2) {
        List<NPCSpawn> list = new ArrayList<>();
        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());
        for (NPC npc : NpcApi.getRegistry()) {
            Location l = npc.isSpawned() ? npc.getEntity().getLocation() : npc.getStoredLocation();
            if (isInsideBounds(l, p1.getWorld(), minX, maxX, minY, maxY, minZ, maxZ)) {
                list.add(new NPCSpawn(npc.getId(), NpcSource.NAKI, l.getBlockX() - minX, l.getBlockY() - minY, l.getBlockZ() - minZ, l.getYaw(), l.getPitch()));
                plugin.getLogger().info("[BuildingStageManager] Captured NAKI npc id=" + npc.getId() + " for stage selection.");
            }
        }
        for (net.citizensnpcs.api.npc.NPC npc : CitizensAPI.getNPCRegistry()) {
            Location l = npc.isSpawned() ? npc.getEntity().getLocation() : npc.getStoredLocation();
            if (isInsideBounds(l, p1.getWorld(), minX, maxX, minY, maxY, minZ, maxZ)) {
                list.add(new NPCSpawn(npc.getId(), NpcSource.CITIZENS, l.getBlockX() - minX, l.getBlockY() - minY, l.getBlockZ() - minZ, l.getYaw(), l.getPitch()));
                plugin.getLogger().info("[BuildingStageManager] Captured Citizens npc id=" + npc.getId() + " for stage selection.");
            }
        }
        plugin.getLogger().info("[BuildingStageManager] Captured total stage NPC templates=" + list.size());
        return list;
    }


    private NPC spawnFromTemplate(NPCSpawn spawn, Location loc) {
        if (spawn.source == NpcSource.CITIZENS) {
            net.citizensnpcs.api.npc.NPC template = CitizensAPI.getNPCRegistry().getById(spawn.id);
            if (template == null) return null;
            org.bukkit.entity.EntityType type = template.isSpawned() && template.getEntity() != null
                    ? template.getEntity().getType()
                    : org.bukkit.entity.EntityType.PLAYER;
            NPC clone = NpcApi.getRegistry().createNpc(type, template.getName());
            clone.setPersistent(false);
            clone.getOrAddTrait(CurrentLocationTrait.class).setLocation(loc);
            clone.spawn(loc);
            if (clone.isSpawned()) {
                clone.getEntity().teleport(loc, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                clone.getEntity().setGravity(false);
            }
            return clone;
        }
        NPC template = NpcApi.getRegistry().getById(spawn.id);
        if (template == null) return null;
        NPC clone = NpcApi.getRegistry().cloneNpc(template);
        clone.getOrAddTrait(CurrentLocationTrait.class).setLocation(loc);
        clone.spawn(loc);
        if (clone.isSpawned()) {
            clone.getEntity().teleport(loc, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
            clone.getEntity().setGravity(false);
        }
        return clone;
    }

    private boolean isInsideBounds(Location l, World world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        if (l == null || world == null || !world.equals(l.getWorld())) return false;
        int x = l.getBlockX();
        int y = l.getBlockY();
        int z = l.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    private List<BlockDef> captureBlocks(Location p1, Location p2) {
        return toBlockDefs(CuboidTemplate.capture(p1, p2).blocks());
    }

    private List<BlockDef> toBlockDefs(List<CuboidTemplate.BlockCopy> copies) {
        List<BlockDef> blocks = new ArrayList<>();
        if (copies == null) {
            return blocks;
        }
        for (CuboidTemplate.BlockCopy copy : copies) {
            blocks.add(new BlockDef(copy.x(), copy.y(), copy.z(), copy.data()));
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

        List<NPCSpawn> npcList = new ArrayList<>();
        if (config.isList(base + "npcs")) {
            for (Object o : config.getList(base + "npcs")) {
                if (!(o instanceof String s)) continue;
                String[] parts = s.split(";");
                if (parts.length < 6) continue;
                try {
                    int id = Integer.parseInt(parts[0]);
                    int dx = Integer.parseInt(parts[1]);
                    int dy = Integer.parseInt(parts[2]);
                    int dz = Integer.parseInt(parts[3]);
                    float yaw = Float.parseFloat(parts[4]);
                    float pitch = Float.parseFloat(parts[5]);
                    NpcSource source = parts.length >= 7 ? NpcSource.fromKey(parts[6]) : NpcSource.NAKI;
                    npcList.add(new NPCSpawn(id, source, dx, dy, dz, yaw, pitch));
                } catch (Exception ignore) {
                }
            }
        }
        List<FurnitureSpawn> furnitureList = new ArrayList<>();
        if (config.isList(base + "furniture")) {
            for (Object o : config.getList(base + "furniture")) {
                if (!(o instanceof String s)) continue;
                String[] parts = s.split(";");
                if (parts.length < 4) continue;
                String id = parts[0];
                try {
                    int dx = Integer.parseInt(parts[1]);
                    int dy = Integer.parseInt(parts[2]);
                    int dz = Integer.parseInt(parts[3]);
                    BlockFace facing = BlockFace.NORTH;
                    if (parts.length >= 5) {
                        try {
                            facing = BlockFace.valueOf(parts[4].toUpperCase());
                        } catch (IllegalArgumentException ignore) {
                            facing = BlockFace.NORTH;
                        }
                    }
                    furnitureList.add(new FurnitureSpawn(id, dx, dy, dz, facing));
                } catch (Exception ignore) {
                }
            }
        }
        String fileName = config.getString(base + "schematic", building.toLowerCase() + "_" + stage + ".schem");
        File schematic = new File(schemFolder, fileName);
        List<BlockDef> blockList = loadTemplateBlocks(pos1, pos2, schematic);
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
                    hx, hy, hz, ox, oy, oz, matCost, coinCost, furnitureList));
    }

    private List<BlockDef> loadTemplateBlocks(Location pos1, Location pos2, File schematic) {
        if (pos1 != null && pos2 != null && pos1.getWorld() != null && pos1.getWorld().equals(pos2.getWorld())) {
            return captureBlocks(pos1, pos2);
        }
        if (schematic == null) {
            return List.of();
        }
        Map<BlockVector3, BlockData> relMap = SchematicUtil.loadSchematic(schematic, plugin.getLogger());
        List<BlockDef> blockList = new ArrayList<>();
        for (var entry : relMap.entrySet()) {
            BlockVector3 vec = entry.getKey();
            blockList.add(new BlockDef(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ(), entry.getValue()));
        }
        return blockList;
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
                        for (NPCSpawn npc : st.npcs) {
                            npcLines.add(npc.id + ";" + npc.x + ";" + npc.y + ";" + npc.z + ";" + npc.yaw + ";" + npc.pitch + ";" + npc.source.key);
                        }
                        config.set(base + "npcs", npcLines);
                        List<String> furnitureLines = new ArrayList<>();
                        for (FurnitureSpawn spawn : st.furniture) {
                            furnitureLines.add(spawn.id + ";" + spawn.x + ";" + spawn.y + ";" + spawn.z + ";" + spawn.facing.name());
                        }
                        config.set(base + "furniture", furnitureLines);
                        config.set(base + "blocks", null); // blocks are captured from source cuboid at load time
                        config.set(base + "schematic", null);
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

    public enum NpcSource {
        NAKI("naki"),
        CITIZENS("citizens");

        private final String key;

        NpcSource(String key) { this.key = key; }

        public static NpcSource fromKey(String key) {
            if (key == null) return NAKI;
            for (NpcSource value : values()) {
                if (value.key.equalsIgnoreCase(key)) return value;
            }
            return NAKI;
        }
    }

    /** NPC spawn position relative to stage origin. */
    public static class NPCSpawn {
        public final int id;
        public final NpcSource source;
        public final int x, y, z;
        public final float yaw, pitch;
        public NPCSpawn(int id, NpcSource source, int x, int y, int z, float yaw, float pitch) {
            this.id = id;
            this.source = source == null ? NpcSource.NAKI : source;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    /** Data for an individual building stage area. */
    public static class BuildingStage {
        public final String name;
        public final int stage;
        public final Location pos1;
        public final Location pos2;
        public final List<NPCSpawn> npcs;
        public final List<BlockDef> blocks;
        public final File schematic;
        public final String fileName;
        /** Priority used when placing blocks for this stage. Higher wins. */
        public final int priority;
        public final int hx, hy, hz;
        public final int ox, oy, oz;
        public final java.util.Map<org.bukkit.Material, Integer> materialCost;
        public final int coinCost;
        public final java.util.List<FurnitureSpawn> furniture;

        public BuildingStage(String name, int stage, Location pos1, Location pos2,
                             List<NPCSpawn> npcs, List<BlockDef> blocks,
                             File schematic, String fileName, int priority,
                             int hx, int hy, int hz,
                             int ox, int oy, int oz,
                             java.util.Map<org.bukkit.Material, Integer> materialCost,
                             int coinCost,
                             java.util.List<FurnitureSpawn> furniture) {
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
            this.furniture = furniture == null ? java.util.Collections.emptyList() : furniture;
        }
    }

    /** Nexo furniture spawn definition relative to stage origin. */
    public static class FurnitureSpawn {
        public final String id;
        public final int x, y, z;
        public final BlockFace facing;
        public FurnitureSpawn(String id, int x, int y, int z, BlockFace facing) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
            this.facing = facing == null ? BlockFace.NORTH : facing;
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
