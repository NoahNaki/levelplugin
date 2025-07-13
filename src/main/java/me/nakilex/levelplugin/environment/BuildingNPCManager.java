package me.nakilex.levelplugin.environment;

import me.nakilex.levelplugin.Main;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.CurrentLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

/**
 * Manages per-building NPC spawns that are shown only to players
 * who have unlocked a specific building stage.
 */
public class BuildingNPCManager {
    private final Main plugin;
    private final Map<String, List<NPCPlacement>> npcMap = new HashMap<>();
    private final Map<UUID, Map<String, List<NPC>>> spawned = new HashMap<>();
    private File file;
    private FileConfiguration config;

    public BuildingNPCManager(Main plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void reload() {
        loadConfig();
    }

    private void loadConfig() {
        file = new File(plugin.getDataFolder(), "buildingnpcs.yml");
        if (!file.exists()) {
            plugin.saveResource("buildingnpcs.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        npcMap.clear();
        if (config.isConfigurationSection("npcs")) {
            for (String building : config.getConfigurationSection("npcs").getKeys(false)) {
                List<NPCPlacement> list = new ArrayList<>();
                var sec = config.getConfigurationSection("npcs." + building);
                if (sec != null) {
                    for (String key : sec.getKeys(false)) {
                        String base = "npcs." + building + "." + key + ".";
                        int id = config.getInt(base + "id", -1);
                        if (id < 0) continue;
                        int level = config.getInt(base + "level", 1);
                        int stage = config.getInt(base + "stage", 1);
                        String world = config.getString(base + "world", null);
                        double x = config.getDouble(base + "x");
                        double y = config.getDouble(base + "y");
                        double z = config.getDouble(base + "z");
                        float yaw = (float) config.getDouble(base + "yaw");
                        float pitch = (float) config.getDouble(base + "pitch");
                        list.add(new NPCPlacement(id, world, x, y, z, yaw, pitch, level, stage));
                    }
                }
                npcMap.put(building.toLowerCase(), list);
            }
        }
    }

    /** Spawn NPCs for the given building stage for only the specified viewer. */
    public void spawnForStage(Player viewer, String building, int level, int stage, Location origin) {
        if (viewer == null || origin == null) return;
        List<NPCPlacement> placements = npcMap.get(building.toLowerCase());
        if (placements == null || placements.isEmpty()) return;
        UUID id = viewer.getUniqueId();
        var map = spawned.computeIfAbsent(id, k -> new HashMap<>());
        String key = building.toLowerCase() + ":" + level + ":" + stage;
        List<NPC> list = map.computeIfAbsent(key, k -> new ArrayList<>());
        for (NPC npc : list) {
            if (npc.isSpawned()) npc.despawn();
            npc.destroy();
        }
        list.clear();
        for (NPCPlacement np : placements) {
            // Show the NPC whenever the player's building is at or beyond the
            // configured requirement. This lets earlier stage NPCs remain
            // visible after upgrading the building.
            if (level >= np.level && stage >= np.stage) {
                NPC template = CitizensAPI.getNPCRegistry().getById(np.id);
                if (template == null) continue;
                NPC clone = template.copy();
                Location loc;
                if (np.world != null) {
                    World w = Bukkit.getWorld(np.world);
                    if (w == null) continue;
                    loc = new Location(w, np.x, np.y, np.z, np.yaw, np.pitch);
                } else {
                    loc = origin.clone().add(np.x, np.y, np.z);
                    loc.setYaw(np.yaw);
                    loc.setPitch(np.pitch);
                }
                clone.getOrAddTrait(CurrentLocation.class).setLocation(loc);
                clone.spawn(loc);
                if (clone.isSpawned()) {
                    clone.getEntity().teleport(loc, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                    clone.getEntity().setGravity(false);
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.equals(viewer)) p.hideEntity(plugin, clone.getEntity());
                }
                list.add(clone);
            }
        }
    }

    /** Despawn NPCs for a viewer for the specified stage. */
    public void despawnForStage(UUID viewerId, String building, int level, int stage) {
        var map = spawned.get(viewerId);
        if (map == null) return;
        String key = building.toLowerCase() + ":" + level + ":" + stage;
        List<NPC> list = map.remove(key);
        if (list != null) {
            for (NPC npc : list) {
                if (npc.isSpawned()) npc.despawn();
                npc.destroy();
            }
        }
        if (map.isEmpty()) spawned.remove(viewerId);
    }

    /** Hide all spawned NPCs from the given player. */
    public void hideNPCsFrom(Player viewer) {
        if (viewer == null) return;
        for (var map : spawned.values()) {
            for (var list : map.values()) {
                for (NPC npc : list) {
                    if (npc.isSpawned()) viewer.hideEntity(plugin, npc.getEntity());
                }
            }
        }
    }

    /** Despawn all NPCs for all players. */
    public void despawnAll() {
        for (var map : spawned.values()) {
            for (var list : map.values()) {
                for (NPC npc : list) {
                    if (npc.isSpawned()) npc.despawn();
                    npc.destroy();
                }
            }
        }
        spawned.clear();
    }

    /** Definition of an NPC relative to a building origin. */
    public static class NPCPlacement {
        public final int id;
        public final String world;
        public final double x, y, z;
        public final float yaw, pitch;
        public final int level, stage;
        public NPCPlacement(int id, String world, double x, double y, double z, float yaw, float pitch, int level, int stage) {
            this.id = id;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.level = level;
            this.stage = stage;
        }
    }
}
