package me.nakilex.levelplugin.environment.stage;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import me.nakilex.levelplugin.npc.system.NpcApi;
import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.npc.system.trait.CurrentLocationTrait;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.sk89q.worldedit.math.BlockVector3;

import me.nakilex.levelplugin.utils.SchematicUtil;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Handles storage of environment stage areas for settlements.
 */
public class TownStageManager {
    private final Main plugin;
    /** Map of town -> level -> stage -> data */
    private final Map<String, Map<Integer, Map<Integer, TownStage>>> stages = new HashMap<>();
    private final Map<java.util.UUID, Map<String, java.util.List<NPC>>> spawnedNPCs = new HashMap<>();
    /** Folder containing FAWE schematics for each stage. */
    private final File schemFolder;
    private File file;
    private FileConfiguration config;

    public TownStageManager(Main plugin) {
        this.plugin = plugin;
        this.schemFolder = new File(plugin.getDataFolder(), "town_schematics");
        if (!schemFolder.exists()) schemFolder.mkdirs();
        loadFromConfig();
    }

    /** Returns all defined town names. */
    public Set<String> getStageNames() {
        return new java.util.HashSet<>(stages.keySet());
    }

    public Set<Integer> getLevels(String town) {
        var levels = stages.get(town.toLowerCase());
        if (levels == null) return Collections.emptySet();
        return new HashSet<>(levels.keySet());
        
    }

    public TownStage getStage(String town, int level, int stage) {
        if (town == null) return null;
        var levels = stages.get(town.toLowerCase());
        if (levels == null) return null;
        var stagesMap = levels.get(level);
        if (stagesMap == null) return null;
        return stagesMap.get(stage);
    }

    /** Highest configured stage for a town across all levels, or {@code null} if none exist. */
    public TownStage getHighestStage(String town) {
        if (town == null) return null;
        var levels = stages.get(town.toLowerCase());
        if (levels == null || levels.isEmpty()) return null;

        int maxLevel = levels.keySet().stream().max(Integer::compareTo).orElse(0);
        var stageMap = levels.get(maxLevel);
        if (stageMap == null || stageMap.isEmpty()) return null;

        int maxStage = stageMap.keySet().stream().max(Integer::compareTo).orElse(0);
        return stageMap.get(maxStage);
    }

    public void createStage(String name, int level, int stage, Location p1, Location p2, Location origin, int priority) {
        java.util.List<NPCSpawn> npcs = new java.util.ArrayList<>();
        java.util.List<BlockDef> blocks = new java.util.ArrayList<>();
        var boxMinX = Math.min(p1.getBlockX(), p2.getBlockX());
        var boxMaxX = Math.max(p1.getBlockX(), p2.getBlockX());
        var boxMinY = Math.min(p1.getBlockY(), p2.getBlockY());
        var boxMaxY = Math.max(p1.getBlockY(), p2.getBlockY());
        var boxMinZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        var boxMaxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());
        for (NPC npc : NpcApi.getRegistry()) {
            Location l = npc.isSpawned() ? npc.getEntity().getLocation() : npc.getStoredLocation();
            if (l == null) continue;
            if (!l.getWorld().equals(p1.getWorld())) continue;
            int x = l.getBlockX();
            int y = l.getBlockY();
            int z = l.getBlockZ();
            if (x >= boxMinX && x <= boxMaxX && y >= boxMinY && y <= boxMaxY && z >= boxMinZ && z <= boxMaxZ) {
                npcs.add(new NPCSpawn(
                        npc.getId(),
                        x - boxMinX,
                        y - boxMinY,
                        z - boxMinZ,
                        l.getYaw(),
                        l.getPitch()
                ));
            }
        }

        int ox = origin.getBlockX() - boxMinX;
        int oy = origin.getBlockY() - boxMinY;
        int oz = origin.getBlockZ() - boxMinZ;

