package me.nakilex.levelplugin.debug;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.VoidWorldGenerator;
import me.nakilex.levelplugin.utils.FileUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;

import java.io.File;

/**
 * Handles lifecycle of the temporary stronghold debug world.
 */
public final class StrongholdDebugWorldManager {
    public static final String WORLD_NAME = "stronghold_debug";

    private final Main plugin;

    public StrongholdDebugWorldManager(Main plugin) {
        this.plugin = plugin;
    }

    public void cleanupStaleWorldAtStartup() {
        deleteWorldIfPresent();
    }

    public void cleanupAtShutdown() {
        deleteWorldIfPresent();
    }

    public World recreateDebugWorld() {
        deleteWorldIfPresent();

        WorldCreator creator = new WorldCreator(WORLD_NAME)
                .environment(World.Environment.NORMAL)
                .type(WorldType.FLAT)
                .generator(new VoidWorldGenerator());
        creator.generateStructures(false);

        World world = Bukkit.createWorld(creator);
        if (world == null) {
            return null;
        }

        world.setSpawnLocation(0, 80, 0);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setTime(6000);
        if (plugin.getWorldManager() != null) {
            plugin.getWorldManager().applyBooleanGameRulesFromPrimary(world);
        }
        return world;
    }

    public void evacuatePlayersFromDebugWorld() {
        World debugWorld = Bukkit.getWorld(WORLD_NAME);
        if (debugWorld == null) {
            return;
        }
        Location fallback = fallbackLocation();
        if (fallback.getWorld() == null) {
            return;
        }
        for (Player player : debugWorld.getPlayers()) {
            player.teleport(fallback);
        }
    }

    public boolean deleteWorldIfPresent() {
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world != null) {
            evacuatePlayersFromDebugWorld();
            Bukkit.unloadWorld(world, false);
        }

        File folder = new File(plugin.getServer().getWorldContainer(), WORLD_NAME);
        if (!folder.exists()) {
            return true;
        }
        FileUtil.deleteDirectory(folder);
        return !folder.exists();
    }

    private Location fallbackLocation() {
        World primary = Bukkit.getWorld("world");
        if (primary == null && !Bukkit.getWorlds().isEmpty()) {
            primary = Bukkit.getWorlds().get(0);
        }
        if (primary != null) {
            return primary.getSpawnLocation();
        }
        return new Location(null, 0, 80, 0);
    }
}
