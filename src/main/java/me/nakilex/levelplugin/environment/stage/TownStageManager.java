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

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.function.operation.Operations;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handles storage of environment stage areas for settlements.
 */
public class TownStageManager {
    private final Main plugin;
    /** Map of town -> stage -> data */
    private final Map<String, Map<Integer, TownStage>> stages = new HashMap<>();
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

    public TownStage getStage(String town, int stage) {
        if (town == null) return null;
        var map = stages.get(town.toLowerCase());
        if (map == null) return null;
        return map.get(stage);
    }

    public void createStage(String name, int stage, Location p1, Location p2, Location origin, int priority) {
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

        String fileName = name.toLowerCase() + "_" + stage + ".schem";
        File schematic = new File(schemFolder, fileName);
        saveSchematic(p1, p2, schematic);
        stages
            .computeIfAbsent(name.toLowerCase(), k -> new java.util.HashMap<>())
            .put(stage, new TownStage(name.toLowerCase(), stage, p1, p2, npcs, blocks, schematic, fileName, priority, ox, oy, oz));
        saveConfig();
    }

    public boolean removeStage(String name, int stage) {
        var map = stages.get(name.toLowerCase());
        if (map == null) return false;
        if (map.remove(stage) != null) {
            if (map.isEmpty()) stages.remove(name.toLowerCase());
            saveConfig();
            return true;
        }
        return false;
    }

    // NPCs should stand directly on the ground
    private static final double NPC_SPAWN_Y_OFFSET = 0.0;

    public void spawnForStage(org.bukkit.entity.Player viewer, String town, int stage, Location origin) {
        TownStage ts = getStage(town, stage);
        if (ts == null || origin == null || viewer == null) return;
        java.util.UUID id = viewer.getUniqueId();
        var map = spawnedNPCs.computeIfAbsent(id, k -> new java.util.HashMap<>());
        String key = town.toLowerCase() + ":" + stage;
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

    public void despawnForStage(java.util.UUID viewerId, String town, int stage) {
        var map = spawnedNPCs.get(viewerId);
        if (map == null) return;
        String key = town.toLowerCase() + ":" + stage;
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
            for (String stKey : townSec.getKeys(false)) {
                int stage;
                try {
                    stage = Integer.parseInt(stKey);
                } catch (NumberFormatException ex) {
                    continue; // skip invalid stage keys
                }
                String base = "stages." + town + "." + stKey + ".";
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
                    String fileName = config.getString(base + "schematic", town.toLowerCase() + "_" + stage + ".schem");
                    File schematic = new File(schemFolder, fileName);
                    blocks = loadSchematic(schematic, world);
                    int priority = config.getInt(base + "priority", 0);
                    int ox = config.getInt(base + "origin.x", 0);
                    int oy = config.getInt(base + "origin.y", 0);
                    int oz = config.getInt(base + "origin.z", 0);
                    stages
                        .computeIfAbsent(town.toLowerCase(), k -> new java.util.HashMap<>())
                        .put(stage, new TownStage(town.toLowerCase(), stage, p1, p2, npcs, blocks, schematic, fileName, priority, ox, oy, oz));
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

    private void saveSchematic(Location p1, Location p2, File file) {
        try {
            int minX = Math.min(p1.getBlockX(), p2.getBlockX());
            int minY = Math.min(p1.getBlockY(), p2.getBlockY());
            int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
            int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
            int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
            int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

            CuboidRegion region = new CuboidRegion(
                    BukkitAdapter.adapt(p1.getWorld()),
                    BlockVector3.at(minX, minY, minZ),
                    BlockVector3.at(maxX, maxY, maxZ)
            );
            Clipboard clipboard = new BlockArrayClipboard(region);
            try (EditSession session = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(p1.getWorld()))) {
                ForwardExtentCopy copy = new ForwardExtentCopy(session, region, clipboard, region.getMinimumPoint());
                Operations.complete(copy);
            }
            var format = ClipboardFormats.findByFile(file);
            if (format == null) {
                format = ClipboardFormats.findByExtension("schem");
            }
            if (format == null) {
                plugin.getLogger().warning("Unknown schematic format for " + file.getName());
                return;
            }
            try (ClipboardWriter writer = format.getWriter(new java.io.FileOutputStream(file))) {
                writer.write(clipboard);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private java.util.List<BlockDef> loadSchematic(File file, World world) {
        java.util.List<BlockDef> blocks = new java.util.ArrayList<>();
        try {
            if (!file.exists()) {
                plugin.getLogger().warning("Schematic not found: " + file.getName());
                return blocks;
            }
            var format = ClipboardFormats.findByFile(file);
            if (format == null) {
                format = ClipboardFormats.findByExtension("schem");
            }
            if (format == null) return blocks;
            try (var reader = format.getReader(new java.io.FileInputStream(file))) {
                Clipboard clipboard = reader.read();
                BlockVector3 min = clipboard.getRegion().getMinimumPoint();
                for (BlockVector3 vec : clipboard.getRegion()) {
                    var state = clipboard.getBlock(vec);
                    BlockData data = BukkitAdapter.adapt(state.toImmutableState());
                    if (data.getMaterial() == Material.AIR) continue;
                    blocks.add(new BlockDef(
                            vec.getBlockX() - min.getBlockX(),
                            vec.getBlockY() - min.getBlockY(),
                            vec.getBlockZ() - min.getBlockZ(),
                            data));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return blocks;
    }

    private void saveConfig() {
        config.set("stages", null);
        for (var entryTown : stages.entrySet()) {
            String town = entryTown.getKey();
            for (var entryStage : entryTown.getValue().entrySet()) {
                int stage = entryStage.getKey();
                TownStage st = entryStage.getValue();
                String base = "stages." + town + "." + stage + ".";
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

        public TownStage(String name, int stage, Location pos1, Location pos2,
                         java.util.List<NPCSpawn> npcs, java.util.List<BlockDef> blocks,
                         File schematic, String fileName, int priority,
                         int ox, int oy, int oz) {
            this.name = name;
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
