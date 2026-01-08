package me.nakilex.levelplugin.arena.instance;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.VoidWorldGenerator;
import me.nakilex.levelplugin.utils.FileUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages instanced copies of the arena region. The template is captured from
 * the flatland world once at startup and pasted into lightweight void worlds
 * whenever a new arena match is required.
 */
public class ArenaInstanceManager {
    private static final String TEMPLATE_WORLD = "flatland";
    private static final int CORNER1_X = -444;
    private static final int CORNER1_Y = -48;
    private static final int CORNER1_Z = -4204;
    private static final int CORNER2_X = -273;
    private static final int CORNER2_Y = 79;
    private static final int CORNER2_Z = -4386;

    private static final int SPAWN1_X = -362;
    private static final int SPAWN1_Y = -31;
    private static final int SPAWN1_Z = -4329;
    private static final int SPAWN2_X = -362;
    private static final int SPAWN2_Y = -31;
    private static final int SPAWN2_Z = -4251;

    private final Main plugin;
    private final List<BlockCopy> templateBlocks;
    private final Vector spawnOffsetOne;
    private final Vector spawnOffsetTwo;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final AtomicInteger instanceCounter = new AtomicInteger();
    private final Map<String, ArenaInstance> activeInstances = new HashMap<>();
    private final boolean templateLoaded;

    private record BlockCopy(int x, int y, int z, BlockData data) {}

    public ArenaInstanceManager(Main plugin) {
        this.plugin = plugin;
        World world = Bukkit.getWorld(TEMPLATE_WORLD);
        if (world == null) {
            plugin.getLogger().warning("Flatland world not loaded. Arena instances are disabled.");
            this.templateBlocks = List.of();
            this.spawnOffsetOne = new Vector();
            this.spawnOffsetTwo = new Vector();
            this.minX = this.minY = this.minZ = 0;
            this.templateLoaded = false;
            return;
        }

        this.minX = Math.min(CORNER1_X, CORNER2_X);
        int maxX = Math.max(CORNER1_X, CORNER2_X);
        this.minY = Math.min(CORNER1_Y, CORNER2_Y);
        int maxY = Math.max(CORNER1_Y, CORNER2_Y);
        this.minZ = Math.min(CORNER1_Z, CORNER2_Z);
        int maxZ = Math.max(CORNER1_Z, CORNER2_Z);

        List<BlockCopy> blocks = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    BlockData data = block.getBlockData();
                    if (!data.getMaterial().isAir()) {
                        blocks.add(new BlockCopy(x - minX, y - minY, z - minZ, data));
                    }
                }
            }
        }
        this.templateBlocks = Collections.unmodifiableList(blocks);
        this.spawnOffsetOne = new Vector(
                SPAWN1_X - minX + 0.5,
                SPAWN1_Y - minY + 0.1,
                SPAWN1_Z - minZ + 0.5
        );
        this.spawnOffsetTwo = new Vector(
                SPAWN2_X - minX + 0.5,
                SPAWN2_Y - minY + 0.1,
                SPAWN2_Z - minZ + 0.5
        );
        this.templateLoaded = true;
    }

    /**
     * Create a fresh arena instance world and paste the template into it.
     *
     * @return the created instance, or {@code null} if the template is missing
     */
    public ArenaInstance createInstance() {
        if (!templateLoaded) {
            return null;
        }

        String worldName = "arena_" + instanceCounter.incrementAndGet();
        WorldCreator creator = new WorldCreator(worldName);
        creator.generator(new VoidWorldGenerator());
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        World world = Bukkit.createWorld(creator);
        if (world == null) {
            plugin.getLogger().warning("Failed to create arena world '" + worldName + "'.");
            return null;
        }

        world.setKeepSpawnInMemory(false);
        world.setAutoSave(false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        world.setTime(6000);

        pasteTemplate(world, 0, 0, 0);

        Location spawnOne = new Location(world,
                spawnOffsetOne.getX(),
                spawnOffsetOne.getY(),
                spawnOffsetOne.getZ(),
                0f,
                0f);
        Location spawnTwo = new Location(world,
                spawnOffsetTwo.getX(),
                spawnOffsetTwo.getY(),
                spawnOffsetTwo.getZ(),
                180f,
                0f);
        world.setSpawnLocation(spawnOne.getBlockX(), (int) Math.floor(spawnOne.getY()), spawnOne.getBlockZ());

        ArenaInstance instance = new ArenaInstance(world, spawnOne, spawnTwo);
        activeInstances.put(worldName, instance);
        return instance;
    }

    private void pasteTemplate(World world, int baseX, int baseY, int baseZ) {
        for (BlockCopy copy : templateBlocks) {
            Block block = world.getBlockAt(baseX + copy.x(), baseY + copy.y(), baseZ + copy.z());
            block.setBlockData(copy.data(), false);
        }
    }

    /**
     * Destroy the provided instance, unloading and deleting its world.
     */
    public void destroyInstance(ArenaInstance instance) {
        if (instance == null) return;
        World world = instance.getWorld();
        if (world == null) return;
        activeInstances.remove(world.getName());
        List<Player> players = new ArrayList<>(world.getPlayers());
        Location fallback = Bukkit.getWorlds().isEmpty()
                ? null
                : Bukkit.getWorlds().get(0).getSpawnLocation();
        for (Player player : players) {
            if (fallback != null) {
                player.teleport(fallback);
            }
        }
        Bukkit.unloadWorld(world, false);
        File folder = new File(plugin.getServer().getWorldContainer(), world.getName());
        FileUtil.deleteDirectory(folder);
    }

    /**
     * Remove and delete all active arena instance worlds.
     */
    public void cleanup() {
        for (ArenaInstance instance : new ArrayList<>(activeInstances.values())) {
            destroyInstance(instance);
        }
        activeInstances.clear();
    }

    public Collection<ArenaInstance> getActiveInstances() {
        return Collections.unmodifiableCollection(activeInstances.values());
    }

    public boolean isInstanceWorld(World world) {
        if (world == null) {
            return false;
        }
        return activeInstances.containsKey(world.getName());
    }

    public boolean isTemplateLoaded() {
        return templateLoaded;
    }
}
