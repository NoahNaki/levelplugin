package me.nakilex.levelplugin.environment.stage;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
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
    private final Map<String, java.util.List<NPC>> spawnedNPCs = new HashMap<>();
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
                npcs.add(new NPCSpawn(npc.getId(), l));
            }
        }
        stages
            .computeIfAbsent(name.toLowerCase(), k -> new java.util.HashMap<>())
            .computeIfAbsent(level, k -> new java.util.HashMap<>())
            .put(stage, new TownStage(name.toLowerCase(), level, stage, p1, p2, npcs));
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

    public void spawnForStage(String town, int level, int stage) {
        TownStage ts = getStage(town, level, stage);
        if (ts == null) return;
        var list = spawnedNPCs.computeIfAbsent(town.toLowerCase() + ":" + level + ":" + stage,
                k -> new java.util.ArrayList<>());
        if (!list.isEmpty()) return;
        for (NPCSpawn ns : ts.npcs) {
            NPC npc = CitizensAPI.getNPCRegistry().getById(ns.id);
            if (npc != null && !npc.isSpawned()) {
                npc.spawn(ns.location);
                list.add(npc);
            }
        }
    }

    public void despawnAll() {
        for (var list : spawnedNPCs.values()) {
            for (NPC npc : list) {
                if (npc.isSpawned()) npc.despawn();
            }
        }
        spawnedNPCs.clear();
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
                    if (config.isList(base + "npcs")) {
                        for (var o : config.getList(base + "npcs")) {
                            if (!(o instanceof String s)) continue;
                            String[] parts = s.split(";");
                            if (parts.length != 4) continue;
                            try {
                                int id = Integer.parseInt(parts[0]);
                                int x = Integer.parseInt(parts[1]);
                                int y = Integer.parseInt(parts[2]);
                                int z = Integer.parseInt(parts[3]);
                                npcs.add(new NPCSpawn(id, new Location(world, x, y, z)));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                    stages
                        .computeIfAbsent(town.toLowerCase(), k -> new java.util.HashMap<>())
                        .computeIfAbsent(level, k -> new java.util.HashMap<>())
                        .put(stage, new TownStage(town.toLowerCase(), level, stage, p1, p2, npcs));
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
                        list.add(npc.id + ";" + npc.location.getBlockX() + ";" + npc.location.getBlockY() + ";" + npc.location.getBlockZ());
                    }
                    config.set(base + "npcs", list);
                }
            }
        }
        try { config.save(file); } catch (Exception e) { e.printStackTrace(); }
    }

    /** Represents a single NPC spawn within a stage. */
    public static class NPCSpawn {
        public final int id;
        public final Location location;
        public NPCSpawn(int id, Location loc) {
            this.id = id;
            this.location = loc;
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

        public TownStage(String name, int level, int stage, Location pos1, Location pos2,
                         java.util.List<NPCSpawn> npcs) {
            this.name = name;
            this.level = level;
            this.stage = stage;
            this.pos1 = pos1;
            this.pos2 = pos2;
            this.npcs = npcs == null ? java.util.Collections.emptyList() : npcs;
        }
    }
}
