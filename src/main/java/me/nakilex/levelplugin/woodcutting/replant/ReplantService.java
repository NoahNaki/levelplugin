package me.nakilex.levelplugin.woodcutting.replant;

import me.nakilex.levelplugin.woodcutting.WoodcuttingConfig;
import me.nakilex.levelplugin.woodcutting.tree.TreeDetectionResult;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class ReplantService {
    private final WoodcuttingConfig config;
    public ReplantService(WoodcuttingConfig config) { this.config = config; }

    public void replant(TreeDetectionResult tree) {
        if (!config.autoReplantEnabled()) return;
        Material sapling = tree.type().sapling();
        if (tree.wasLargeTree() && config.replantLargeTrees()) placeTwoByTwoSaplings(tree.root(), sapling);
        else placeSapling(tree.root(), sapling);
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
}
