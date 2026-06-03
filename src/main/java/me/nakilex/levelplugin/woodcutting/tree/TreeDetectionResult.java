package me.nakilex.levelplugin.woodcutting.tree;

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
    private static final TreeDetectionResult INVALID = new TreeDetectionResult(false, null, null, null, Set.of(), Set.of(), List.of(), false);

    private final boolean valid;
    private final TreeType type;
    private final Block root;
    private final Block clicked;
    private final Set<Block> logs;
    private final Set<Block> leaves;
    private final List<CapturedBlock> snapshots;
    private final boolean largeTree;

    private TreeDetectionResult(boolean valid, TreeType type, Block root, Block clicked, Set<Block> logs, Set<Block> leaves,
                                List<CapturedBlock> snapshots, boolean largeTree) {
        this.valid = valid;
        this.type = type;
        this.root = root;
        this.clicked = clicked;
        this.logs = Collections.unmodifiableSet(new LinkedHashSet<>(logs));
        this.leaves = Collections.unmodifiableSet(new LinkedHashSet<>(leaves));
        this.snapshots = List.copyOf(snapshots);
        this.largeTree = largeTree;
    }

    public static TreeDetectionResult invalid() { return INVALID; }

    public static TreeDetectionResult valid(TreeType type, Block root, Block clicked, Set<Block> logs, Set<Block> leaves, boolean largeTree) {
        Set<Block> all = new LinkedHashSet<>(logs);
        all.addAll(leaves);
        List<CapturedBlock> snapshots = all.stream()
                .map(block -> new CapturedBlock(block, block.getState(), block.getBlockData(), block.getLocation(),
                        block.getLocation().toVector().subtract(root.getLocation().toVector())))
                .toList();
        return new TreeDetectionResult(true, type, root, clicked, logs, leaves, snapshots, largeTree);
    }

    public boolean valid() { return valid; }
    public TreeType type() { return type; }
    public Block root() { return root; }
    public Block clicked() { return clicked; }
    public Set<Block> logs() { return logs; }
    public Set<Block> leaves() { return leaves; }
    public List<CapturedBlock> snapshots() { return snapshots; }
    public boolean wasLargeTree() { return largeTree; }

    public Set<Block> allBlocks() {
        Set<Block> all = new LinkedHashSet<>(logs);
        all.addAll(leaves);
        return all;
    }

    public record CapturedBlock(Block block, BlockState state, BlockData blockData, Location originalLocation, Vector relativeOffset) {}
}
