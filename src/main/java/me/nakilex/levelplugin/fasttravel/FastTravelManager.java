package me.nakilex.levelplugin.fasttravel;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.fasttravel.data.FastTravelPoint;
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
    private final Map<UUID, Set<String>> unlocked = new HashMap<>();
    private final Map<UUID, String> lastUsed = new HashMap<>();
    private File file;
    private FileConfiguration config;

    public FastTravelManager(Main plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        file = new File(plugin.getDataFolder(), "fasttravel.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);

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

    public Collection<FastTravelPoint> getPoints() { return points.values(); }

    public FastTravelPoint getPoint(String name) { return points.get(name.toLowerCase()); }

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

    /**
     * Returns the unlocked fast travel point the player is currently standing at.
     *
     * @param player the player to check
     * @return the {@link FastTravelPoint} if one is found within its radius, otherwise {@code null}
     */
    public FastTravelPoint getNearbyUnlockedPoint(Player player) {
        Location loc = player.getLocation();
        for (FastTravelPoint pt : points.values()) {
            if (!isUnlocked(player, pt.getName())) continue;
            if (!pt.getLocation().getWorld().equals(loc.getWorld())) continue;
            if (loc.distance(pt.getLocation()) <= pt.getRadius()) return pt;
        }
        return null;
    }

    public Main getPlugin() { return plugin; }
}
