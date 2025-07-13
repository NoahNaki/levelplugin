package me.nakilex.levelplugin.environment.stage;

import me.nakilex.levelplugin.Main;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.CurrentLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.EditSession;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * Stores building stage data and handles spawning the stage structures/NPCs.
 */
public class BuildingStageManager {
    private final Main plugin;
    /** Map of building -> level -> stage -> data */
    private final Map<String, Map<Integer, Map<Integer, BuildingStage>>> stages = new HashMap<>();
    /** Map of town -> building -> placement offset */
    private final Map<String, Map<String, Placement>> placements = new HashMap<>();
    private final Map<java.util.UUID, Map<String, List<NPC>>> spawnedNPCs = new HashMap<>();
    private File file;
    private FileConfiguration config;

    public BuildingStageManager(Main plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /** All defined building names. */
    public Set<String> getStageNames() {
        return new HashSet<>(stages.keySet());
    }

    public BuildingStage getStage(String building, int level, int stage) {
        var buildMap = stages.get(building.toLowerCase());
        if (buildMap == null) return null;
        var levelMap = buildMap.get(level);
        if (levelMap == null) return null;
        return levelMap.get(stage);
    }

    /** Return all building names defined for a town. */
    public Set<String> getBuildings(String town) {
        var map = placements.get(town.toLowerCase());
        if (map == null) return Collections.emptySet();
        return new HashSet<>(map.keySet());
    }

    /** Create a new stage from the selected area. */
    public void createStage(String building, int level, int stage,
                            Location pos1, Location pos2, Location standLoc,
                            Location origin) {
        List<NPCSpawn> npcs = captureNPCs(pos1, pos2);
        List<BlockDef> blocks = captureBlocks(pos1, pos2);

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
            .computeIfAbsent(level, k -> new HashMap<>())
            .put(stage, new BuildingStage(building.toLowerCase(), level, stage, pos1, pos2,
                    npcs, blocks, null, hx, hy, hz, ox, oy, oz));
        saveConfig();
    }

    public boolean removeStage(String building, int level, int stage) {
        var buildMap = stages.get(building.toLowerCase());
        if (buildMap == null) return false;
        var levelMap = buildMap.get(level);
        if (levelMap == null) return false;
        if (levelMap.remove(stage) != null) {
            if (levelMap.isEmpty()) buildMap.remove(level);
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
        BuildingStage st = getStage(building, 1, 1);
        if (st == null) return null;
        int minX = Math.min(st.pos1.getBlockX(), st.pos2.getBlockX());
        int minY = Math.min(st.pos1.getBlockY(), st.pos2.getBlockY());
        int minZ = Math.min(st.pos1.getBlockZ(), st.pos2.getBlockZ());
        return new Location(st.pos1.getWorld(), minX + st.ox, minY + st.oy, minZ + st.oz);
    }

    // Offset for spawning NPCs. Use zero so they stand directly on the ground.
    private static final double NPC_SPAWN_Y_OFFSET = 0.0;

    public void spawnForStage(Player viewer, String building, int level,
                              int stage, Location origin) {
        BuildingStage st = getStage(building, level, stage);
        if (st == null || origin == null || viewer == null) return;
        UUID id = viewer.getUniqueId();
        var map = spawnedNPCs.computeIfAbsent(id, k -> new HashMap<>());
        String key = building.toLowerCase() + ":" + level + ":" + stage;
        List<NPC> list = map.computeIfAbsent(key, k -> new ArrayList<>());
        for (NPC npc : list) {
            if (npc.isSpawned()) npc.despawn();
            npc.destroy();
        }
        list.clear();
        for (NPCSpawn spawn : st.npcs) {
            NPC template = CitizensAPI.getNPCRegistry().getById(spawn.id);
            if (template == null) continue;
            NPC clone = template.copy();
            Location loc = origin.clone().add(
                    spawn.x - st.ox + 0.5,
                    spawn.y - st.oy + NPC_SPAWN_Y_OFFSET,
                    spawn.z - st.oz + 0.5
            );
            loc.setYaw(spawn.yaw);
            loc.setPitch(spawn.pitch);
            clone.getOrAddTrait(CurrentLocation.class).setLocation(loc);
            clone.spawn(loc);
            if (clone.isSpawned()) {
                clone.getEntity().teleport(loc, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                clone.getEntity().setGravity(false);
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(viewer)) {
                    p.hideEntity(plugin, clone.getEntity());
                }
            }
            list.add(clone);
        }
    }

    public void despawnForStage(UUID viewerId, String building, int level, int stage) {
        var map = spawnedNPCs.get(viewerId);
        if (map == null) return;
        String key = building.toLowerCase() + ":" + level + ":" + stage;
        List<NPC> list = map.remove(key);
        if (list != null) {
            for (NPC npc : list) {
                if (npc.isSpawned()) npc.despawn();
                npc.destroy();
            }
        }
        if (map.isEmpty()) spawnedNPCs.remove(viewerId);
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
    }

    public void hideNPCsFrom(Player viewer) {
        if (viewer == null) return;
        for (var map : spawnedNPCs.values()) {
            for (var list : map.values()) {
                for (NPC npc : list) {
                    if (npc.isSpawned()) viewer.hideEntity(plugin, npc.getEntity());
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
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            Location l = npc.isSpawned() ? npc.getEntity().getLocation() : npc.getStoredLocation();
            if (l == null || !l.getWorld().equals(p1.getWorld())) continue;
            int x = l.getBlockX();
            int y = l.getBlockY();
            int z = l.getBlockZ();
            if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ) {
                list.add(new NPCSpawn(npc.getId(), x - minX, y - minY, z - minZ, l.getYaw(), l.getPitch()));
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

    /** Paste a schematic file at the specified origin using FastAsyncWorldEdit. */
    public void pasteSchematic(java.io.File file, Location origin) {
        try {
            ClipboardFormat format = ClipboardFormats.findByFile(file);
            if (format == null) return;
            try (ClipboardReader reader = format.getReader(new java.io.FileInputStream(file))) {
                Clipboard clipboard = reader.read();
                com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(origin.getWorld());
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                    Operation op = new ClipboardHolder(clipboard)
                            .createPaste(editSession)
                            .to(BlockVector3.at(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ()))
                            .ignoreAirBlocks(true)
                            .build();
                    Operations.complete(op);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                for (String lvlKey : buildSec.getKeys(false)) {
                    int level;
                    try {
                        level = Integer.parseInt(lvlKey);
                    } catch (NumberFormatException ex) {
                        continue;
                    }
                    var lvlSec = config.getConfigurationSection("stages." + building + "." + lvlKey);
                    if (lvlSec == null) continue;
                    for (String stageKey : lvlSec.getKeys(false)) {
                        int stage;
                        try {
                            stage = Integer.parseInt(stageKey);
                        } catch (NumberFormatException ex) {
                            continue;
                        }
                        String base = "stages." + building + "." + lvlKey + "." + stageKey + ".";
                        World world = Bukkit.getWorld(config.getString(base + "world"));
                        if (world == null) continue;
                        Location pos1 = readLocation(world, base + "pos1");
                        Location pos2 = readLocation(world, base + "pos2");
                        if (pos1 == null || pos2 == null) continue;
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
                                    npcList.add(new NPCSpawn(id, dx, dy, dz, yaw, pitch));
                                } catch (Exception ignore) {}
                            }
                        }
                        List<BlockDef> blockList = new ArrayList<>();
                        if (config.isList(base + "blocks")) {
                            for (String line : config.getStringList(base + "blocks")) {
                                String[] parts = line.split(";");
                                if (parts.length < 4) continue;
                                try {
                                    int dx = Integer.parseInt(parts[0]);
                                    int dy = Integer.parseInt(parts[1]);
                                    int dz = Integer.parseInt(parts[2]);
                                    BlockData data = Bukkit.createBlockData(parts[3]);
                                    blockList.add(new BlockDef(dx, dy, dz, data));
                                } catch (Exception ignore) {}
                            }
                        }
                        java.io.File schemFile = null;
                        String schemPath = config.getString(base + "schem");
                        if (schemPath != null) {
                            schemFile = new File(plugin.getDataFolder(), schemPath);
                        }
                        int hx = config.getInt(base + "holo.x", 0);
                        int hy = config.getInt(base + "holo.y", 0);
                        int hz = config.getInt(base + "holo.z", 0);
                        int ox = config.getInt(base + "origin.x", 0);
                        int oy = config.getInt(base + "origin.y", 0);
                        int oz = config.getInt(base + "origin.z", 0);
                        stages
                            .computeIfAbsent(building.toLowerCase(), k -> new HashMap<>())
                            .computeIfAbsent(level, k -> new HashMap<>())
                            .put(stage, new BuildingStage(building.toLowerCase(), level, stage,
                                    pos1, pos2, npcList, blockList, schemFile, hx, hy, hz, ox, oy, oz));
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

    private void saveConfig() {
        config.set("stages", null);
        for (var buildEntry : stages.entrySet()) {
            String building = buildEntry.getKey();
            for (var levelEntry : buildEntry.getValue().entrySet()) {
                int level = levelEntry.getKey();
                for (var stageEntry : levelEntry.getValue().entrySet()) {
                    int stage = stageEntry.getKey();
                    BuildingStage st = stageEntry.getValue();
                    String base = "stages." + building + "." + level + "." + stage + ".";
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
                            npcLines.add(npc.id + ";" + npc.x + ";" + npc.y + ";" + npc.z + ";" + npc.yaw + ";" + npc.pitch);
                        }
                        config.set(base + "npcs", npcLines);
                        List<String> blockLines = new ArrayList<>();
                        for (BlockDef b : st.blocks) {
                            blockLines.add(b.x + ";" + b.y + ";" + b.z + ";" + b.data.getAsString());
                        }
                        config.set(base + "blocks", blockLines);
                        if (st.schematic != null) {
                            config.set(base + "schem", st.schematic.getName());
                        }
                        config.set(base + "holo.x", st.hx);
                        config.set(base + "holo.y", st.hy);
                        config.set(base + "holo.z", st.hz);
                        config.set(base + "origin.x", st.ox);
                        config.set(base + "origin.y", st.oy);
                        config.set(base + "origin.z", st.oz);
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

    /** NPC spawn position relative to stage origin. */
    public static class NPCSpawn {
        public final int id;
        public final int x, y, z;
        public final float yaw, pitch;
        public NPCSpawn(int id, int x, int y, int z, float yaw, float pitch) {
            this.id = id;
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
        public final int level;
        public final int stage;
        public final Location pos1;
        public final Location pos2;
        public final List<NPCSpawn> npcs;
        public final List<BlockDef> blocks;
        /** Optional schematic file used instead of block definitions. */
        public final java.io.File schematic;
        public final int hx, hy, hz;
        public final int ox, oy, oz;
        public BuildingStage(String name, int level, int stage, Location pos1, Location pos2,
                             List<NPCSpawn> npcs, List<BlockDef> blocks,
                             java.io.File schematic,
                             int hx, int hy, int hz,
                             int ox, int oy, int oz) {
            this.name = name;
            this.level = level;
            this.stage = stage;
            this.pos1 = pos1;
            this.pos2 = pos2;
            this.npcs = npcs == null ? Collections.emptyList() : npcs;
            this.blocks = blocks == null ? Collections.emptyList() : blocks;
            this.schematic = schematic;
            this.hx = hx;
            this.hy = hy;
            this.hz = hz;
            this.ox = ox;
            this.oy = oy;
            this.oz = oz;
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
