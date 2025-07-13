package me.nakilex.levelplugin.environment.stage;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.regions.CuboidRegion;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.CurrentLocation;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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

    public void createStage(String name, int level, int stage, Location p1, Location p2, Location origin) {
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
        stages
            .computeIfAbsent(name.toLowerCase(), k -> new java.util.HashMap<>())
            .computeIfAbsent(level, k -> new java.util.HashMap<>())
            .put(stage, new TownStage(name.toLowerCase(), level, stage, p1, p2, npcs, blocks, null, ox, oy, oz));
        saveConfig();
    }

    /**
     * Capture the selected area and export it to a schematic file instead of storing all block data.
     */
    public void createStageSchem(String name, int level, int stage, Location p1, Location p2, Location origin) {
        java.util.List<NPCSpawn> npcs = new java.util.ArrayList<>();
        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            Location l = npc.isSpawned() ? npc.getEntity().getLocation() : npc.getStoredLocation();
            if (l == null || !l.getWorld().equals(p1.getWorld())) continue;
            int x = l.getBlockX();
            int y = l.getBlockY();
            int z = l.getBlockZ();
            if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ) {
                npcs.add(new NPCSpawn(npc.getId(), x - minX, y - minY, z - minZ, l.getYaw(), l.getPitch()));
            }
        }

        int ox = origin.getBlockX() - minX;
        int oy = origin.getBlockY() - minY;
        int oz = origin.getBlockZ() - minZ;

        File schemDir = new File(plugin.getDataFolder(), "schematics");
        schemDir.mkdirs();
        File out = new File(schemDir, name.toLowerCase() + "_l" + level + "_s" + stage + ".schem");
        saveSchematic(p1, p2, out);

        stages
            .computeIfAbsent(name.toLowerCase(), k -> new java.util.HashMap<>())
            .computeIfAbsent(level, k -> new java.util.HashMap<>())
            .put(stage, new TownStage(name.toLowerCase(), level, stage, p1, p2, npcs, java.util.Collections.emptyList(), out, ox, oy, oz));
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
            NPC template = CitizensAPI.getNPCRegistry().getById(ns.id);
            if (template == null) {
                plugin.getLogger().warning("NPC template with ID " + ns.id + " not found while spawning stage NPCs");
                continue;
            }
            // Use Citizens API clone support to copy all traits/metadata
            NPC clone = template.copy();

            // Translate original NPC position relative to the player's town
            // origin. Add a Y offset so the NPC doesn't spawn partially in the ground.
            Location loc = origin.clone().add(
                    ns.x - ts.ox + 0.5,
                    ns.y - ts.oy + NPC_SPAWN_Y_OFFSET,
                    ns.z - ts.oz + 0.5
            );
            loc.setYaw(ns.yaw);
            loc.setPitch(ns.pitch);

            clone.getOrAddTrait(CurrentLocation.class).setLocation(loc);
            plugin.getLogger().info("Spawning NPC clone from template " + ns.id + " at "
                    + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()
                    + " for " + viewer.getName());
            clone.spawn(loc);
            if (clone.isSpawned()) {
                clone.getEntity().teleport(loc, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                clone.getEntity().setGravity(false);
            }
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

    /** Save the selected region to a schematic file using FAWE. */
    private void saveSchematic(Location p1, Location p2, File file) {
        try {
            int minX = Math.min(p1.getBlockX(), p2.getBlockX());
            int minY = Math.min(p1.getBlockY(), p2.getBlockY());
            int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
            int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
            int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
            int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(p1.getWorld());
            BlockVector3 min = BlockVector3.at(minX, minY, minZ);
            BlockVector3 max = BlockVector3.at(maxX, maxY, maxZ);
            CuboidRegion region = new CuboidRegion(weWorld, min, max);
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
                Operations.complete(copy);
            }

            ClipboardFormat format = ClipboardFormats.findByFile(file);
            if (format == null) {
                format = ClipboardFormats.findByAlias("schem");
            }
            file.getParentFile().mkdirs();
            try (ClipboardWriter writer = format.getWriter(new java.io.FileOutputStream(file))) {
                writer.write(clipboard);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Paste a schematic file at the given origin using FAWE. */
    public void pasteSchematic(java.io.File file, Location origin) {
        try {
            ClipboardFormat format = ClipboardFormats.findByFile(file);
            if (format == null) return;
            try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
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

    /**
     * Load block definitions from a schematic file so town stages can be
     * displayed using fake blocks instead of modifying the world.
     */
    public java.util.List<BlockDef> loadSchematicBlocks(java.io.File file) {
        java.util.List<BlockDef> list = new java.util.ArrayList<>();
        try {
            ClipboardFormat format = ClipboardFormats.findByFile(file);
            if (format == null) return list;
            try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                Clipboard clipboard = reader.read();
                BlockVector3 min = clipboard.getMinimumPoint();
                BlockVector3 max = clipboard.getMaximumPoint();
                for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
                    for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                        for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                            var state = clipboard.getFullBlock(BlockVector3.at(x, y, z));
                            if (state.getBlockType().getMaterial().isAir()) continue;
                            BlockData data = BukkitAdapter.adapt(state);
                            list.add(new BlockDef(x - min.getBlockX(), y - min.getBlockY(), z - min.getBlockZ(), data));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Retrieve block definitions for a stage. If the stage uses a schematic,
     * its blocks are loaded on demand from the file.
     */
    public java.util.List<BlockDef> getBlocksForStage(TownStage st) {
        if (st == null) return java.util.Collections.emptyList();
        if (!st.blocks.isEmpty()) return st.blocks;
        if (st.schematic != null && st.schematic.exists()) {
            return loadSchematicBlocks(st.schematic);
        }
        return java.util.Collections.emptyList();
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
                    java.io.File schemFile = null;
                    String schemPath = config.getString(base + "schem");
                    if (schemPath != null) {
                        schemFile = new File(plugin.getDataFolder(), schemPath);
                    }
                    int ox = config.getInt(base + "origin.x", 0);
                    int oy = config.getInt(base + "origin.y", 0);
                    int oz = config.getInt(base + "origin.z", 0);
                    stages
                        .computeIfAbsent(town.toLowerCase(), k -> new java.util.HashMap<>())
                        .computeIfAbsent(level, k -> new java.util.HashMap<>())
                        .put(stage, new TownStage(town.toLowerCase(), level, stage, p1, p2, npcs, blocks, schemFile, ox, oy, oz));
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
                    if (st.schematic != null) {
                        try {
                            java.nio.file.Path root = plugin.getDataFolder().toPath();
                            java.nio.file.Path rel = root.relativize(st.schematic.toPath());
                            config.set(base + "schem", rel.toString().replace('\\','/'));
                        } catch (Exception ex) {
                            config.set(base + "schem", st.schematic.getName());
                        }
                    }
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
        /** Optional schematic file instead of explicit blocks. */
        public final java.io.File schematic;
        public final int ox, oy, oz;

        public TownStage(String name, int level, int stage, Location pos1, Location pos2,
                         java.util.List<NPCSpawn> npcs, java.util.List<BlockDef> blocks,
                         java.io.File schematic,
                         int ox, int oy, int oz) {
            this.name = name;
            this.level = level;
            this.stage = stage;
            this.pos1 = pos1;
            this.pos2 = pos2;
            this.npcs = npcs == null ? java.util.Collections.emptyList() : npcs;
            this.blocks = blocks == null ? java.util.Collections.emptyList() : blocks;
            this.schematic = schematic;
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
