package me.nakilex.levelplugin.pathfinding;

import me.nakilex.levelplugin.pathfinding.npc.PathNpc;
import me.nakilex.levelplugin.pathfinding.npc.PathNpcFactory;
import me.nakilex.levelplugin.pathfinding.npc.RogueMercenary;
import org.bukkit.Bukkit;
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
    private final FileConfiguration config;

    public PathfindingManager(Plugin plugin) {
        this.plugin = plugin;
        plugin.saveResource("paths.yml", false);
        this.file = new File(plugin.getDataFolder(), "paths.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        loadPaths();
    }

    private void loadPaths() {
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
        PathSession session = createSession(name, profile);
        if (session != null) {
            session.setCompletion(() -> {});
            session.start();
        }
    }

    public Set<String> getPathNames() {
        return paths.keySet();
    }

    /**
     * Exposes a cloned list of the stored locations for the requested path.
     * The returned list can safely be modified by callers.
     */
    public List<Location> getPathPoints(String name) {
        List<Location> list = paths.get(name.toLowerCase(Locale.ROOT));
        if (list == null) {
            return Collections.emptyList();
        }
        List<Location> copy = new ArrayList<>(list.size());
        for (Location loc : list) {
            copy.add(loc == null ? null : loc.clone());
        }
        return copy;
    }

    public int nextPointIndex() {
        return editingPoints.size() + 1;
    }

    /**
     * Creates a {@link PathSession} for the given path without automatically
     * starting it. Returns {@code null} if the path does not exist.
     */
    public PathSession createSession(String name, PathNpc profile) {
        List<Location> list = paths.get(name.toLowerCase(Locale.ROOT));
        if (list == null || list.isEmpty()) {
            return null;
        }
        return new PathSession(plugin, list, profile);
    }

    /**
     * Helper used by YAML driven systems where only an NPC profile identifier
     * is stored. Falls back to a rogue mercenary if the identifier is missing
     * or unknown so legacy data remains functional.
     */
    public PathSession createSession(String name, String profileId) {
        PathNpc profile = PathNpcFactory.fromId(profileId).orElseGet(RogueMercenary::new);
        return createSession(name, profile);
    }
}

