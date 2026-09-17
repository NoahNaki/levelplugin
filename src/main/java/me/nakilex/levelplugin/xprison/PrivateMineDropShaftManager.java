package me.nakilex.levelplugin.xprison;

import dev.drawethree.libs.worldguardwrapper.region.IWrappedRegion;
import dev.drawethree.libs.worldguardwrapper.selection.ICuboidSelection;
import dev.drawethree.xprivatemines.api.XPrivateMinesAPI;
import dev.drawethree.xprivatemines.api.events.PrivateMineCreateEvent;
import dev.drawethree.xprivatemines.api.events.PrivateMineDeleteEvent;
import dev.drawethree.xprivatemines.api.events.PrivateMineExpandEvent;
import dev.drawethree.xprivatemines.api.manager.PrivateMinesManager;
import dev.drawethree.xprivatemines.api.model.PrivateMine;
import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps a drop shaft around every private mine: a ring of air directly around the mining box
 * with the schematic's bedrock tub (walls, floor and stair lip) rebuilt one ring further out, so a
 * player can step off any edge of the mine and fall straight to the floor.
 *
 * <p>X-PrivateMines has no wall/border concept: {@code mine.schem} ships a bedrock ring hugging the
 * mining box, and expanding just grows the box {@code expandLevel} blocks on X/Z into the
 * surrounding stone. So the tub is rebuilt from the mine's live region instead of the schematic:
 * <pre>
 *   WALL | GAP | MINE ... MINE | GAP | WALL      (gap width configurable, default 1)
 * </pre>
 * The manual {@code /pmine expand} path fires no {@link PrivateMineExpandEvent} (only force/auto
 * expand do), so each mine's region is polled and compared with the last box this class built
 * around; events only trigger an earlier check.</p>
 *
 * <p>Only real blocks outside the mining box are touched. The interior belongs to X-PrivateMines:
 * with {@code packet-mines-settings.clear-real-interior: true} it wipes old wall blocks that end up
 * inside a grown box itself.</p>
 */
public final class PrivateMineDropShaftManager implements Listener {

    private static final String CONFIG_ROOT = "xprison.private-mine-drop-shaft";

    private final Main plugin;
    private final File stateFile;

    private final Map<UUID, Box> appliedBoxes = new HashMap<>();
    private final Set<UUID> queuedMines = new HashSet<>();
    private final Deque<Job> jobs = new ArrayDeque<>();

    private PrivateMinesManager minesManager;
    private BukkitTask pollTask;
    private BukkitTask workTask;
    private boolean enabled;

    // Settings
    private Set<String> schematics = new HashSet<>();
    private int gapWidth;
    private int floorDepth;
    private int clearAboveLayers;
    private int blocksPerTick;
    private boolean cancelFallDamage;
    private BlockData wallData;
    private Material rimStairsMaterial;
    private BlockData rimCornerData;

    public PrivateMineDropShaftManager(Main plugin) {
        this.plugin = plugin;
        this.stateFile = new File(plugin.getDataFolder(), "private-mine-shafts.yml");
    }

    public void enable() {
        if (!plugin.getConfig().getBoolean(CONFIG_ROOT + ".enabled", true)) {
            return;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("X-PrivateMines")) {
            return;
        }
        if (!loadSettings()) {
            return;
        }
        loadState();

        Bukkit.getPluginManager().registerEvents(this, plugin);
        long interval = Math.max(20L, plugin.getConfig().getLong(CONFIG_ROOT + ".check-interval-ticks", 40L));
        pollTask = Bukkit.getScheduler().runTaskTimer(plugin, this::pollMines, 40L, interval);
        workTask = Bukkit.getScheduler().runTaskTimer(plugin, this::processJobs, 1L, 1L);
        enabled = true;
        plugin.getLogger().info("Private mine drop shafts active for schematics " + schematics
                + " (gap " + gapWidth + ", wall " + wallData.getMaterial() + ").");
    }

    public void disable() {
        if (pollTask != null) {
            pollTask.cancel();
            pollTask = null;
        }
        if (workTask != null) {
            workTask.cancel();
            workTask = null;
        }
        // Unfinished jobs are simply redone next start: their box was never recorded as applied.
        jobs.clear();
        queuedMines.clear();
        HandlerList.unregisterAll(this);
        enabled = false;
    }

