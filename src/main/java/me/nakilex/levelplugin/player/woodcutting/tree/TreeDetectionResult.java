package me.nakilex.levelplugin.player.woodcutting.tree;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TreeDetectionResult {
    private static final TreeDetectionResult INVALID = new TreeDetectionResult(false, TreeDetectionInvalidReason.UNKNOWN_TREE_TYPE,
            null, null, null, Set.of(), Set.of(), Set.of(), List.of(), false);

    private final boolean valid;
    private final TreeDetectionInvalidReason invalidReason;
    private final TreeType type;
    private final Block root;
    private final Block clicked;
    private final Set<Block> logs;
    private final Set<Block> leaves;
    private final Set<Block> attachedBlocks;
    private final List<CapturedBlock> snapshots;
    private final boolean largeTree;

    private TreeDetectionResult(boolean valid, TreeDetectionInvalidReason invalidReason, TreeType type, Block root, Block clicked,
                                Set<Block> logs, Set<Block> leaves, Set<Block> attachedBlocks,
                                List<CapturedBlock> snapshots, boolean largeTree) {
        this.valid = valid;
        this.invalidReason = invalidReason;
        this.type = type;
        this.root = root;
        this.clicked = clicked;
        this.logs = Collections.unmodifiableSet(new LinkedHashSet<>(logs));
        this.leaves = Collections.unmodifiableSet(new LinkedHashSet<>(leaves));
        this.attachedBlocks = Collections.unmodifiableSet(new LinkedHashSet<>(attachedBlocks));
        this.snapshots = List.copyOf(snapshots);
        this.largeTree = largeTree;
    }

    public static TreeDetectionResult invalid() { return INVALID; }

    public static TreeDetectionResult invalid(TreeDetectionInvalidReason reason, TreeType type, Block root, Block clicked,
                                              Set<Block> logs, Set<Block> leaves, Set<Block> attachedBlocks) {
        return new TreeDetectionResult(false, reason == null ? TreeDetectionInvalidReason.UNKNOWN_TREE_TYPE : reason,
                type, root, clicked, logs, leaves, attachedBlocks, List.of(), false);
    }

    public static TreeDetectionResult valid(TreeType type, Block root, Block clicked, Set<Block> logs, Set<Block> leaves,
                                            Set<Block> attachedBlocks, boolean largeTree) {
        Set<Block> all = new LinkedHashSet<>(logs);
        all.addAll(leaves);
        all.addAll(attachedBlocks);
        List<CapturedBlock> snapshots = all.stream()
                .map(block -> new CapturedBlock(block, block.getState(), block.getBlockData(), block.getLocation(),
                        block.getLocation().toVector().subtract(root.getLocation().toVector())))
                .toList();
        return new TreeDetectionResult(true, TreeDetectionInvalidReason.NONE, type, root, clicked, logs, leaves, attachedBlocks, snapshots, largeTree);
    }

    public boolean valid() { return valid; }
    public TreeDetectionInvalidReason invalidReason() { return invalidReason; }
    public TreeType type() { return type; }
    public Block root() { return root; }
    public Block clicked() { return clicked; }
    public Set<Block> logs() { return logs; }
    public Set<Block> leaves() { return leaves; }
    public Set<Block> attachedBlocks() { return attachedBlocks; }
    public List<CapturedBlock> snapshots() { return snapshots; }
    public boolean wasLargeTree() { return largeTree; }

    public Location pivotLocation() {
        if (root == null) return null;
        if (!largeTree) return root.getLocation().add(0.5D, 0.5D, 0.5D);

        int rootY = root.getY();
        List<Block> rootBlocks = logs.stream()
                .filter(block -> block.getY() == rootY)
                .filter(block -> Math.abs(block.getX() - root.getX()) <= 1 && Math.abs(block.getZ() - root.getZ()) <= 1)
                .toList();
        if (rootBlocks.size() < 2) return root.getLocation().add(0.5D, 0.5D, 0.5D);

        double averageX = rootBlocks.stream().mapToDouble(Block::getX).average().orElse(root.getX()) + 0.5D;
        double averageZ = rootBlocks.stream().mapToDouble(Block::getZ).average().orElse(root.getZ()) + 0.5D;
        return new Location(root.getWorld(), averageX, rootY + 0.5D, averageZ);
    }

    public int treeHeight() {
        if (logs.isEmpty()) return 1;
        int minY = logs.stream().mapToInt(Block::getY).min().orElse(root == null ? 0 : root.getY());
        int maxY = logs.stream().mapToInt(Block::getY).max().orElse(minY);
        return Math.max(1, maxY - minY + 1);
    }

    public Set<Block> allBlocks() {
        Set<Block> all = new LinkedHashSet<>(logs);
        all.addAll(leaves);
        all.addAll(attachedBlocks);
        return all;
    }

    public record CapturedBlock(Block block, BlockState state, BlockData blockData, Location originalLocation, Vector relativeOffset) {}
}