        var world = p1.getWorld();
        for (int x = boxMinX; x <= boxMaxX; x++) {
            for (int y = boxMinY; y <= boxMaxY; y++) {
                for (int z = boxMinZ; z <= boxMaxZ; z++) {
                    var block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.AIR) continue;
                    BlockData data = block.getBlockData();
                    blocks.add(new BlockDef(x - boxMinX, y - boxMinY, z - boxMinZ, data));
                }
            }
        }

        String fileName = name.toLowerCase() + "_" + level + "_" + stage + ".schem";
        File schematic = new File(schemFolder, fileName);
        SchematicUtil.saveSchematic(p1, p2, schematic, plugin.getLogger());
        stages
            .computeIfAbsent(name.toLowerCase(), k -> new java.util.HashMap<>())
            .computeIfAbsent(level, k -> new java.util.HashMap<>())
            .put(stage, new TownStage(name.toLowerCase(), level, stage, p1, p2, npcs, blocks, schematic, fileName, priority, ox, oy, oz));
        saveConfig();
    }

    public boolean removeStage(String name, int level, int stage) {
        var levels = stages.get(name.toLowerCase());
        if (levels == null) return false;
        var map = levels.get(level);
        if (map == null) return false;
        if (map.remove(stage) != null) {
            if (map.isEmpty()) levels.remove(level);
            if (levels.isEmpty()) stages.remove(name.toLowerCase());
            saveConfig();
            return true;
        }
        return false;
    }

    // NPCs should stand directly on the ground
    private static final double NPC_SPAWN_Y_OFFSET = 0.0;

    public void spawnForStage(org.bukkit.entity.Player viewer, String town, int level, int stage, Location origin) {
        TownStage ts = getStage(town, level, stage);
        if (ts == null || origin == null || viewer == null) return;
        java.util.UUID id = viewer.getUniqueId();
        var map = spawnedNPCs.computeIfAbsent(id, k -> new java.util.HashMap<>());
        String key = town.toLowerCase() + ":" + level + ":" + stage;
        var list = map.computeIfAbsent(key, k -> new java.util.ArrayList<>());
        for (NPC npc : list) {
            if (npc.isSpawned()) npc.despawn();
            npc.destroy();
        }
        list.clear();
        for (NPCSpawn ns : ts.npcs) {
            NPC template = NpcApi.getRegistry().getById(ns.id);
            if (template == null) {
                plugin.getLogger().warning("NPC template with ID " + ns.id + " not found while spawning stage NPCs");
                continue;
            }
            // Clone the template NPC so staged spawns retain base metadata
            NPC clone = NpcApi.getRegistry().cloneNpc(template);

            // Translate original NPC position relative to the player's town
            // origin. Add a Y offset so the NPC doesn't spawn partially in the ground.
            Location loc = origin.clone().add(
                    ns.x - ts.ox + 0.5,
                    ns.y - ts.oy + NPC_SPAWN_Y_OFFSET,
                    ns.z - ts.oz + 0.5
            );
            loc.setYaw(ns.yaw);
            loc.setPitch(ns.pitch);

            clone.getOrAddTrait(CurrentLocationTrait.class).setLocation(loc);
            plugin.getLogger().info("Spawning NPC clone from template " + ns.id + " at "
                    + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()
                    + " for " + viewer.getName());
            clone.spawn(loc);
            if (clone.isSpawned()) {
                clone.getEntity().teleport(loc, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                clone.getEntity().setGravity(false);
            }
            list.add(clone);
        }
    }

    public void despawnForStage(java.util.UUID viewerId, String town, int level, int stage) {
        var map = spawnedNPCs.get(viewerId);
        if (map == null) return;
        String key = town.toLowerCase() + ":" + level + ":" + stage;
        var list = map.remove(key);
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

    /** Hide any player-specific NPCs from the given viewer. */
    public void hideNPCsFrom(org.bukkit.entity.Player viewer) {
        if (viewer == null) return;
        for (var map : spawnedNPCs.values()) {
            for (var list : map.values()) {
                for (NPC npc : list) {
                    // NPCs are now public; do nothing
                }
            }
        }
    }

    private void loadFromConfig() {
        file = new File(plugin.getDataFolder(), "townstages.yml");
        if (!file.exists()) {
            plugin.saveResource("townstages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        if (!config.isConfigurationSection("stages")) return;
        for (String town : config.getConfigurationSection("stages").getKeys(false)) {
            var townSec = config.getConfigurationSection("stages." + town);
            if (townSec == null) continue;
            for (String lvlKey : townSec.getKeys(false)) {
                int level;
                try {
                    level = Integer.parseInt(lvlKey);
                } catch (NumberFormatException ex) {
                    continue; // skip non-numeric keys
                }
                var lvlSec = config.getConfigurationSection("stages." + town + "." + lvlKey);
                if (lvlSec == null) continue;
                for (String stKey : lvlSec.getKeys(false)) {
                    int stage;
                    try {
                        stage = Integer.parseInt(stKey);
                    } catch (NumberFormatException ex) {
                        continue; // skip invalid stage keys
                    }
                    String base = "stages." + town + "." + lvlKey + "." + stKey + ".";
                    String worldName = config.getString(base + "world");
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) continue;
                    Location p1 = readLocation(world, base + "pos1");
                    Location p2 = readLocation(world, base + "pos2");
                    if (p1 == null || p2 == null) continue;
                    java.util.List<NPCSpawn> npcs = new java.util.ArrayList<>();
                    java.util.List<BlockDef> blocks;
                    if (config.isList(base + "npcs")) {
                        for (var o : config.getList(base + "npcs")) {
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
                                npcs.add(new NPCSpawn(id, dx, dy, dz, yaw, pitch));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                    String fileName = config.getString(base + "schematic", town.toLowerCase() + "_" + level + "_" + stage + ".schem");
                    File schematic = new File(schemFolder, fileName);
                    Map<BlockVector3, BlockData> rel = SchematicUtil.loadSchematic(schematic, plugin.getLogger());
                    blocks = new java.util.ArrayList<>();
                    for (var entry : rel.entrySet()) {
                        BlockVector3 vec = entry.getKey();
                        blocks.add(new BlockDef(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ(), entry.getValue()));
                    }
                    int priority = config.getInt(base + "priority", 0);
                    int ox = config.getInt(base + "origin.x", 0);
                    int oy = config.getInt(base + "origin.y", 0);
                    int oz = config.getInt(base + "origin.z", 0);
                    stages
                        .computeIfAbsent(town.toLowerCase(), k -> new java.util.HashMap<>())
                        .computeIfAbsent(level, k -> new java.util.HashMap<>())
                        .put(stage, new TownStage(town.toLowerCase(), level, stage, p1, p2, npcs, blocks, schematic, fileName, priority, ox, oy, oz));
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
        for (var entryTown : stages.entrySet()) {
            String town = entryTown.getKey();
            for (var entryLevel : entryTown.getValue().entrySet()) {
                int level = entryLevel.getKey();
                for (var entryStage : entryLevel.getValue().entrySet()) {
                    int stage = entryStage.getKey();
                    TownStage st = entryStage.getValue();
                    String base = "stages." + town + "." + level + "." + stage + ".";
                    Location p1 = st.pos1;
                    Location p2 = st.pos2;
                    config.set(base + "world", p1.getWorld().getName());
                    config.set(base + "pos1.x", p1.getBlockX());
                    config.set(base + "pos1.y", p1.getBlockY());
                    config.set(base + "pos1.z", p1.getBlockZ());
                    config.set(base + "pos2.x", p2.getBlockX());
                    config.set(base + "pos2.y", p2.getBlockY());
                    config.set(base + "pos2.z", p2.getBlockZ());
                    java.util.List<String> list = new java.util.ArrayList<>();
                    for (NPCSpawn npc : st.npcs) {
                        list.add(npc.id + ";" + npc.x + ";" + npc.y + ";" + npc.z
                                + ";" + npc.yaw + ";" + npc.pitch);
                    }
                    config.set(base + "npcs", list);
                    config.set(base + "blocks", null); // blocks stored as schematic
                    config.set(base + "schematic", st.fileName);
                    config.set(base + "priority", st.priority);
                    config.set(base + "origin.x", st.ox);
                    config.set(base + "origin.y", st.oy);
                    config.set(base + "origin.z", st.oz);
                }
            }
        }
        try { config.save(file); } catch (Exception e) { e.printStackTrace(); }
    }

    /** Represents a single NPC spawn within a stage, stored relative to pos1. */
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

    /** Simple storage class for a town stage area. */
    public static class TownStage {
        public final String name;
        public final int level;
        public final int stage;
        public final Location pos1;
        public final Location pos2;
        public final java.util.List<NPCSpawn> npcs;
        public final java.util.List<BlockDef> blocks;
        public final File schematic;
        public final String fileName;
        /** Priority used when placing blocks for this stage. Higher wins. */
        public final int priority;
        public final int ox, oy, oz;

        public TownStage(String name, int level, int stage, Location pos1, Location pos2,
                         java.util.List<NPCSpawn> npcs, java.util.List<BlockDef> blocks,
                         File schematic, String fileName, int priority,
                         int ox, int oy, int oz) {
            this.name = name;
            this.level = level;
            this.stage = stage;
            this.pos1 = pos1;
            this.pos2 = pos2;
            this.npcs = npcs == null ? java.util.Collections.emptyList() : npcs;
            this.blocks = blocks == null ? java.util.Collections.emptyList() : blocks;
            this.schematic = schematic;
            this.fileName = fileName;
            this.priority = priority;
            this.ox = ox;
            this.oy = oy;
            this.oz = oz;
        }
    }

    /** Represents a single block inside a stage structure. */
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
