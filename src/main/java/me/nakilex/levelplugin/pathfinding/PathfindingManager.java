package me.nakilex.levelplugin.pathfinding;

import me.nakilex.levelplugin.pathfinding.npc.RogueMercenary;
import me.nakilex.levelplugin.pathfinding.npc.PathNpc;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages editable location sequences and executes them with Citizens NPCs.
 * Designed to be generic so different systems can reuse stored paths.
 */
public class PathfindingManager {
    private final Plugin plugin;
    private final Map<Integer, Location> editingPoints = new HashMap<>();
    private final Map<String, List<Location>> paths = new HashMap<>();
    private final File file;
    private FileConfiguration config;

    public PathfindingManager(Plugin plugin) {
        this.plugin = plugin;
        plugin.saveResource("paths.yml", false);
        this.file = new File(plugin.getDataFolder(), "paths.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        loadPaths();
    }

    private void loadPaths() {
        paths.clear();
        if (!config.isConfigurationSection("paths")) {
            return;
        }
        for (String name : config.getConfigurationSection("paths").getKeys(false)) {
            List<Location> list = (List<Location>) config.getList("paths." + name);
            if (list != null) {
                paths.put(name.toLowerCase(Locale.ROOT), list);
            }
        }
    }

    public synchronized void reload() {
        this.config = YamlConfiguration.loadConfiguration(file);
        loadPaths();
    }

    private void savePath(String name, List<Location> list) {
        config.set("paths." + name, list);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Store or replace a temporary point for the next path creation. */
    public void setPoint(int index, Location loc) {
        editingPoints.put(index, loc);
    }

    /**
     * Creates a named path from the currently stored points.
     * Points are ordered by their numeric index.
     */
    public void createPath(String name) {
        List<Location> list = editingPoints.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();
        String key = name.toLowerCase(Locale.ROOT);
        paths.put(key, list);
        savePath(key, list);
        editingPoints.clear();
    }

    /** Execute a previously created path with default rogue profile. */
    public void executePath(String name) {
        executePath(name, new RogueMercenary());
    }

    /** Execute a previously created path with a custom NPC profile. */
    public void executePath(String name, PathNpc profile) {
        List<Location> list = paths.get(name.toLowerCase(Locale.ROOT));
        if (list == null || list.isEmpty()) {
            return;
        }
        PathFollower follower = PathFollower.spawnNpc(plugin, list, profile, true, null);
        follower.start();
    }

    public Set<String> getPathNames() {
        return paths.keySet();
    }

    public int nextPointIndex() {
        return editingPoints.size() + 1;
    }
}
