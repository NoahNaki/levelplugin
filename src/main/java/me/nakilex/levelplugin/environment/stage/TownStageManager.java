package me.nakilex.levelplugin.environment.stage;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.CurrentLocation;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
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
    private File file;
    private FileConfiguration config;

    public TownStageManager(Main plugin) {
        this.plugin = plugin;
        loadFromConfig();
    }

    /** Returns all defined town names. */
    public Set<String> getStageNames() {
        return new java.util.HashSet<>(stages.keySet());
    }

    public TownStage getStage(String town, int level, int stage) {
        if (town == null) return null;
        var levels = stages.get(town.toLowerCase());
        if (levels == null) return null;
        var stagesMap = levels.get(level);
        if (stagesMap == null) return null;
        return stagesMap.get(stage);
    }

    public void createStage(String name, int level, int stage, Location p1, Location p2) {
        java.util.List<NPCSpawn> npcs = new java.util.ArrayList<>();
        java.util.List<BlockDef> blocks = new java.util.ArrayList<>();
        var boxMinX = Math.min(p1.getBlockX(), p2.getBlockX());
        var boxMaxX = Math.max(p1.getBlockX(), p2.getBlockX());
        var boxMinY = Math.min(p1.getBlockY(), p2.getBlockY());
        var boxMaxY = Math.max(p1.getBlockY(), p2.getBlockY());
        var boxMinZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        var boxMaxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
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
        stages
            .computeIfAbsent(name.toLowerCase(), k -> new java.util.HashMap<>())
            .computeIfAbsent(level, k -> new java.util.HashMap<>())
            .put(stage, new TownStage(name.toLowerCase(), level, stage, p1, p2, npcs, blocks));
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
            NPC template = CitizensAPI.getNPCRegistry().getById(ns.id);
            if (template == null) {
                plugin.getLogger().warning("NPC template with ID " + ns.id + " not found while spawning stage NPCs");
                continue;
            }
            // Use Citizens API clone support to copy all traits/metadata
            NPC clone = template.copy();

            // Translate original NPC position by the player's town origin offset
            Location base = ts.pos1;
            Location loc = base.clone().add(ns.x, ns.y, ns.z);
            loc.add(origin.getX() - base.getBlockX(), origin.getY() - base.getBlockY(), origin.getZ() - base.getBlockZ());
            loc.add(0, 1, 0); // spawn one block higher so they don't clip
            loc.setYaw(ns.yaw);
            loc.setPitch(ns.pitch);

            clone.getOrAddTrait(CurrentLocation.class).setLocation(loc);
            plugin.getLogger().info("Spawning NPC clone from template " + ns.id + " at "
                    + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()
                    + " for " + viewer.getName());
            clone.spawn(loc);
            for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(viewer)) {
                    p.hideEntity(plugin, clone.getEntity());
                }
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
                    if (npc.isSpawned()) {
                        viewer.hideEntity(plugin, npc.getEntity());
                    }
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
                    java.util.List<BlockDef> blocks = new java.util.ArrayList<>();
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
                    if (config.isList(base + "blocks")) {
                        for (String line : config.getStringList(base + "blocks")) {
                            String[] parts = line.split(";");
                            if (parts.length < 4) continue;
                            try {
                                int dx = Integer.parseInt(parts[0]);
                                int dy = Integer.parseInt(parts[1]);
                                int dz = Integer.parseInt(parts[2]);
                                BlockData data = Bukkit.createBlockData(parts[3]);
                                blocks.add(new BlockDef(dx, dy, dz, data));
                            } catch (Exception ignored) {}
                        }
                    }
                    stages
                        .computeIfAbsent(town.toLowerCase(), k -> new java.util.HashMap<>())
                        .computeIfAbsent(level, k -> new java.util.HashMap<>())
                        .put(stage, new TownStage(town.toLowerCase(), level, stage, p1, p2, npcs, blocks));
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
                    java.util.List<String> blockLines = new java.util.ArrayList<>();
                    for (BlockDef b : st.blocks) {
                        blockLines.add(b.x + ";" + b.y + ";" + b.z + ";" + b.data.getAsString());
                    }
                    config.set(base + "blocks", blockLines);
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

        public TownStage(String name, int level, int stage, Location pos1, Location pos2,
                         java.util.List<NPCSpawn> npcs, java.util.List<BlockDef> blocks) {
            this.name = name;
            this.level = level;
            this.stage = stage;
            this.pos1 = pos1;
            this.pos2 = pos2;
            this.npcs = npcs == null ? java.util.Collections.emptyList() : npcs;
            this.blocks = blocks == null ? java.util.Collections.emptyList() : blocks;
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