    private boolean loadSettings() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(CONFIG_ROOT);
        List<String> configuredSchematics = section != null && section.isList("schematics")
                ? section.getStringList("schematics")
                : List.of("mine");
        schematics = new HashSet<>();
        for (String name : configuredSchematics) {
            schematics.add(name.toLowerCase(Locale.ROOT));
        }
        gapWidth = Math.max(1, plugin.getConfig().getInt(CONFIG_ROOT + ".gap-width", 1));
        floorDepth = Math.max(1, plugin.getConfig().getInt(CONFIG_ROOT + ".floor-depth", 6));
        clearAboveLayers = Math.max(1, plugin.getConfig().getInt(CONFIG_ROOT + ".clear-above-layers", 2));
        blocksPerTick = Math.max(100, plugin.getConfig().getInt(CONFIG_ROOT + ".blocks-per-tick", 4000));
        cancelFallDamage = plugin.getConfig().getBoolean(CONFIG_ROOT + ".cancel-fall-damage", true);

        Material wall = parseMaterial(plugin.getConfig().getString(CONFIG_ROOT + ".wall-material", "BEDROCK"));
        Material stairs = parseMaterial(plugin.getConfig().getString(CONFIG_ROOT + ".rim-stairs-material", "STONE_STAIRS"));
        Material corner = parseMaterial(plugin.getConfig().getString(CONFIG_ROOT + ".rim-corner-material", "STONE"));
        if (wall == null || !wall.isBlock() || corner == null || !corner.isBlock()
                || stairs == null || !(stairs.createBlockData() instanceof Stairs)) {
            plugin.getLogger().warning("Private mine drop shafts disabled: invalid wall/rim material in " + CONFIG_ROOT + ".");
            return false;
        }
        wallData = wall.createBlockData();
        rimStairsMaterial = stairs;
        rimCornerData = corner.createBlockData();
        return true;
    }

    private static Material parseMaterial(String name) {
        return name == null ? null : Material.matchMaterial(name.trim());
    }

    // ----------------------------------------------------------------------------------------
    // Detection
    // ----------------------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExpand(PrivateMineExpandEvent event) {
        // Fired before the region is rebuilt (after an async clear), so just check again soon.
        Bukkit.getScheduler().runTaskLater(plugin, this::pollMines, 20L);
    }

    @EventHandler
    public void onCreate(PrivateMineCreateEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, this::pollMines, 40L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDelete(PrivateMineDeleteEvent event) {
        UUID id = event.getMine().getUuid();
        appliedBoxes.remove(id);
        jobs.removeIf(job -> job.mineId.equals(id));
        queuedMines.remove(id);
        saveState();
    }

    private void pollMines() {
        if (!enabled || !resolveManager() || !minesManager.isMinesReady()) {
            return;
        }
        for (PrivateMine mine : minesManager.getAll()) {
            if (mine == null || mine.getSchematic() == null
                    || !schematics.contains(mine.getSchematic().getName().toLowerCase(Locale.ROOT))) {
                continue;
            }
            Box box = currentBox(mine);
            if (box == null || box.equals(appliedBoxes.get(mine.getUuid())) || queuedMines.contains(mine.getUuid())) {
                continue;
            }
            World world = Bukkit.getWorld(box.world);
            if (world == null) {
                continue;
            }
            jobs.add(buildJob(mine.getUuid(), world, box, appliedBoxes.get(mine.getUuid())));
            queuedMines.add(mine.getUuid());
        }
    }

    private boolean resolveManager() {
        if (minesManager != null) {
            return true;
        }
        try {
            XPrivateMinesAPI api = XPrivateMinesAPI.getInstance();
            minesManager = api == null ? null : api.getMinesManager();
        } catch (RuntimeException | LinkageError ex) {
            minesManager = null;
        }
        return minesManager != null;
    }

    private static Box currentBox(PrivateMine mine) {
        if (mine.getMine() == null) {
            return null;
        }
        IWrappedRegion region = mine.getMine().getRegion();
        if (region == null || !(region.getSelection() instanceof ICuboidSelection selection)) {
            return null;
        }
        Location min = selection.getMinimumPoint();
        Location max = selection.getMaximumPoint();
        if (min == null || max == null || min.getWorld() == null) {
            return null;
        }
        return new Box(min.getWorld().getName(),
                Math.min(min.getBlockX(), max.getBlockX()), Math.min(min.getBlockY(), max.getBlockY()),
                Math.min(min.getBlockZ(), max.getBlockZ()), Math.max(min.getBlockX(), max.getBlockX()),
                Math.max(min.getBlockY(), max.getBlockY()), Math.max(min.getBlockZ(), max.getBlockZ()));
    }

    // ----------------------------------------------------------------------------------------
    // Shell geometry
    // ----------------------------------------------------------------------------------------

    private Job buildJob(UUID mineId, World world, Box box, Box previous) {
        Job job = new Job(mineId, world, box);
        int wallRing = gapWidth + 1;
        int floorBottom = box.minY - floorDepth;
        int rimY = box.maxY + 1;
        BlockData air = Material.AIR.createBlockData();

        // A shrunk mine leaves its old gap and wall outside the new shell: fill that band solid
        // (the stone it replaced is gone) and restore a walkable surface on top.
        if (previous != null && previous.world.equals(box.world)) {
            for (int x = previous.minX - wallRing; x <= previous.maxX + wallRing; x++) {
                for (int z = previous.minZ - wallRing; z <= previous.maxZ + wallRing; z++) {
                    if (box.ringDistance(x, z) <= wallRing) {
                        continue;
                    }
                    for (int y = Math.min(floorBottom, previous.minY - floorDepth); y <= previous.maxY; y++) {
                        job.add(x, y, z, wallData);
                    }
                    job.add(x, previous.maxY + 1, z, rimCornerData);
                    for (int y = previous.maxY + 2; y <= previous.maxY + clearAboveLayers; y++) {
                        job.add(x, y, z, air);
                    }
                }
            }
        }

        for (int x = box.minX - wallRing; x <= box.maxX + wallRing; x++) {
            for (int z = box.minZ - wallRing; z <= box.maxZ + wallRing; z++) {
                int ring = box.ringDistance(x, z);

                // Floor under the whole shell, so the gap and an expanded box never sit over the void.
                for (int y = floorBottom; y < box.minY; y++) {
                    job.add(x, y, z, wallData);
                }

                if (ring == 0) {
                    // Mining box: X-PrivateMines owns the interior. Only open up the surface above it
                    // (terrain or an old stair lip left over from a smaller box).
                    for (int y = rimY; y < rimY + clearAboveLayers; y++) {
                        job.add(x, y, z, air);
                    }
                } else if (ring <= gapWidth) {
                    for (int y = box.minY; y < rimY + clearAboveLayers; y++) {
                        job.add(x, y, z, air);
                    }
                } else {
                    for (int y = box.minY; y <= box.maxY; y++) {
                        job.add(x, y, z, wallData);
                    }
                    job.add(x, rimY, z, rimData(box, x, z, wallRing));
                }
            }
        }
        return job;
    }

    /** Stair lip on the straight sides facing away from the mine, plain block on the corners. */
    private BlockData rimData(Box box, int x, int z, int wallRing) {
        boolean west = x == box.minX - wallRing;
        boolean east = x == box.maxX + wallRing;
        boolean north = z == box.minZ - wallRing;
        boolean south = z == box.maxZ + wallRing;
        if ((west || east) && (north || south)) {
            return rimCornerData;
        }
        Stairs stairs = (Stairs) rimStairsMaterial.createBlockData();
        stairs.setHalf(Stairs.Half.BOTTOM);
        stairs.setShape(Stairs.Shape.STRAIGHT);
        stairs.setFacing(west ? BlockFace.WEST : east ? BlockFace.EAST : north ? BlockFace.NORTH : BlockFace.SOUTH);
        return stairs;
    }

    // ----------------------------------------------------------------------------------------
    // Execution
    // ----------------------------------------------------------------------------------------

    private void processJobs() {
        int budget = blocksPerTick;
        while (budget > 0 && !jobs.isEmpty()) {
            Job job = jobs.peek();
            if (job.world != Bukkit.getWorld(job.world.getName())) {
                jobs.poll();
                queuedMines.remove(job.mineId);
                continue;
            }
            while (budget > 0 && job.hasNext()) {
                job.applyNext();
                budget--;
            }
            if (!job.hasNext()) {
                jobs.poll();
                queuedMines.remove(job.mineId);
                appliedBoxes.put(job.mineId, job.box);
                saveState();
                plugin.getLogger().info("Rebuilt private mine drop shaft for " + job.mineId + " around " + job.box + ".");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!cancelFallDamage || event.getCause() != EntityDamageEvent.DamageCause.FALL
                || !(event.getEntity() instanceof Player player) || minesManager == null) {
            return;
        }
        PrivateMine mine = minesManager.getPrivateMineAtLocation(player.getLocation());
        if (mine != null && mine.getSchematic() != null
                && schematics.contains(mine.getSchematic().getName().toLowerCase(Locale.ROOT))) {
            event.setCancelled(true);
        }
    }

    // ----------------------------------------------------------------------------------------
    // Persistence
    // ----------------------------------------------------------------------------------------

    private void loadState() {
        appliedBoxes.clear();
        if (!stateFile.isFile()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(stateFile);
        ConfigurationSection mines = yaml.getConfigurationSection("mines");
        if (mines == null) {
            return;
        }
        for (String key : mines.getKeys(false)) {
            ConfigurationSection s = mines.getConfigurationSection(key);
            if (s == null) {
                continue;
            }
            try {
                appliedBoxes.put(UUID.fromString(key), new Box(s.getString("world", ""),
                        s.getInt("min-x"), s.getInt("min-y"), s.getInt("min-z"),
                        s.getInt("max-x"), s.getInt("max-y"), s.getInt("max-z")));
            } catch (IllegalArgumentException ignored) {
                // Skip a hand-edited entry with a bad UUID.
            }
        }
    }

    private void saveState() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Box> entry : appliedBoxes.entrySet()) {
            String path = "mines." + entry.getKey();
            Box box = entry.getValue();
            yaml.set(path + ".world", box.world);
            yaml.set(path + ".min-x", box.minX);
            yaml.set(path + ".min-y", box.minY);
            yaml.set(path + ".min-z", box.minZ);
            yaml.set(path + ".max-x", box.maxX);
            yaml.set(path + ".max-y", box.maxY);
            yaml.set(path + ".max-z", box.maxZ);
        }
        try {
            yaml.save(stateFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save " + stateFile.getName() + ": " + ex.getMessage());
        }
    }

    // ----------------------------------------------------------------------------------------
    // Model
    // ----------------------------------------------------------------------------------------

    private record Box(String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        /** Horizontal ring index around the box: 0 inside, 1 for the first ring outside, and so on. */
        int ringDistance(int x, int z) {
            int dx = x < minX ? minX - x : x > maxX ? x - maxX : 0;
            int dz = z < minZ ? minZ - z : z > maxZ ? z - maxZ : 0;
            return Math.max(dx, dz);
        }

        @Override
        public String toString() {
            return world + " " + minX + "," + minY + "," + minZ + " -> " + maxX + "," + maxY + "," + maxZ;
        }
    }

    private static final class Job {
        private final UUID mineId;
        private final World world;
        private final Box box;
        private final List<int[]> positions = new ArrayList<>();
        private final List<BlockData> data = new ArrayList<>();
        private int cursor;

        Job(UUID mineId, World world, Box box) {
            this.mineId = mineId;
            this.world = world;
            this.box = box;
        }

        void add(int x, int y, int z, BlockData blockData) {
            positions.add(new int[]{x, y, z});
            data.add(blockData);
        }

        boolean hasNext() {
            return cursor < positions.size();
        }

        void applyNext() {
            int[] pos = positions.get(cursor);
            BlockData target = data.get(cursor);
            cursor++;
            if (pos[1] < world.getMinHeight() || pos[1] >= world.getMaxHeight()) {
                return;
            }
            Block block = world.getBlockAt(pos[0], pos[1], pos[2]);
            if (!block.getBlockData().matches(target)) {
                block.setBlockData(target, false);
            }
        }
    }
}
