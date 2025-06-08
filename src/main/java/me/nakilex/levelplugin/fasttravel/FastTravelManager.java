package me.nakilex.levelplugin.fasttravel;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.fasttravel.data.FastTravelPoint;
import me.nakilex.levelplugin.fasttravel.data.Waystone;
import me.nakilex.levelplugin.fasttravel.data.WaystoneType;
import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import org.bukkit.block.BlockFace;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FastTravelManager {
    private final Main plugin;
    private final Map<String, FastTravelPoint> points = new HashMap<>();
    private final Map<String, Waystone> waystones = new HashMap<>();
    private final Map<UUID, Set<String>> unlocked = new HashMap<>();
    private final Map<UUID, String> lastUsed = new HashMap<>();
    private File file;
    private FileConfiguration config;
    private File wsFile;
    private FileConfiguration wsConfig;

    public FastTravelManager(Main plugin) {
        this.plugin = plugin;
        load();
        // Spawn all waystones shortly after startup so the Nexo plugin has time
        // to register its furniture items.
        plugin.getServer().getScheduler().runTaskLater(plugin, this::spawnAllWaystones, 20L);
    }

    private void load() {
        file = new File(plugin.getDataFolder(), "fasttravel.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);

        wsFile = new File(plugin.getDataFolder(), "waystones.yml");
        if (!wsFile.exists()) {
            try { wsFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        wsConfig = YamlConfiguration.loadConfiguration(wsFile);

        if (config.isConfigurationSection("locations")) {
            for (String key : config.getConfigurationSection("locations").getKeys(false)) {
                String path = "locations." + key;
                String world = config.getString(path + ".world");
                double x = config.getDouble(path + ".x");
                double y = config.getDouble(path + ".y");
                double z = config.getDouble(path + ".z");
                String colorName = config.getString(path + ".color", "WHITE");
                String desc = config.getString(path + ".desc", "");
                double radius = config.getDouble(path + ".radius", 10);
                boolean town = config.getBoolean(path + ".town", false);
                Location loc = new Location(plugin.getServer().getWorld(world), x, y, z);
                FastTravelPoint pt = new FastTravelPoint(key, ChatColor.valueOf(colorName), desc, loc, radius, town);
                points.put(key.toLowerCase(), pt);
            }
        }

        if (wsConfig.isConfigurationSection("waystones")) {
            for (String key : wsConfig.getConfigurationSection("waystones").getKeys(false)) {
                String path = "waystones." + key;
                String world = wsConfig.getString(path + ".world");
                double x = wsConfig.getDouble(path + ".x");
                double y = wsConfig.getDouble(path + ".y");
                double z = wsConfig.getDouble(path + ".z");
                String typeName = wsConfig.getString(path + ".type", "TOWN");
                Location loc = new Location(plugin.getServer().getWorld(world), x, y, z);
                WaystoneType type = WaystoneType.valueOf(typeName.toUpperCase());
                waystones.put(key.toLowerCase(), new Waystone(key, loc, type));
                plugin.getLogger().info("[FastTravelManager] Loaded waystone '" + key + "' @ "
                        + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + " type=" + type);
            }
        }

        if (config.isConfigurationSection("players")) {
            for (String id : config.getConfigurationSection("players").getKeys(false)) {
                List<String> list = config.getStringList("players." + id);
                unlocked.put(UUID.fromString(id), new HashSet<>(list));
            }
        }
    }

    private void save() {
        for (Map.Entry<String, FastTravelPoint> e : points.entrySet()) {
            FastTravelPoint pt = e.getValue();
            String path = "locations." + e.getKey();
            Location loc = pt.getLocation();
            config.set(path + ".world", loc.getWorld().getName());
            config.set(path + ".x", loc.getX());
            config.set(path + ".y", loc.getY());
            config.set(path + ".z", loc.getZ());
            config.set(path + ".color", pt.getColor().name());
            config.set(path + ".desc", pt.getDescription());
            config.set(path + ".radius", pt.getRadius());
            config.set(path + ".town", pt.isTown());
        }
        for (Map.Entry<UUID, Set<String>> e : unlocked.entrySet()) {
            config.set("players." + e.getKey(), new ArrayList<>(e.getValue()));
        }
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }

        for (Map.Entry<String, Waystone> e : waystones.entrySet()) {
            Waystone ws = e.getValue();
            String path = "waystones." + e.getKey();
            Location loc = ws.getLocation();
            wsConfig.set(path + ".world", loc.getWorld().getName());
            wsConfig.set(path + ".x", loc.getX());
            wsConfig.set(path + ".y", loc.getY());
            wsConfig.set(path + ".z", loc.getZ());
            wsConfig.set(path + ".type", ws.getType().name());
        }
        try { wsConfig.save(wsFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public void addLocation(String name, ChatColor color, String desc, Location loc, double radius, boolean town) {
        points.put(name.toLowerCase(), new FastTravelPoint(name, color, desc, loc, radius, town));
        save();
    }

    public void moveLocation(String name, Location loc) {
        FastTravelPoint pt = points.get(name.toLowerCase());
        if (pt != null) {
            pt.setLocation(loc);
            save();
        }
    }

    public void removeLocation(String name) {
        points.remove(name.toLowerCase());
        save();
    }

    public void addWaystone(String name, Location loc, WaystoneType type) {
        waystones.put(name.toLowerCase(), new Waystone(name, loc, type));
        save();
    }

    public void moveWaystone(String name, Location loc) {
        Waystone ws = waystones.get(name.toLowerCase());
        if (ws != null) {
            waystones.put(name.toLowerCase(), new Waystone(ws.getName(), loc, ws.getType()));
            save();
        }
    }

    public void removeWaystone(String name) {
        waystones.remove(name.toLowerCase());
        save();
    }

    public Collection<FastTravelPoint> getPoints() { return points.values(); }

    public FastTravelPoint getPoint(String name) { return points.get(name.toLowerCase()); }

    public Collection<Waystone> getWaystones() { return waystones.values(); }

    public Waystone getWaystone(String name) { return waystones.get(name.toLowerCase()); }

    /** Spawn every waystone defined in waystones.yml. */
    public void spawnAllWaystones() {
        plugin.getLogger().info("[FastTravelManager] Spawning " + waystones.size() + " waystones...");
        for (Waystone ws : waystones.values()) {
            spawnWaystone(ws);
        }
    }

    /**
     * Places the active beacon furniture for a single waystone if it does not
     * already exist at the target location.
     */
    public void spawnWaystone(Waystone ws) {
        Location loc = ws.getLocation();
        if (loc.getWorld() == null) {
            plugin.getLogger().warning("[FastTravelManager] Waystone '" + ws.getName() + "' has null world");
            return;
        }
        if(!loc.getChunk().isLoaded()) loc.getChunk().load();
        FurnitureMechanic existing = NexoFurniture.furnitureMechanic(loc.getBlock());
        if (existing != null) {
            plugin.getLogger().info("[FastTravelManager] Waystone '" + ws.getName() + "' already present at "
                    + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()
                    + " (" + existing.getItemID() + ")");
            return;
        }
        String id = ws.getType() == WaystoneType.TOWN ? "base_beacon_blue" : "base_beacon_red";
        FurnitureMechanic mech = NexoFurniture.furnitureMechanic(id);
        if (mech == null) {
            plugin.getLogger().severe("[FastTravelManager] Furniture ID '" + id + "' not registered!");
            return;
        }
        NexoFurniture.place(id, loc, 0f, BlockFace.NORTH);
        plugin.getLogger().info("[FastTravelManager] Placed waystone '" + ws.getName() + "' at "
                + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
    }

    public FastTravelPoint getNearestPoint(Location loc, double maxDistance) {
        double best = maxDistance * maxDistance;
        FastTravelPoint bestPt = null;
        for (FastTravelPoint pt : points.values()) {
            if (!pt.getLocation().getWorld().equals(loc.getWorld())) continue;
            double d = pt.getLocation().distanceSquared(loc);
            if (d < best) { best = d; bestPt = pt; }
        }
        return bestPt;
    }

    public Waystone getNearestWaystone(Location loc, double maxDistance) {
        double best = maxDistance * maxDistance;
        Waystone bestWs = null;
        for (Waystone ws : waystones.values()) {
            if (!ws.getLocation().getWorld().equals(loc.getWorld())) continue;
            double d = ws.getLocation().distanceSquared(loc);
            if (d < best) { best = d; bestWs = ws; }
        }
        return bestWs;
    }

    public void recordUse(Player player, String name) {
        lastUsed.put(player.getUniqueId(), name.toLowerCase());
    }

    public String getLastUsed(Player player) {
        return lastUsed.get(player.getUniqueId());
    }

    public Location getNearestUnlockedTown(Player player) {
        Location from = player.getLocation();
        double best = Double.MAX_VALUE;
        Location bestLoc = null;
        for (FastTravelPoint pt : points.values()) {
            if (!pt.isTown()) continue;
            if (!isUnlocked(player, pt.getName())) continue;
            double d = from.distanceSquared(pt.getLocation());
            if (d < best) { best = d; bestLoc = pt.getLocation(); }
        }
        return bestLoc;
    }

    public void unlock(Player player, String name) {
        unlocked.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(name.toLowerCase());
        save();
    }

    public boolean isUnlocked(Player player, String name) {
        return unlocked.getOrDefault(player.getUniqueId(), Collections.emptySet()).contains(name.toLowerCase());
    }

    public Set<String> getUnlocked(Player player) {
        return unlocked.getOrDefault(player.getUniqueId(), Collections.emptySet());
    }

    public Main getPlugin() { return plugin; }
}
