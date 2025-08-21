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
        file = new File(plugin.getDataFolder(), "regions.yml");
        if (!file.exists()) {
            File legacy = new File(plugin.getDataFolder(), "fasttravel.yml");
            if (legacy.exists()) {
                legacy.renameTo(file);
            } else {
                plugin.saveResource("regions.yml", false);
            }
        }
        config = YamlConfiguration.loadConfiguration(file);

        String sectionName = config.isConfigurationSection("regions") ? "regions" : "locations";
        if (config.isConfigurationSection(sectionName)) {
            for (String key : config.getConfigurationSection(sectionName).getKeys(false)) {
                String path = sectionName + "." + key;
                String world = config.getString(path + ".world");
                double x = config.getDouble(path + ".x");
                double y = config.getDouble(path + ".y");
                double z = config.getDouble(path + ".z");
                String colorName = config.getString(path + ".color", "WHITE");
                String desc = config.getString(path + ".desc", "");
                double radius = config.getDouble(path + ".radius", 10);
                boolean town = config.getBoolean(path + ".town", false);
                int exp = config.getInt(path + ".exp", 0);
                org.bukkit.World w = plugin.getServer().getWorld(world);
                if (w == null && "world2".equalsIgnoreCase(world)) {
                    w = plugin.getServer().getWorld("world");
                }
                Location loc = new Location(w, x, y, z);
                String displayName = me.nakilex.levelplugin.utils.TextUtil.beautifyWords(key);
                FastTravelPoint pt = new FastTravelPoint(displayName, ChatColor.valueOf(colorName), desc, loc, radius, town, exp);
                points.put(key.toLowerCase(), pt);
            }
        }

        if (config.isConfigurationSection("players")) {
            me.nakilex.levelplugin.player.config.PlayerConfig pCfg = plugin.getPlayerConfig();
            for (String id : config.getConfigurationSection("players").getKeys(false)) {
                List<String> list = config.getStringList("players." + id);
                UUID uuid = UUID.fromString(id);
                unlocked.put(uuid, new HashSet<>(list));
                if (pCfg != null) {
                    pCfg.getConfig().set("players." + id + ".fasttravel", list);
                }
            }
            if (pCfg != null) pCfg.saveConfigFile();
            config.set("players", null);
            try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void save() {
        config.set("regions", null);
        config.set("locations", null);
        for (Map.Entry<String, FastTravelPoint> e : points.entrySet()) {
            FastTravelPoint pt = e.getValue();
            String path = "regions." + e.getKey();
            Location loc = pt.getLocation();
            config.set(path + ".world", loc.getWorld().getName());
            config.set(path + ".x", loc.getX());
            config.set(path + ".y", loc.getY());
            config.set(path + ".z", loc.getZ());
            config.set(path + ".color", pt.getColor().name());
            config.set(path + ".desc", pt.getDescription());
            config.set(path + ".radius", pt.getRadius());
            config.set(path + ".town", pt.isTown());
            config.set(path + ".exp", pt.getExpReward());
        }
        config.set("players", null);
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void addLocation(String name,
                             ChatColor color,
                             String desc,
                             Location loc,
                             double radius,
                             boolean town,
                             int exp) {
        String displayName = me.nakilex.levelplugin.utils.TextUtil.beautifyWords(name);
        points.put(name.toLowerCase(), new FastTravelPoint(displayName, color, desc, loc, radius, town, exp));
        save();
    }

    public void addLocation(String name, ChatColor color, String desc, Location loc, double radius, boolean town) {
        addLocation(name, color, desc, loc, radius, town, 0);
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

    /** Return the fast travel point located at the given world location, or null if none. */
    public FastTravelPoint getPointAt(Location loc) {
        if (loc == null) return null;
        for (FastTravelPoint pt : points.values()) {
            Location pLoc = pt.getLocation();
            if (pLoc.getWorld().equals(loc.getWorld()) && loc.distance(pLoc) <= pt.getRadius()) {
                return pt;
            }
        }
        return null;
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
        unlock(player, name, false);
    }

    public void unlock(Player player, String name, boolean recordCodex) {
        UUID id = player.getUniqueId();
        unlocked.computeIfAbsent(id, k -> new HashSet<>()).add(name.toLowerCase());
        if (plugin.getPlayerConfig() != null) {
            plugin.getPlayerConfig().savePlayerData(id);
        }
        if (recordCodex) {
            Main.getInstance().getCodexManager().recordLocation(player, me.nakilex.levelplugin.utils.TextUtil.beautifyWords(name));
        }
        Main.getInstance().getQuestManager().handleDiscover(player, name.toLowerCase());
        Main.getInstance().getQuestManager().handleWaystoneUnlock(player, name.toLowerCase());

        FastTravelPoint pt = points.get(name.toLowerCase());
        if (pt != null) {
            int exp = pt.getExpReward();
            me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, " ", 45);
            me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§6§lRegion Discovered");
            me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, pt.getColor() + pt.getName());
            if (exp > 0) {
                plugin.getLevelManager().addXP(player, exp);
                String expColor = me.nakilex.levelplugin.utils.ChatFormatter.experienceColor();
                String expLabel = me.nakilex.levelplugin.utils.ChatFormatter.experienceLabel();
                player.sendMessage(" ");
                me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player,
                        expColor + "+" + exp + " <glyph:experience_orb_icon> " + expLabel);
            }
            me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, " ", 45);
        }
    }

    public boolean isUnlocked(Player player, String name) {
        return getUnlocked(player.getUniqueId()).contains(name.toLowerCase());
    }

    public Set<String> getUnlocked(Player player) {
        return getUnlocked(player.getUniqueId());
    }

    public Set<String> getUnlocked(UUID uuid) {
        return unlocked.getOrDefault(uuid, Collections.emptySet());
    }

    public void setUnlocked(UUID uuid, Collection<String> names) {
        unlocked.put(uuid, new HashSet<>(names));
    }

    public void clearUnlocked(UUID uuid) {
        unlocked.remove(uuid);
    }

    public Main getPlugin() { return plugin; }
}
