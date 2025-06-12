package me.nakilex.levelplugin.environment.stage;

import me.nakilex.levelplugin.Main;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.CurrentLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

/**
 * Stores building stage data and handles spawning the stage structures/NPCs.
 */
public class BuildingStageManager {
    private final Main plugin;
    /** Map of town -> building -> level -> stage -> data */
    private final Map<String, Map<String, Map<Integer, Map<Integer, BuildingStage>>>> stages = new HashMap<>();
    private final Map<java.util.UUID, Map<String, List<NPC>>> spawnedNPCs = new HashMap<>();
    private File file;
    private FileConfiguration config;

    public BuildingStageManager(Main plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /** All defined stage names across all towns. */
    public Set<String> getStageNames() {
        Set<String> names = new HashSet<>();
        for (Map<String, Map<Integer, Map<Integer, BuildingStage>>> town : stages.values()) {
            names.addAll(town.keySet());
        }
        return names;
    }

    public BuildingStage getStage(String town, String building, int level, int stage) {
        if (town == null) return null;
        var townMap = stages.get(town.toLowerCase());
        if (townMap == null) return null;
        var buildMap = townMap.get(building.toLowerCase());
        if (buildMap == null) return null;
        var levelMap = buildMap.get(level);
        if (levelMap == null) return null;
        return levelMap.get(stage);
    }

    /** Return all building names defined for a town. */
    public Set<String> getBuildings(String town) {
        var map = stages.get(town.toLowerCase());
        if (map == null) return Collections.emptySet();
        return new HashSet<>(map.keySet());
    }

    /** Create a new stage from the selected area. */
    public void createStage(String town, String building, int level, int stage,
                            Location pos1, Location pos2, Location standLoc) {
        List<NPCSpawn> npcs = captureNPCs(pos1, pos2);
        List<BlockDef> blocks = captureBlocks(pos1, pos2);

        int hx = standLoc.getBlockX() - pos1.getBlockX();
        int hy = standLoc.getBlockY() - pos1.getBlockY();
        int hz = standLoc.getBlockZ() - pos1.getBlockZ();

        stages
            .computeIfAbsent(town.toLowerCase(), k -> new HashMap<>())
            .computeIfAbsent(building.toLowerCase(), k -> new HashMap<>())
            .computeIfAbsent(level, k -> new HashMap<>())
            .put(stage, new BuildingStage(building.toLowerCase(), level, stage, pos1, pos2, npcs, blocks, hx, hy, hz));
        saveConfig();
    }

    public boolean removeStage(String town, String building, int level, int stage) {
        var townMap = stages.get(town.toLowerCase());
        if (townMap == null) return false;
        var buildMap = townMap.get(building.toLowerCase());
        if (buildMap == null) return false;
        var levelMap = buildMap.get(level);
        if (levelMap == null) return false;
        if (levelMap.remove(stage) != null) {
            if (levelMap.isEmpty()) buildMap.remove(level);
            if (buildMap.isEmpty()) townMap.remove(building.toLowerCase());
            if (townMap.isEmpty()) stages.remove(town.toLowerCase());
            saveConfig();
            return true;
        }
        return false;
    }

    // Raise spawned NPCs slightly so they don't clip into the ground
    private static final double NPC_SPAWN_Y_OFFSET = 2.0;

    public void spawnForStage(Player viewer, String town, String building, int level,
                              int stage, Location origin) {
        BuildingStage st = getStage(town, building, level, stage);
        if (st == null || origin == null || viewer == null) return;
        UUID id = viewer.getUniqueId();
        var map = spawnedNPCs.computeIfAbsent(id, k -> new HashMap<>());
        String key = town.toLowerCase() + ":" + building.toLowerCase() + ":" + level + ":" + stage;
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
            Location loc = origin.clone().add(spawn.x + 0.5, spawn.y + NPC_SPAWN_Y_OFFSET, spawn.z + 0.5);
            loc.setYaw(spawn.yaw);
            loc.setPitch(spawn.pitch);
            clone.getOrAddTrait(CurrentLocation.class).setLocation(loc);
            clone.spawn(loc);
            if (clone.isSpawned()) {
                clone.getEntity().teleport(loc, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(viewer)) {
                    p.hideEntity(plugin, clone.getEntity());
                }
            }
            list.add(clone);
        }
    }

    public void despawnForStage(UUID viewerId, String town, String building, int level, int stage) {
        var map = spawnedNPCs.get(viewerId);
        if (map == null) return;
        String key = town.toLowerCase() + ":" + building.toLowerCase() + ":" + level + ":" + stage;
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

    private void loadConfig() {
        file = new File(plugin.getDataFolder(), "buildingstages.yml");
        if (!file.exists()) {
            plugin.saveResource("buildingstages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        if (!config.isConfigurationSection("stages")) return;
        for (String town : config.getConfigurationSection("stages").getKeys(false)) {
            var townSec = config.getConfigurationSection("stages." + town);
            if (townSec == null) continue;
            for (String building : townSec.getKeys(false)) {
                var buildSec = config.getConfigurationSection("stages." + town + "." + building);
                if (buildSec == null) continue;
                for (String lvlKey : buildSec.getKeys(false)) {
                    int level;
                    try {
                        level = Integer.parseInt(lvlKey);
                    } catch (NumberFormatException ex) {
                        continue;
                    }
                    var lvlSec = config.getConfigurationSection("stages." + town + "." + building + "." + lvlKey);
                    if (lvlSec == null) continue;
                    for (String stageKey : lvlSec.getKeys(false)) {
                        int stage;
                        try {
                            stage = Integer.parseInt(stageKey);
                        } catch (NumberFormatException ex) {
                            continue;
                        }
                        String base = "stages." + town + "." + building + "." + lvlKey + "." + stageKey + ".";
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
                        int hx = config.getInt(base + "holo.x", 0);
                        int hy = config.getInt(base + "holo.y", 0);
                        int hz = config.getInt(base + "holo.z", 0);
                        stages
                            .computeIfAbsent(town.toLowerCase(), k -> new HashMap<>())
                            .computeIfAbsent(building.toLowerCase(), k -> new HashMap<>())
                            .computeIfAbsent(level, k -> new HashMap<>())
                            .put(stage, new BuildingStage(building.toLowerCase(), level, stage, pos1, pos2, npcList, blockList, hx, hy, hz));
                    }
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
        for (var townEntry : stages.entrySet()) {
            String town = townEntry.getKey();
            for (var buildEntry : townEntry.getValue().entrySet()) {
                String building = buildEntry.getKey();
                for (var levelEntry : buildEntry.getValue().entrySet()) {
                    int level = levelEntry.getKey();
                    for (var stageEntry : levelEntry.getValue().entrySet()) {
                        int stage = stageEntry.getKey();
                        BuildingStage st = stageEntry.getValue();
                        String base = "stages." + town + "." + building + "." + level + "." + stage + ".";
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
                        config.set(base + "holo.x", st.hx);
                        config.set(base + "holo.y", st.hy);
                        config.set(base + "holo.z", st.hz);
                    }
                }
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
        public final int hx, hy, hz;
        public BuildingStage(String name, int level, int stage, Location pos1, Location pos2,
                             List<NPCSpawn> npcs, List<BlockDef> blocks,
                             int hx, int hy, int hz) {
            this.name = name;
            this.level = level;
            this.stage = stage;
            this.pos1 = pos1;
            this.pos2 = pos2;
            this.npcs = npcs == null ? Collections.emptyList() : npcs;
            this.blocks = blocks == null ? Collections.emptyList() : blocks;
            this.hx = hx;
            this.hy = hy;
            this.hz = hz;
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
}
