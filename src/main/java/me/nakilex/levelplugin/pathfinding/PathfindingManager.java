package me.nakilex.levelplugin.pathfinding;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.event.NavigationCompleteEvent;
import net.citizensnpcs.api.ai.event.NavigationStuckEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.*;

/**
 * Manages editable location sequences and executes them with Citizens NPCs.
 * Designed to be generic so different systems can reuse stored paths.
 */
public class PathfindingManager {
    private final Plugin plugin;
    private final Map<Integer, Location> editingPoints = new HashMap<>();
    private final Map<String, List<Location>> paths = new HashMap<>();

    public PathfindingManager(Plugin plugin) {
        this.plugin = plugin;
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
        paths.put(name.toLowerCase(Locale.ROOT), list);
        editingPoints.clear();
    }

    /** Execute a previously created path. */
    public void executePath(String name) {
        List<Location> list = paths.get(name.toLowerCase(Locale.ROOT));
        if (list == null || list.isEmpty()) {
            return;
        }
        new PathRunner(plugin, list).start();
    }

    private static class PathRunner implements Listener {
        private final Plugin plugin;
        private final NPC npc;
        private final List<Location> points;
        private int index = 1;

        PathRunner(Plugin plugin, List<Location> points) {
            this.plugin = plugin;
            this.points = points;
            this.npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "PathNPC");
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }

        void start() {
            npc.spawn(points.get(0));
            if (points.size() > 1) {
                npc.getNavigator().setTarget(points.get(1));
            } else {
                cleanup();
            }
        }

        @EventHandler
        public void onComplete(NavigationCompleteEvent event) {
            if (!event.getNPC().equals(npc)) {
                return;
            }
            if (++index >= points.size()) {
                cleanup();
                return;
            }
            npc.getNavigator().setTarget(points.get(index));
        }

        @EventHandler
        public void onStuck(NavigationStuckEvent event) {
            if (!event.getNPC().equals(npc)) {
                return;
            }
            npc.getNavigator().setTarget(points.get(index));
        }

        private void cleanup() {
            npc.despawn();
            npc.destroy();
            HandlerList.unregisterAll(this);
        }
    }
}

