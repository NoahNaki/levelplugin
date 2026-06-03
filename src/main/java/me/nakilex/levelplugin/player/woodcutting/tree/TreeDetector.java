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
        TreeParts treeParts = findTreePartsNearLogs(logs, box.expand(2), type);
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
        int radius = Math.max(1, config.mixedLeafRadius());
        for (Block log : logs) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        Block nearby = log.getRelative(dx, dy, dz);
                        if (!box.contains(nearby)) continue;
                        Material material = nearby.getType();
                        boolean directLeaf = type.isLeaf(material);
                        boolean directAttached = type.isAttachedNaturalBlock(material);
                        boolean mixedLeaf = config.allowMixedLeaves()
                                && treeTypeRegistry.isAnyConfiguredLeaf(material)
                                && isCloseEnoughToDetectedTree(nearby, logs, leaves, radius);
                        if (directLeaf || mixedLeaf) {
                            leaves.add(nearby);
                            if (leaves.size() > config.maxLeaves()) return new TreeParts(leaves, attachedBlocks);
                        } else if (directAttached) {
                            attachedBlocks.add(nearby);
                        }
                    }
                }
            }
        }
        return new TreeParts(leaves, attachedBlocks);
    }

    private boolean isCloseEnoughToDetectedTree(Block block, Set<Block> logs, Set<Block> detectedLeaves, int radius) {
        return isCloseEnoughToAny(block, logs, radius) || isCloseEnoughToAny(block, detectedLeaves, radius);
    }

    private boolean isCloseEnoughToAny(Block block, Set<Block> candidates, int radius) {
        int radiusSquared = radius * radius;
        for (Block candidate : candidates) {
            int dx = block.getX() - candidate.getX();
            int dy = block.getY() - candidate.getY();
            int dz = block.getZ() - candidate.getZ();
            if ((dx * dx) + (dy * dy) + (dz * dz) <= radiusSquared) return true;
        }
        return false;
    }

    private record TreeParts(Set<Block> leaves, Set<Block> attachedBlocks) {}
}
