package me.nakilex.levelplugin.environment;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Recreates an X-PrivateMines-style private mine inside a kingdom plot.
 *
 * <p>XPrivateMines cannot be told where to place a mine - {@code createPrivateMine} takes no
 * location and its offsets are read-only - so a kingdom mine is assembled from the same parts
 * a pmine schematic declares, mapped onto the kingdom's pasted build:</p>
 *
 * <ul>
 *   <li><b>ore region</b> - the cuboid X-Prison refills, flagged {@code block-break: ALLOW}
 *       and {@code upc-enchants: ALLOW} so prison enchants and autosell fire inside it;</li>
 *   <li><b>protected shell</b> - the whole build, flagged {@code build: DENY} and
 *       {@code block-break: DENY} so only the ore volume is mineable;</li>
 *   <li><b>locked envelope</b> - future tiers are visible as protected bedrock around the
 *       centered active mine and are converted to ore as the owner's pickaxe levels up;</li>
 *   <li><b>the X-Prison mine</b> itself, registered over the ore cuboid.</li>
 * </ul>
 *
 * <p>Kingdom plots share one ephemeral runtime world, while X-Prison persists mines and
 * reloads them at boot. Each plot's mine and regions are therefore removed with its session;
 * {@link #sweepOrphans()} clears whatever survived a crash.</p>
 *
 * <p>X-Prison and WorldGuard are soft dependencies: every call into them is guarded and
 * wrapped, so LevelPlugin still loads on a server without the prison core.</p>
 */
public final class KingdomMineService implements Listener {

    /** Mines and regions owned by this system; the suffix is the owner's short uuid. */
    private static final String MINE_NAME_PREFIX = "kingdom_";
    /** Suffix for the outer protected region; the ore region takes the bare mine name. */
    private static final String SHELL_SUFFIX = "_area";
    /** X-Prison's own flag gating enchants and autosell to mine regions. */
    private static final String ENCHANTS_FLAG = "upc-enchants";
    /** X-Prison's WorldGuard flag allowing mine bombs in mine regions. */
    private static final String BOMBS_FLAG = "mine-bombs";

    private final Main plugin;
    private final boolean enabled;
    private final int resetIntervalSeconds;
    private final List<MineLevel> levels;
    private final Map<UUID, ActiveMine> activeMines = new HashMap<>();
    private final boolean dropShaftEnabled;
    private final int gapWidth;
    private final int floorDepth;
    private final int clearAboveLayers;
    private final boolean cancelFallDamage;

    public KingdomMineService(Main plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("environment.kingdom-mine.enabled", true);
        this.resetIntervalSeconds = plugin.getConfig().getInt("environment.kingdom-mine.reset-interval-seconds", 300);
        this.levels = loadLevels();
        String shaft = "environment.kingdom-mine.drop-shaft.";
        this.dropShaftEnabled = plugin.getConfig().getBoolean(shaft + "enabled", true);
        this.gapWidth = Math.max(1, plugin.getConfig().getInt(shaft + "gap-width", 1));
        this.floorDepth = Math.max(1, plugin.getConfig().getInt(shaft + "floor-depth", 6));
        this.clearAboveLayers = Math.max(1, plugin.getConfig().getInt(shaft + "clear-above-layers", 2));
        this.cancelFallDamage = plugin.getConfig().getBoolean(shaft + "cancel-fall-damage", true);
        if (Bukkit.getPluginManager().isPluginEnabled("X-Prison")) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }
    }

    /** @return whether kingdom mines are enabled and the prison core is present */
    public boolean isAvailable() {
        return enabled && Bukkit.getPluginManager().isPluginEnabled("X-Prison");
    }

    private boolean worldGuardPresent() {
        return Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
    }

    public static String mineName(UUID ownerId) {
        return MINE_NAME_PREFIX + ownerId.toString().substring(0, 8).toLowerCase(Locale.ROOT);
    }

    /**
     * Builds the owner's mine.
     *
     * @param ore   minX,minY,minZ,maxX,maxY,maxZ of the refilled cuboid
     * @param shell minX,minY,minZ,maxX,maxY,maxZ of the protected build around it
     */
    public void createFor(UUID ownerId, World world, int[] ore, int[] shell) {
        if (!isAvailable() || ownerId == null || world == null || ore == null) {
            return;
        }
        ActiveMine active = new ActiveMine(world, normalized(ore), shell == null ? null : normalized(shell), -1);
        activeMines.put(ownerId, active);
        applyLevel(ownerId, active, levelFor(ownerId), false);
    }

    private void createMine(World world, String name, int[] ore, MineLevel level) {
        try {
            var minesApi = dev.drawethree.xprison.api.XPrisonAPI.getInstance().getMinesApi();

            var stale = minesApi.getMineByName(name);
            if (stale != null) {
                minesApi.deleteMine(stale);
            }

            var selection = dev.drawethree.xprison.api.mines.model.MineSelection.of(
                    me.lucko.helper.serialize.Position.of(ore[0], ore[1], ore[2], world),
                    me.lucko.helper.serialize.Position.of(ore[3], ore[4], ore[5], world));
            var mine = minesApi.createMine(selection, name);
            if (mine == null) {
                plugin.getLogger().warning("[KingdomMine] X-Prison refused to create mine " + name);
                return;
            }

            mine.getBlockPalette().setPaletteByIds(level.palette());
            if (resetIntervalSeconds > 0) {
                minesApi.setMineResetInterval(mine, resetIntervalSeconds);
            }
            minesApi.resetMine(mine);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[KingdomMine] Could not create mine " + name + ": " + throwable);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickaxeLevelUp(dev.drawethree.xprison.api.pickaxelevels.event.PlayerPickaxeLevelUpEvent event) {
        if (event.getNewLevel() == null) {
            return;
        }
        UUID ownerId = event.getPlayer().getUniqueId();
        ActiveMine active = activeMines.get(ownerId);
        if (active == null) {
            return;
        }
        int newPickaxeLevel = event.getNewLevel().getLevel();
        int oldPickaxeLevel = event.getOldLevel() == null ? newPickaxeLevel : event.getOldLevel().getLevel();
        MineLevel level = levelForPickaxeLevel(newPickaxeLevel);
        if (level.number() == active.levelNumber()) {
            return;
        }
        // Rebirth intentionally downgrades the mine back to its early tiers. Only upward progression
        // should display the normal "Unlocked tier" announcement.
        applyLevel(ownerId, active, level, newPickaxeLevel > oldPickaxeLevel);
    }

    private void applyLevel(UUID ownerId, ActiveMine active, MineLevel level, boolean announce) {
        String name = mineName(ownerId);
        int[] unlocked = centeredFootprint(active.maximumOre(), level.size());
        fillMaximumWithBedrock(active.world(), active.maximumOre());
        if (dropShaftEnabled) {
            carveDropShaft(active.world(), active.maximumOre(), unlocked);
        }
        createMine(active.world(), name, unlocked, level);
        createRegions(active.world(), name, unlocked, active.shell());
        activeMines.put(ownerId, active.withLevelNumber(level.number()));

        if (announce) {
            Player player = Bukkit.getPlayer(ownerId);
            if (player != null && player.isOnline()) {
                player.sendMessage("\u00a76\u00a7lKINGDOM MINE \u00a78\u00bb \u00a7aUnlocked tier " + level.number()
                        + " \u00a77(" + level.size() + "x" + height(active.maximumOre()) + "x" + level.size() + ").");
            }
        }
        plugin.getLogger().info("[KingdomMine] " + name + " is tier " + level.number()
                + " at pickaxe level " + level.minimumPickaxeLevel() + "+, size "
                + level.size() + "x" + height(active.maximumOre()) + "x" + level.size() + ".");
    }

    private MineLevel levelFor(UUID ownerId) {
        int pickaxeLevel = plugin.getPlayerConfig() == null
                ? 1
                : Math.max(1, plugin.getPlayerConfig().getXPrisonPickaxeLevel(ownerId));
        Player player = Bukkit.getPlayer(ownerId);
        if (player != null && player.isOnline()) {
            try {
                Optional<dev.drawethree.xprison.api.pickaxelevels.model.PickaxeLevel> live =
                        dev.drawethree.xprison.api.XPrisonAPI.getInstance().getPickaxeLevelsApi().getPickaxeLevel(player);
                if (live.isPresent()) {
                    pickaxeLevel = Math.max(pickaxeLevel, live.get().getLevel());
                }
            } catch (RuntimeException | LinkageError ignored) {
                // The persisted snapshot is deliberately the fallback for an absent/unequipped pickaxe.
            }
        }
        return levelForPickaxeLevel(pickaxeLevel);
    }

    private MineLevel levelForPickaxeLevel(int pickaxeLevel) {
        MineLevel selected = levels.getFirst();
        for (MineLevel candidate : levels) {
            if (pickaxeLevel < candidate.minimumPickaxeLevel()) {
                break;
            }
            selected = candidate;
        }
        return selected;
    }

    private List<MineLevel> loadLevels() {
        var section = plugin.getConfig().getConfigurationSection("environment.kingdom-mine.levels");
        List<MineLevel> loaded = new ArrayList<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                var levelSection = section.getConfigurationSection(key);
                if (levelSection == null) {
                    continue;
                }
                int number;
                try {
                    number = Integer.parseInt(key);
                } catch (NumberFormatException ignored) {
                    plugin.getLogger().warning("[KingdomMine] Ignoring non-numeric mine tier '" + key + "'.");
                    continue;
                }
                int minimum = Math.max(1, levelSection.getInt("minimum-pickaxe-level", 1));
                int size = Math.max(1, levelSection.getInt("size", 10));
                Map<String, Double> palette = new LinkedHashMap<>();
                var paletteSection = levelSection.getConfigurationSection("blocks");
                if (paletteSection != null) {
                    for (String blockId : paletteSection.getKeys(false)) {
                        double percentage = paletteSection.getDouble(blockId);
                        if (percentage > 0.0D) {
                            palette.put(blockId.toUpperCase(Locale.ROOT), percentage);
                        }
                    }
                }
                if (palette.isEmpty()) {
                    plugin.getLogger().warning("[KingdomMine] Ignoring tier " + number + " because its palette is empty.");
                    continue;
                }
                double total = palette.values().stream().mapToDouble(Double::doubleValue).sum();
                if (Math.abs(total - 100.0D) > 0.001D) {
                    plugin.getLogger().warning("[KingdomMine] Tier " + number + " block percentages total "
                            + total + " instead of 100; X-Prison will normalize the palette.");
                }
                loaded.add(new MineLevel(number, minimum, size, Map.copyOf(palette)));
            }
        }
        loaded.sort(Comparator.comparingInt(MineLevel::minimumPickaxeLevel)
                .thenComparingInt(MineLevel::number));
        if (loaded.isEmpty()) {
            plugin.getLogger().warning("[KingdomMine] No progression tiers configured; using a 10x10 stone mine.");
            return List.of(new MineLevel(1, 1, 10, Map.of("STONE", 100.0D)));
        }
        return List.copyOf(loaded);
    }

    private void fillMaximumWithBedrock(World world, int[] maximum) {
        try (var session = com.sk89q.worldedit.WorldEdit.getInstance()
                .newEditSession(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world))) {
            var region = new com.sk89q.worldedit.regions.CuboidRegion(
                    com.sk89q.worldedit.math.BlockVector3.at(maximum[0], maximum[1], maximum[2]),
                    com.sk89q.worldedit.math.BlockVector3.at(maximum[3], maximum[4], maximum[5]));
            session.setBlocks((com.sk89q.worldedit.regions.Region) region,
                    com.sk89q.worldedit.world.block.BlockTypes.BEDROCK.getDefaultState());
            session.flushQueue();
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[KingdomMine] Could not place locked bedrock area: " + throwable);
        }
    }

    /**
     * Opens a drop shaft around the unlocked mine: an air ring directly around it, so a player
     * can step off any edge and fall to the floor. Inside the envelope the locked bedrock is the
     * wall. Near the top tiers the ring reaches past the envelope into the build's own bedrock
     * ring, so there the tub (floor, wall, stair lip) is rebuilt one ring further out, like
     * private mines do. Runs right after the envelope was refilled with bedrock, which already
     * undid the previous tier's ring inside it.
     */
    private void carveDropShaft(World world, int[] envelope, int[] unlocked) {
        int minY = unlocked[1];
        int maxY = unlocked[4];
        int floorBottom = minY - floorDepth;
        int wallRing = gapWidth + 1;
        var air = com.sk89q.worldedit.world.block.BlockTypes.AIR.getDefaultState();
        var bedrock = com.sk89q.worldedit.world.block.BlockTypes.BEDROCK.getDefaultState();
        var stone = com.sk89q.worldedit.world.block.BlockTypes.STONE.getDefaultState();
        try (var session = com.sk89q.worldedit.WorldEdit.getInstance()
                .newEditSession(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world))) {
            // Undo a larger tier's shell outside the envelope (a rebirth shrinks the mine): the
            // build's own ring directly around the envelope is bedrock with a stair lip, and
            // the ring we moved it to goes back to plain surface.
            int reach = wallRing;
            for (int x = envelope[0] - reach; x <= envelope[3] + reach; x++) {
                for (int z = envelope[2] - reach; z <= envelope[5] + reach; z++) {
                    int ring = ringDistance(envelope, x, z);
                    if (ring == 0) {
                        continue;
                    }
                    if (ring == 1) {
                        if (!session.getBlock(at(x, (minY + maxY) / 2, z)).getBlockType()
                                .equals(com.sk89q.worldedit.world.block.BlockTypes.AIR)) {
                            continue;
                        }
                        for (int y = minY; y <= maxY; y++) {
                            session.setBlock(at(x, y, z), bedrock);
                        }
                        session.setBlock(at(x, maxY + 1, z), rim(envelope, x, z, 1));
                    } else if (session.getBlock(at(x, maxY + 1, z)).getBlockType()
                            .equals(com.sk89q.worldedit.world.block.BlockTypes.STONE_STAIRS)) {
                        session.setBlock(at(x, maxY + 1, z), stone);
                    }
                }
            }

            for (int x = unlocked[0] - wallRing; x <= unlocked[3] + wallRing; x++) {
                for (int z = unlocked[2] - wallRing; z <= unlocked[5] + wallRing; z++) {
                    int ring = ringDistance(unlocked, x, z);
                    if (ring == 0) {
                        continue;
                    }
                    boolean outside = ringDistance(envelope, x, z) > 0;
                    if (outside) {
                        for (int y = floorBottom; y < minY; y++) {
                            session.setBlock(at(x, y, z), bedrock);
                        }
                    }
                    if (ring <= gapWidth) {
                        for (int y = minY; y <= maxY + clearAboveLayers; y++) {
                            session.setBlock(at(x, y, z), air);
                        }
                    } else if (outside) {
                        for (int y = minY; y <= maxY; y++) {
                            session.setBlock(at(x, y, z), bedrock);
                        }
                        session.setBlock(at(x, maxY + 1, z), rim(unlocked, x, z, wallRing));
                    }
                }
            }
            session.flushQueue();
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[KingdomMine] Could not carve the drop shaft: " + throwable);
        }
    }

    /** Stair lip facing away from the box on straight sides, plain stone on the corners. */
    private static com.sk89q.worldedit.world.block.BlockState rim(int[] box, int x, int z, int ring) {
        boolean west = x == box[0] - ring;
        boolean east = x == box[3] + ring;
        boolean north = z == box[2] - ring;
        boolean south = z == box[5] + ring;
        if ((west || east) && (north || south)) {
            return com.sk89q.worldedit.world.block.BlockTypes.STONE.getDefaultState();
        }
        var stairs = (org.bukkit.block.data.type.Stairs) org.bukkit.Material.STONE_STAIRS.createBlockData();
        stairs.setFacing(west ? org.bukkit.block.BlockFace.WEST : east ? org.bukkit.block.BlockFace.EAST
                : north ? org.bukkit.block.BlockFace.NORTH : org.bukkit.block.BlockFace.SOUTH);
        return com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(stairs);
    }

    private static com.sk89q.worldedit.math.BlockVector3 at(int x, int y, int z) {
        return com.sk89q.worldedit.math.BlockVector3.at(x, y, z);
    }

    /** Horizontal ring index around a box: 0 inside, 1 for the first ring outside, and so on. */
    private static int ringDistance(int[] box, int x, int z) {
        int dx = x < box[0] ? box[0] - x : x > box[3] ? x - box[3] : 0;
        int dz = z < box[2] ? box[2] - z : z > box[5] ? z - box[5] : 0;
        return Math.max(dx, dz);
    }

    /** The drop into the shaft is the full mine height; landing there should not kill. */
    @EventHandler(ignoreCancelled = true)
    public void onFallDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!dropShaftEnabled || !cancelFallDamage
                || event.getCause() != org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL
                || !(event.getEntity() instanceof Player player)) {
            return;
        }
        var loc = player.getLocation();
        int reach = gapWidth + 1;
        for (ActiveMine active : activeMines.values()) {
            int[] e = active.maximumOre();
            if (active.world().equals(loc.getWorld())
                    && ringDistance(e, loc.getBlockX(), loc.getBlockZ()) <= reach
                    && loc.getBlockY() >= e[1] - floorDepth && loc.getBlockY() <= e[4] + clearAboveLayers + 1) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private static int[] centeredFootprint(int[] maximum, int requestedSize) {
        int width = maximum[3] - maximum[0] + 1;
        int depth = maximum[5] - maximum[2] + 1;
        int size = Math.max(1, Math.min(requestedSize, Math.min(width, depth)));
        int minX = maximum[0] + (width - size) / 2;
        int minZ = maximum[2] + (depth - size) / 2;
        return new int[]{minX, maximum[1], minZ, minX + size - 1, maximum[4], minZ + size - 1};
    }

    private static int[] normalized(int[] box) {
        return new int[]{
                Math.min(box[0], box[3]), Math.min(box[1], box[4]), Math.min(box[2], box[5]),
                Math.max(box[0], box[3]), Math.max(box[1], box[4]), Math.max(box[2], box[5])
        };
    }

    private static int height(int[] box) {
        return box[4] - box[1] + 1;
    }

    /**
     * Creates the protected shell and the ore region, mirroring the flags a pmine schematic
     * declares. The shell is lower priority so the ore region wins where they overlap.
     */
    private void createRegions(World world, String name, int[] ore, int[] shell) {
        if (!worldGuardPresent()) {
            plugin.getLogger().warning("[KingdomMine] WorldGuard absent; " + name
                    + " will have no protection and prison enchants will not fire in it.");
            return;
        }
        try {
            var container = com.sk89q.worldguard.WorldGuard.getInstance()
                    .getPlatform().getRegionContainer();
            var manager = container.get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world));
            if (manager == null) {
                plugin.getLogger().warning("[KingdomMine] No WorldGuard region manager for " + world.getName());
                return;
            }

            if (shell != null) {
                var shellRegion = cuboid(name + SHELL_SUFFIX, shell);
                shellRegion.setPriority(1);
                shellRegion.setFlag(com.sk89q.worldguard.protection.flags.Flags.BUILD,
                        com.sk89q.worldguard.protection.flags.StateFlag.State.DENY);
                shellRegion.setFlag(com.sk89q.worldguard.protection.flags.Flags.BLOCK_BREAK,
                        com.sk89q.worldguard.protection.flags.StateFlag.State.DENY);
                manager.addRegion(shellRegion);
            }

            var oreRegion = cuboid(name, ore);
            oreRegion.setPriority(2);
            oreRegion.setFlag(com.sk89q.worldguard.protection.flags.Flags.BLOCK_BREAK,
                    com.sk89q.worldguard.protection.flags.StateFlag.State.ALLOW);
            oreRegion.setFlag(com.sk89q.worldguard.protection.flags.Flags.BUILD,
                    com.sk89q.worldguard.protection.flags.StateFlag.State.ALLOW);
            applyStateFlag(oreRegion, ENCHANTS_FLAG, name,
                    "prison enchants and autosell may not fire in ");
            applyStateFlag(oreRegion, BOMBS_FLAG, name,
                    "mine bombs may not fire in ");
            manager.addRegion(oreRegion);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[KingdomMine] Could not create regions for " + name + ": " + throwable);
        }
    }

    private static com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion cuboid(String name, int[] box) {
        return new com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion(
                name,
                com.sk89q.worldedit.math.BlockVector3.at(box[0], box[1], box[2]),
                com.sk89q.worldedit.math.BlockVector3.at(box[3], box[4], box[5]));
    }

    /**
     * X-Prison registers upc-enchants at runtime, so it is looked up by name rather than
     * referenced statically - the constant does not exist in its API jar.
     */
    private void applyStateFlag(com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion region,
                                String flagName,
                                String mineName,
                                String missingImpact) {
        var flag = com.sk89q.worldguard.WorldGuard.getInstance().getFlagRegistry().get(flagName);
        if (flag instanceof com.sk89q.worldguard.protection.flags.StateFlag stateFlag) {
            region.setFlag(stateFlag, com.sk89q.worldguard.protection.flags.StateFlag.State.ALLOW);
            return;
        }
        plugin.getLogger().warning("[KingdomMine] Flag " + flagName
                + " is not registered; " + missingImpact + mineName);
    }

    /** Deletes the owner's mine and regions without touching other plots in the shared world. */
    public void removeFor(UUID ownerId, World world) {
        if (!isAvailable() || ownerId == null) {
            return;
        }
        String name = mineName(ownerId);
        activeMines.remove(ownerId);
        try {
            var minesApi = dev.drawethree.xprison.api.XPrisonAPI.getInstance().getMinesApi();
            var mine = minesApi.getMineByName(name);
            if (mine != null) {
                minesApi.deleteMine(mine);
            }
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[KingdomMine] Could not delete mine " + name + ": " + throwable);
        }
        removeRegions(world, name);
    }

    private void removeRegions(World world, String name) {
        if (world == null || !worldGuardPresent()) {
            return;
        }
        try {
            var manager = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world));
            if (manager == null) {
                return;
            }
            manager.removeRegion(name);
            manager.removeRegion(name + SHELL_SUFFIX);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[KingdomMine] Could not remove regions for " + name + ": " + throwable);
        }
    }

    /**
     * Removes kingdom mines left behind by a crash. The shared world is rebuilt at startup,
     * so any persisted mine is dangling by definition.
     */
    public void sweepOrphans() {
        if (!isAvailable()) {
            return;
        }
        try {
            var minesApi = dev.drawethree.xprison.api.XPrisonAPI.getInstance().getMinesApi();
            List<dev.drawethree.xprison.api.mines.model.Mine> orphans = new ArrayList<>();
            for (var mine : minesApi.getMines()) {
                if (mine.getName() != null && mine.getName().startsWith(MINE_NAME_PREFIX)) {
                    orphans.add(mine);
                }
            }
            for (var orphan : orphans) {
                minesApi.deleteMine(orphan);
            }
            if (!orphans.isEmpty()) {
                plugin.getLogger().info("[KingdomMine] Cleared " + orphans.size() + " orphaned kingdom mine(s).");
            }
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[KingdomMine] Orphan sweep failed: " + throwable);
        }
    }

    private record MineLevel(int number, int minimumPickaxeLevel, int size, Map<String, Double> palette) {
    }

    private record ActiveMine(World world, int[] maximumOre, int[] shell, int levelNumber) {
        private ActiveMine withLevelNumber(int value) {
            return new ActiveMine(world, maximumOre, shell, value);
        }
    }
}
