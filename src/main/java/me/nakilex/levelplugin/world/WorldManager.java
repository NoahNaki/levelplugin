package me.nakilex.levelplugin.world;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.VoidWorldGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.World.Environment;
import org.bukkit.GameRule;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import me.nakilex.levelplugin.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collection;

/**
 * Basic world management handling creation, loading and spawn points.
 */
public class WorldManager {
    private final Main plugin;
    private final Map<String, Location> spawns = new HashMap<>();
    private final Set<String> persistentWorlds = new HashSet<>();
    private final Map<String, Integer> maxBuildHeights = new HashMap<>();
    private File file;
    private FileConfiguration config;

    public WorldManager(Main plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        spawns.clear();
        persistentWorlds.clear();
        maxBuildHeights.clear();
        file = new File(plugin.getDataFolder(), "worlds.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);
        if (config.isConfigurationSection("spawns")) {
            for (String w : config.getConfigurationSection("spawns").getKeys(false)) {
                String path = "spawns." + w;
                World world = Bukkit.getWorld(w);
                if (world == null) continue;
                double x = config.getDouble(path + ".x");
                double y = config.getDouble(path + ".y");
                double z = config.getDouble(path + ".z");
                float yaw = (float) config.getDouble(path + ".yaw", 0);
                float pitch = (float) config.getDouble(path + ".pitch", 0);
                spawns.put(w.toLowerCase(), new Location(world, x, y, z, yaw, pitch));
            }
        }
        List<String> worlds = config.getStringList("worlds");
        for (String w : worlds) {
            persistentWorlds.add(w.toLowerCase());
            if (Bukkit.getWorld(w) == null) {
                importWorld(w);
            }
        }
        if (config.isConfigurationSection("max-build-height")) {
            for (String worldName : config.getConfigurationSection("max-build-height").getKeys(false)) {
                int height = config.getInt("max-build-height." + worldName);
                if (height > 0) {
                    maxBuildHeights.put(worldName.toLowerCase(), height);
                }
            }
        }
        for (World world : Bukkit.getWorlds()) {
            applyBooleanGameRulesFromPrimary(world);
            applyConfiguredMaxBuildHeight(world);
        }
    }

    public synchronized void reload() {
        load();
    }

    private void save() {
        for (Map.Entry<String, Location> e : spawns.entrySet()) {
            Location loc = e.getValue();
            String path = "spawns." + e.getKey();
            config.set(path + ".x", loc.getX());
            config.set(path + ".y", loc.getY());
            config.set(path + ".z", loc.getZ());
            config.set(path + ".yaw", loc.getYaw());
            config.set(path + ".pitch", loc.getPitch());
        }
        for (Map.Entry<String, Integer> entry : maxBuildHeights.entrySet()) {
            config.set("max-build-height." + entry.getKey(), entry.getValue());
        }
        config.set("worlds", new java.util.ArrayList<>(persistentWorlds));
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public World createWorld(String name, WorldType type, Environment env) {
        WorldCreator wc = new WorldCreator(name).environment(env).type(type);
        wc.generateStructures(false);
        if (env == Environment.NORMAL && type == WorldType.FLAT) {
            wc.generator(new VoidWorldGenerator());
        }
        World world = Bukkit.createWorld(wc);
        if (world != null) {
            applyBooleanGameRulesFromPrimary(world);
            persistentWorlds.add(name.toLowerCase());
            save();
        }
        return world;
    }

    public World importWorld(String name) {
        WorldCreator wc = new WorldCreator(name);
        wc.generateStructures(false);
        World world = Bukkit.createWorld(wc);
        if (world != null) {
            applyBooleanGameRulesFromPrimary(world);
            persistentWorlds.add(name.toLowerCase());
            save();
        }
        return world;
    }

    public void setSpawn(World world, Location loc) {
        spawns.put(world.getName().toLowerCase(), loc);
        save();
    }

    /**
     * Delete the given world folder and remove its spawn entry.
     * The world will be unloaded if currently loaded.
     */
    public boolean deleteWorld(String name) {
        World world = Bukkit.getWorld(name);
        if (world != null) {
            Bukkit.unloadWorld(world, false);
        }
        File folder = new File(plugin.getServer().getWorldContainer(), name);
        if (!folder.exists()) {
            return false;
        }
        FileUtil.deleteDirectory(folder);
        spawns.remove(name.toLowerCase());
        persistentWorlds.remove(name.toLowerCase());
        save();
        return true;
    }

    public Location getSpawn(World world) {
        return spawns.getOrDefault(world.getName().toLowerCase(), world.getSpawnLocation());
    }

    public boolean teleport(Player player, String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return false;
        Location loc = getSpawn(world);
        player.teleport(loc);
        return true;
    }

    public List<World> listWorlds() {
        return Bukkit.getWorlds();
    }

    /**
     * Unload a world from memory without deleting files.
     */
    public boolean unloadWorld(String name) {
        World world = Bukkit.getWorld(name);
        return world != null && Bukkit.unloadWorld(world, true);
    }

    /**
     * Clone a world folder (including entities) under a new name.
     */
    public boolean cloneWorld(String sourceName, String targetName) {
        if (sourceName == null || targetName == null) {
            return false;
        }
        if (Bukkit.getWorld(targetName) != null) {
            return false;
        }
        File sourceFolder = new File(plugin.getServer().getWorldContainer(), sourceName);
        if (!sourceFolder.exists()) {
            return false;
        }
        File targetFolder = new File(plugin.getServer().getWorldContainer(), targetName);
        if (targetFolder.exists()) {
            return false;
        }

        World sourceWorld = Bukkit.getWorld(sourceName);
        if (sourceWorld != null) {
            sourceWorld.save();
        }

        try {
            FileUtil.copyDirectory(sourceFolder, targetFolder);
        } catch (IOException | RuntimeException e) {
            if (e instanceof RuntimeException re && re.getCause() instanceof IOException io) {
                io.printStackTrace();
            } else {
                e.printStackTrace();
            }
            return false;
        }

        World cloned = importWorld(targetName);
        return cloned != null;
    }

    public void ensureWorldsLoaded(Collection<String> names) {
        boolean changed = false;
        for (String name : names) {
            String lower = name.toLowerCase();
            if (Bukkit.getWorld(name) == null) {
                importWorld(name);
            } else if (persistentWorlds.add(lower)) {
                changed = true;
            }
        }
        if (changed) {
            save();
        }
    }

    public void ensureWorldsLoaded(String... names) {
        ensureWorldsLoaded(Arrays.asList(names));
    }

    public void applyBooleanGameRuleToAll(GameRule<Boolean> rule, boolean value) {
        if (rule == null) return;
        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(rule, value);
        }
    }

    public void applyBooleanGameRulesFromPrimary(World target) {
        World primary = Bukkit.getWorld("world");
        if (primary == null || target == null || primary.equals(target)) {
            return;
        }
        for (GameRule<?> rule : GameRule.values()) {
            if (rule.getType() != Boolean.class) {
                continue;
            }
            @SuppressWarnings("unchecked")
            GameRule<Boolean> boolRule = (GameRule<Boolean>) rule;
            Boolean value = primary.getGameRuleValue(boolRule);
            if (value != null) {
                target.setGameRule(boolRule, value);
            }
        }
    }

    public Integer getConfiguredMaxBuildHeight(World world) {
        if (world == null) {
            return null;
        }
        return maxBuildHeights.get(world.getName().toLowerCase());
    }

    public boolean setMaxBuildHeight(World world, int height) {
        if (world == null) {
            return false;
        }
        int clamped = Math.max(world.getMinHeight() + 1, height);
        maxBuildHeights.put(world.getName().toLowerCase(), clamped);
        save();
        return applyMaxBuildHeight(world, clamped);
    }

    private void applyConfiguredMaxBuildHeight(World world) {
        Integer height = getConfiguredMaxBuildHeight(world);
        if (height == null) {
            return;
        }
        applyMaxBuildHeight(world, height);
    }

    private boolean applyMaxBuildHeight(World world, int height) {
        try {
            var method = world.getClass().getMethod("setMaxHeight", int.class);
            method.invoke(world, height);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().warning("Failed to apply max build height for " + world.getName() + ": " + e.getMessage());
            return false;
        }
    }
}
