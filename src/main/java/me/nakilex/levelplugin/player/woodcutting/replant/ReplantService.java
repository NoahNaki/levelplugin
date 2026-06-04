package me.nakilex.levelplugin.player.woodcutting.replant;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.woodcutting.WoodcuttingConfig;
import me.nakilex.levelplugin.player.woodcutting.tree.TreeDetectionResult;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;

public class ReplantService {
    private final Main plugin;
    private final WoodcuttingConfig config;
    private final Set<BukkitTask> regrowTasks = new HashSet<>();

    public ReplantService(Main plugin, WoodcuttingConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void replant(TreeDetectionResult tree) {
        if (config.autoReplantEnabled()) {
            Material sapling = tree.type().sapling();
            if (tree.wasLargeTree() && config.replantLargeTrees()) placeTwoByTwoSaplings(tree.root(), sapling);
            else placeSapling(tree.root(), sapling);
        }
        scheduleExactRegrow(tree);
    }

    public void shutdown() {
        for (BukkitTask task : Set.copyOf(regrowTasks)) task.cancel();
        regrowTasks.clear();
    }

    private void scheduleExactRegrow(TreeDetectionResult tree) {
        if (!config.exactRegrowEnabled()) return;
        Runnable restore = () -> restoreOriginalTree(tree);
        if (config.exactRegrowDelayTicks() <= 0L) {
            restore.run();
            return;
        }
        BukkitTask[] handle = new BukkitTask[1];
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    restore.run();
                } finally {
                    if (handle[0] != null) regrowTasks.remove(handle[0]);
                    regrowTasks.removeIf(BukkitTask::isCancelled);
                }
            }
        };
        handle[0] = runnable.runTaskLater(plugin, config.exactRegrowDelayTicks());
        regrowTasks.add(handle[0]);
    }

    private void restoreOriginalTree(TreeDetectionResult tree) {
        if (config.exactRegrowOnlyIfSpaceClear() && !canRestoreAll(tree)) {
            if (config.debug()) plugin.getLogger().info("[Woodcutting] Exact regrow skipped because tree space is obstructed at root " + format(tree.root()));
            return;
        }
        for (TreeDetectionResult.CapturedBlock snapshot : tree.snapshots()) {
            Block target = snapshot.originalLocation().getBlock();
            if (!canReplaceForRegrow(target, tree.type().sapling())) continue;
            target.setBlockData(snapshot.blockData(), false);
        }
        if (config.debug()) plugin.getLogger().info("[Woodcutting] Exact regrow restored " + tree.snapshots().size() + " blocks at root " + format(tree.root()));
    }

    private boolean canRestoreAll(TreeDetectionResult tree) {
        Material sapling = tree.type().sapling();
        for (TreeDetectionResult.CapturedBlock snapshot : tree.snapshots()) {
            if (!canReplaceForRegrow(snapshot.originalLocation().getBlock(), sapling)) return false;
        }
        return true;
    }

    private boolean canReplaceForRegrow(Block block, Material sapling) {
        Material type = block.getType();
        if (type.isAir()) return true;
        return config.exactRegrowReplaceSaplings() && (type == sapling || Tag.SAPLINGS.isTagged(type));
    }

    private void placeTwoByTwoSaplings(Block root, Material sapling) {
        if (placeSapling(root, sapling)) {
            placeSapling(root.getRelative(1, 0, 0), sapling);
            placeSapling(root.getRelative(0, 0, 1), sapling);
            placeSapling(root.getRelative(1, 0, 1), sapling);
        }
    }

    private boolean placeSapling(Block block, Material sapling) {
        if (!block.getType().isAir()) return false;
        if (!canSupportSapling(block.getRelative(BlockFace.DOWN).getType())) return false;
        block.setType(sapling, false);
        return true;
    }

    private boolean canSupportSapling(Material material) {
        return Tag.DIRT.isTagged(material) || material == Material.GRASS_BLOCK || material == Material.FARMLAND;
    }

    private String format(Block block) {
        return block.getWorld().getName() + " " + block.getX() + "," + block.getY() + "," + block.getZ();
    }
}
