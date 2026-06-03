package me.nakilex.levelplugin.player.woodcutting.tree;

import me.nakilex.levelplugin.player.woodcutting.WoodcuttingConfig;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;

public class TreeDetector {
    private final WoodcuttingConfig config;
    private final TreeTypeRegistry treeTypeRegistry;
    private final TreeRootFinder rootFinder;
    private final TreeValidator treeValidator;

    public TreeDetector(WoodcuttingConfig config, TreeTypeRegistry treeTypeRegistry, TreeRootFinder rootFinder, TreeValidator treeValidator) {
        this.config = config;
        this.treeTypeRegistry = treeTypeRegistry;
        this.rootFinder = rootFinder;
        this.treeValidator = treeValidator;
    }

    public TreeDetectionResult detect(Block clicked, Player player) {
        TreeType type = treeTypeRegistry.fromLog(clicked.getType());
        if (type == null) return TreeDetectionResult.invalid(TreeDetectionInvalidReason.UNKNOWN_TREE_TYPE, null, null, clicked, Set.of(), Set.of(), Set.of());
        Block root = rootFinder.findRoot(clicked, type);
        if (clicked.getY() - root.getY() > config.maxHeightAboveRoot()) {
            return TreeDetectionResult.invalid(TreeDetectionInvalidReason.CLICKED_TOO_HIGH, type, root, clicked, Set.of(), Set.of(), Set.of());
        }

        TreeBox box = TreeBox.fromRoot(root, type.heuristic());
        Set<Block> logs = findConnectedLogsInsideBox(clicked, box, type);
        TreeParts treeParts = findTreePartsNearLogs(logs, box.expand(config.leafBoxExpansion()), type);
        Set<Block> leaves = treeParts.leaves();
        Set<Block> attachedBlocks = treeParts.attachedBlocks();

        if (logs.size() < config.minimumLogs()) return TreeDetectionResult.invalid(TreeDetectionInvalidReason.TOO_FEW_LOGS, type, root, clicked, logs, leaves, attachedBlocks);
        if (leaves.size() < config.minimumLeaves()) return TreeDetectionResult.invalid(TreeDetectionInvalidReason.TOO_FEW_LEAVES, type, root, clicked, logs, leaves, attachedBlocks);
        if (logs.size() > config.maxLogs()) return TreeDetectionResult.invalid(TreeDetectionInvalidReason.TOO_MANY_LOGS, type, root, clicked, logs, leaves, attachedBlocks);
        if (leaves.size() > config.maxLeaves()) return TreeDetectionResult.invalid(TreeDetectionInvalidReason.TOO_MANY_LEAVES, type, root, clicked, logs, leaves, attachedBlocks);
        TreeDetectionInvalidReason validationFailure = treeValidator.validationFailure(root, logs, leaves, type);
        if (validationFailure != null) return TreeDetectionResult.invalid(validationFailure, type, root, clicked, logs, leaves, attachedBlocks);
        return TreeDetectionResult.valid(type, root, clicked, logs, leaves, attachedBlocks, treeValidator.isLargeTree(root, logs, type));
    }

    private Set<Block> findConnectedLogsInsideBox(Block start, TreeBox box, TreeType type) {
        Set<Block> found = new LinkedHashSet<>();
        Set<Block> queued = new HashSet<>();
        Queue<Block> queue = new ArrayDeque<>();
        queue.add(start);
        queued.add(start);
        while (!queue.isEmpty()) {
            Block block = queue.poll();
            if (!box.contains(block) || !type.isLog(block.getType())) continue;
            if (!found.add(block)) continue;
            if (found.size() > config.maxLogs()) break;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Block relative = block.getRelative(dx, dy, dz);
                        if (queued.add(relative)) queue.add(relative);
                    }
                }
            }
        }
        return found;
    }

    private TreeParts findTreePartsNearLogs(Set<Block> logs, TreeBox box, TreeType type) {
        Set<Block> leaves = new LinkedHashSet<>();
        Set<Block> attachedBlocks = new LinkedHashSet<>();
        Set<Block> queuedLeaves = new HashSet<>();
        Queue<Block> leafQueue = new ArrayDeque<>();

        int seedRadius = Math.max(1, config.mixedLeafRadius());
        for (Block log : logs) {
            scanTreePartsAround(log, seedRadius, box, type, leaves, attachedBlocks, queuedLeaves, leafQueue);
            if (leaves.size() > config.maxLeaves()) return new TreeParts(leaves, attachedBlocks);
        }

        expandCanopyFromDetectedLeaves(box, type, leaves, attachedBlocks, queuedLeaves, leafQueue);
        return new TreeParts(leaves, attachedBlocks);
    }

    private void expandCanopyFromDetectedLeaves(TreeBox box, TreeType type, Set<Block> leaves, Set<Block> attachedBlocks,
                                                Set<Block> queuedLeaves, Queue<Block> leafQueue) {
        int expansionRadius = Math.max(1, config.canopyExpansionRadius());
        while (!leafQueue.isEmpty() && leaves.size() <= config.maxLeaves()) {
            scanTreePartsAround(leafQueue.poll(), expansionRadius, box, type, leaves, attachedBlocks, queuedLeaves, leafQueue);
        }
    }

    private void scanTreePartsAround(Block center, int radius, TreeBox box, TreeType type, Set<Block> leaves,
                                     Set<Block> attachedBlocks, Set<Block> queuedLeaves, Queue<Block> leafQueue) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    Block nearby = center.getRelative(dx, dy, dz);
                    if (!box.contains(nearby)) continue;
                    Material material = nearby.getType();
                    if (matchesLeaf(type, material)) {
                        if (leaves.add(nearby) && queuedLeaves.add(nearby)) leafQueue.add(nearby);
                        if (leaves.size() > config.maxLeaves()) return;
                    } else if (type.isAttachedNaturalBlock(material)) {
                        attachedBlocks.add(nearby);
                    }
                }
            }
        }
    }

    private boolean matchesLeaf(TreeType type, Material material) {
        return type.isLeaf(material) || (config.allowMixedLeaves() && treeTypeRegistry.isAnyConfiguredLeaf(material));
    }

    private record TreeParts(Set<Block> leaves, Set<Block> attachedBlocks) {}
}
