package me.nakilex.levelplugin.pathfinding;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.event.NavigationStuckEvent;
import net.citizensnpcs.api.npc.NPC;
import me.nakilex.levelplugin.spells.managers.CooldownManager;
import me.nakilex.levelplugin.pathfinding.npc.AssassinMercenary;
import me.nakilex.levelplugin.pathfinding.npc.PathNpc;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

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

    /** Execute a previously created path with default assassin profile. */
    public void executePath(String name) {
        executePath(name, new AssassinMercenary());
    }

    /** Execute a previously created path with a custom NPC profile. */
    public void executePath(String name, PathNpc profile) {
        List<Location> list = paths.get(name.toLowerCase(Locale.ROOT));
        if (list == null || list.isEmpty()) {
            return;
        }
        new PathRunner(plugin, list, profile).start();
    }

    public Set<String> getPathNames() {
        return paths.keySet();
    }

    public int nextPointIndex() {
        return editingPoints.size() + 1;
    }

    private static class PathRunner implements Listener {
        private final Plugin plugin;
        private final NPC npc;
        private final List<Location> points;
        private final PathNpc profile;
        private BukkitTask task;
        private int index = 1;
        private LivingEntity combatTarget;
        private final CooldownManager cd = CooldownManager.getInstance();

        PathRunner(Plugin plugin, List<Location> points, PathNpc profile) {
            this.plugin = plugin;
            this.points = points;
            this.profile = profile;
            this.npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, profile.name());
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }

        void start() {
            npc.spawn(points.get(0));

            // Apply speed multiplier and equipment from the profile
            var params = npc.getNavigator().getDefaultParameters();
            params.baseSpeed(params.baseSpeed() * profile.speedMultiplier());
            profile.equip(npc);

            if (points.size() <= 1) {
                cleanup();
                return;
            }
            npc.getNavigator().setTarget(points.get(1));
            task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (combatTarget != null) {
                    if (combatTarget.isDead() || !combatTarget.isValid()) {
                        combatTarget = null;
                        npc.getNavigator().setTarget(points.get(index));
                        return;
                    }
                    profile.handleCombat(npc, combatTarget, cd);
                    return;
                }

                LivingEntity hostile = findNearestHostile();
                if (hostile != null) {
                    combatTarget = hostile;
                    profile.handleCombat(npc, combatTarget, cd);
                    return;
                }

                if (!npc.getNavigator().isNavigating()) {
                    if (++index >= points.size()) {
                        cleanup();
                        return;
                    }
                    npc.getNavigator().setTarget(points.get(index));
                }
            }, 10L, 10L);
        }

        @EventHandler
        public void onStuck(NavigationStuckEvent event) {
            if (event.getNPC().equals(npc)) {
                if (combatTarget != null && combatTarget.isValid()) {
                    npc.getNavigator().setTarget(combatTarget, true);
                } else {
                    npc.getNavigator().setTarget(points.get(index));
                }
            }
        }

        private void cleanup() {
            if (task != null) {
                task.cancel();
            }
            npc.despawn();
            npc.destroy();
            HandlerList.unregisterAll(this);
        }

        private LivingEntity findNearestHostile() {
            if (!(npc.getEntity() instanceof LivingEntity le)) {
                return null;
            }
            Location loc = le.getLocation();
            double radius = 10;
            return loc.getWorld().getNearbyEntities(loc, radius, radius, radius).stream()
                    .filter(e -> e instanceof Monster)
                    .map(e -> (LivingEntity) e)
                    .min(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(loc)))
                    .orElse(null);
        }
    }
}

