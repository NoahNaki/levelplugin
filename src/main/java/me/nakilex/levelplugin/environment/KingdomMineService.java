package me.nakilex.levelplugin.environment;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Recreates an X-PrivateMines-style private mine inside a kingdom instance world.
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
 *   <li><b>the X-Prison mine</b> itself, registered over the ore cuboid.</li>
 * </ul>
 *
 * <p>Kingdom instance worlds are ephemeral - {@code onQuit} and {@code removeKingdom} unload
 * the world <em>and delete its folder</em> - but X-Prison persists mines and reloads them at
 * boot, so everything created here is torn down with its world. {@link #sweepOrphans()} clears
 * whatever survived a crash.</p>
 *
 * <p>X-Prison and WorldGuard are soft dependencies: every call into them is guarded and
 * wrapped, so LevelPlugin still loads on a server without the prison core.</p>
 */
public final class KingdomMineService {

    /** Mines and regions owned by this system; the suffix is the owner's short uuid. */
    private static final String MINE_NAME_PREFIX = "kingdom_";
    /** Suffix for the outer protected region; the ore region takes the bare mine name. */
    private static final String SHELL_SUFFIX = "_area";
    /** X-Prison's own flag gating enchants and autosell to mine regions. */
    private static final String ENCHANTS_FLAG = "upc-enchants";

    private final Main plugin;
    private final boolean enabled;
    private final String paletteSourceMineName;
    private final int resetIntervalSeconds;

    public KingdomMineService(Main plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("environment.kingdom-mine.enabled", true);
        this.paletteSourceMineName = plugin.getConfig().getString("environment.kingdom-mine.palette-source", "A");
        this.resetIntervalSeconds = plugin.getConfig().getInt("environment.kingdom-mine.reset-interval-seconds", 300);
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
        String name = mineName(ownerId);
        createRegions(world, name, ore, shell);
        createMine(world, name, ore);
    }

    private void createMine(World world, String name, int[] ore) {
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

            applyPalette(minesApi, mine, name);
            if (resetIntervalSeconds > 0) {
                minesApi.setMineResetInterval(mine, resetIntervalSeconds);
            }
            minesApi.resetMine(mine);
            plugin.getLogger().info("[KingdomMine] Created " + name + " in " + world.getName()
                    + " ore " + ore[0] + "," + ore[1] + "," + ore[2]
                    + " -> " + ore[3] + "," + ore[4] + "," + ore[5]);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[KingdomMine] Could not create mine " + name + ": " + throwable);
        }
    }

    /** Copies the block composition from the configured source mine onto the new mine. */
    private void applyPalette(dev.drawethree.xprison.api.mines.XPrisonMinesAPI minesApi,
                              dev.drawethree.xprison.api.mines.model.Mine mine,
                              String name) {
        var source = minesApi.getMineByName(paletteSourceMineName);
        if (source == null) {
            // Mines are stored under their exact name (A, not a), so retry ignoring case
            // before giving up on the configured value.
            source = minesApi.getMines().stream()
                    .filter(candidate -> candidate.getName() != null
                            && candidate.getName().equalsIgnoreCase(paletteSourceMineName))
                    .findFirst()
                    .orElse(null);
        }
        if (source == null) {
            source = minesApi.getMines().stream()
                    .filter(candidate -> !candidate.getName().startsWith(MINE_NAME_PREFIX))
                    .findFirst()
                    .orElse(null);
            if (source == null) {
                plugin.getLogger().warning("[KingdomMine] No source mine to copy a palette from; "
                        + name + " will be empty. Set environment.kingdom-mine.palette-source.");
                return;
            }
            plugin.getLogger().warning("[KingdomMine] Palette source " + paletteSourceMineName
                    + " not found; using " + source.getName() + " instead.");
        }

        var sourcePalette = source.getBlockPalette();
        Map<String, Double> byId = new LinkedHashMap<>();
        for (var block : sourcePalette.getBlocks()) {
            byId.put(block.getId(), sourcePalette.getPercentage(block));
        }
        if (byId.isEmpty()) {
            plugin.getLogger().warning("[KingdomMine] Source mine " + source.getName()
                    + " has an empty palette; " + name + " will not fill.");
            return;
        }
        mine.getBlockPalette().setPaletteByIds(byId);
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
            applyEnchantsFlag(oreRegion, name);
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
    private void applyEnchantsFlag(com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion region,
                                   String name) {
        var flag = com.sk89q.worldguard.WorldGuard.getInstance().getFlagRegistry().get(ENCHANTS_FLAG);
        if (flag instanceof com.sk89q.worldguard.protection.flags.StateFlag stateFlag) {
            region.setFlag(stateFlag, com.sk89q.worldguard.protection.flags.StateFlag.State.ALLOW);
            return;
        }
        plugin.getLogger().warning("[KingdomMine] Flag " + ENCHANTS_FLAG
                + " is not registered; prison enchants may not fire in " + name);
    }

    /** Deletes the owner's mine and regions. Must run before the instance world is unloaded. */
    public void removeFor(UUID ownerId, World world) {
        if (!isAvailable() || ownerId == null) {
            return;
        }
        String name = mineName(ownerId);
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
     * Removes kingdom mines left behind by a crash. Their instance worlds are deleted on quit,
     * so any that exist at startup are dangling by definition.
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
}
