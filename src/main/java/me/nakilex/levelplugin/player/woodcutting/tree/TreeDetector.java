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
        Set<Block> visitedParts = new HashSet<>();
        Queue<Block> queue = new ArrayDeque<>();

        int seedRadius = config.leafSeedRadius();
        int connectivityRadius = config.leafConnectivityRadius();
        config.debugLog("[Woodcutting] Leaf seed radius=" + seedRadius
                + " connectivity radius=" + connectivityRadius
                + " box expansion=" + config.leafBoxExpansion());

        for (Block log : logs) {
            seedTreePartsAroundLog(log, seedRadius, box, type, leaves, attachedBlocks, visitedParts, queue);
            if (leaves.size() > config.maxLeaves()) return filterTreeParts(leaves, attachedBlocks, logs, seedRadius);
        }

        expandConnectedCanopy(box, type, leaves, attachedBlocks, visitedParts, queue, connectivityRadius);
        return filterTreeParts(leaves, attachedBlocks, logs, seedRadius);
    }

    private void seedTreePartsAroundLog(Block log, int radius, TreeBox box, TreeType type, Set<Block> leaves,
                                        Set<Block> attachedBlocks, Set<Block> visitedParts, Queue<Block> queue) {
        forEachBlockInRadius(log, radius, candidate -> {
            if (!box.contains(candidate)) return;
            Material material = candidate.getType();
            if (matchesLeaf(type, material)) {
                if (!visitedParts.add(candidate)) return;
                leaves.add(candidate);
                queue.add(candidate);
            } else if (type.isAttachedNaturalBlock(material)) {
                if (!visitedParts.add(candidate)) return;
                attachedBlocks.add(candidate);
                queue.add(candidate);
            }
        });
    }

    private void expandConnectedCanopy(TreeBox box, TreeType type, Set<Block> leaves, Set<Block> attachedBlocks,
                                       Set<Block> visitedParts, Queue<Block> queue, int connectivityRadius) {
        while (!queue.isEmpty() && leaves.size() <= config.maxLeaves()) {
            Block current = queue.poll();
            forEachBlockInRadius(current, connectivityRadius, neighbor -> {
                if (!box.contains(neighbor)) return;
                Material material = neighbor.getType();
                if (matchesLeaf(type, material)) {
                    if (!visitedParts.add(neighbor)) return;
                    leaves.add(neighbor);
                    queue.add(neighbor);
                } else if (type.isAttachedNaturalBlock(material)) {
                    if (!visitedParts.add(neighbor)) return;
                    attachedBlocks.add(neighbor);
                    queue.add(neighbor);
                }
            });
        }
    }

    private TreeParts filterTreeParts(Set<Block> leaves, Set<Block> attachedBlocks, Set<Block> logs, int seedRadius) {
        ComponentFilterResult componentResult = keepAnchoredLeafComponents(leaves, logs, seedRadius);
        Set<Block> keptAttachedBlocks = keepAttachedBlocksConnectedToTree(attachedBlocks, logs, componentResult.keptLeaves());
        if (componentResult.rejectedComponents() > 0) {
            config.debugLog("[Woodcutting] Leaf components found=" + componentResult.totalComponents()
                    + " kept=" + componentResult.keptComponents()
                    + " rejected=" + componentResult.rejectedComponents());
            for (RejectedComponent rejected : componentResult.rejected()) {
                config.debugLog("[Woodcutting] Rejected detached leaf component size=" + rejected.size()
                        + " nearestLogDistance=" + rejected.nearestLogDistance());
            }
        } else {
            config.debugLog("[Woodcutting] Leaf components found=" + componentResult.totalComponents()
                    + " kept=" + componentResult.keptComponents() + " rejected=0");
        }
        return new TreeParts(componentResult.keptLeaves(), keptAttachedBlocks);
    }

    private ComponentFilterResult keepAnchoredLeafComponents(Set<Block> leaves, Set<Block> logs, int seedRadius) {
        Set<Block> unvisited = new HashSet<>(leaves);
        Set<Block> keptLeaves = new LinkedHashSet<>();
        java.util.List<RejectedComponent> rejected = new java.util.ArrayList<>();
        int total = 0;
        int kept = 0;

        while (!unvisited.isEmpty()) {
            Block first = unvisited.iterator().next();
            Set<Block> component = collectLeafComponent(first, unvisited);
            total++;
            if (componentTouchesDetectedLogs(component, logs, seedRadius)) {
                kept++;
                keptLeaves.addAll(component);
            } else {
                rejected.add(new RejectedComponent(component.size(), nearestLogDistance(component, logs)));
            }
        }

        return new ComponentFilterResult(keptLeaves, total, kept, rejected);
    }

    private Set<Block> collectLeafComponent(Block first, Set<Block> unvisited) {
        Set<Block> component = new LinkedHashSet<>();
        Queue<Block> queue = new ArrayDeque<>();
        queue.add(first);
        unvisited.remove(first);
        while (!queue.isEmpty()) {
            Block current = queue.poll();
            component.add(current);
            forEachBlockInRadius(current, 1, neighbor -> {
                if (unvisited.remove(neighbor)) queue.add(neighbor);
            });
        }
        return component;
    }

    private boolean componentTouchesDetectedLogs(Set<Block> component, Set<Block> logs, int seedRadius) {
        int radiusSquared = seedRadius * seedRadius;
        for (Block leaf : component) {
            for (Block log : logs) {
                int dx = leaf.getX() - log.getX();
                int dy = leaf.getY() - log.getY();
                int dz = leaf.getZ() - log.getZ();
                int distanceSquared = (dx * dx) + (dy * dy) + (dz * dz);
                if (distanceSquared <= radiusSquared || (Math.abs(dx) <= 1 && Math.abs(dy) <= 1 && Math.abs(dz) <= 1)) return true;
            }
        }
        return false;
    }

    private Set<Block> keepAttachedBlocksConnectedToTree(Set<Block> attachedBlocks, Set<Block> logs, Set<Block> leaves) {
        Set<Block> kept = new LinkedHashSet<>();
        for (Block attached : attachedBlocks) {
            if (touchesAny(attached, logs) || touchesAny(attached, leaves)) kept.add(attached);
        }
        return kept;
    }

    private boolean touchesAny(Block block, Set<Block> candidates) {
        for (Block candidate : candidates) {
            if (Math.abs(block.getX() - candidate.getX()) <= 1
                    && Math.abs(block.getY() - candidate.getY()) <= 1
                    && Math.abs(block.getZ() - candidate.getZ()) <= 1) return true;
        }
        return false;
    }

    private int nearestLogDistance(Set<Block> component, Set<Block> logs) {
        double nearestSquared = Double.MAX_VALUE;
        for (Block leaf : component) {
            for (Block log : logs) {
                int dx = leaf.getX() - log.getX();
                int dy = leaf.getY() - log.getY();
                int dz = leaf.getZ() - log.getZ();
                nearestSquared = Math.min(nearestSquared, (dx * dx) + (dy * dy) + (dz * dz));
            }
        }
        return nearestSquared == Double.MAX_VALUE ? -1 : (int) Math.ceil(Math.sqrt(nearestSquared));
    }

    private void forEachBlockInRadius(Block center, int radius, java.util.function.Consumer<Block> consumer) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    consumer.accept(center.getRelative(dx, dy, dz));
                }
            }
        }
    }

    private boolean matchesLeaf(TreeType type, Material material) {
        return type.isLeaf(material) || (config.allowMixedLeaves() && treeTypeRegistry.isAnyConfiguredLeaf(material));
    }

    private record ComponentFilterResult(Set<Block> keptLeaves, int totalComponents, int keptComponents,
                                         java.util.List<RejectedComponent> rejected) {
        int rejectedComponents() { return rejected.size(); }
    }

    private record RejectedComponent(int size, int nearestLogDistance) {}

    private record TreeParts(Set<Block> leaves, Set<Block> attachedBlocks) {}
}
