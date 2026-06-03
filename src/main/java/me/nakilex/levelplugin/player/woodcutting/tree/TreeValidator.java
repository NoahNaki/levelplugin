package me.nakilex.levelplugin.player.woodcutting.tree;

import me.nakilex.levelplugin.player.woodcutting.WoodcuttingConfig;
import me.nakilex.levelplugin.player.woodcutting.protection.PlacedBlockTracker;
import org.bukkit.block.Block;

import java.util.Set;

public class TreeValidator {
    private final WoodcuttingConfig config;
    private final PlacedBlockTracker placedBlockTracker;

    public TreeValidator(WoodcuttingConfig config, PlacedBlockTracker placedBlockTracker) {
        this.config = config;
        this.placedBlockTracker = placedBlockTracker;
    }

    public boolean looksNatural(Block root, Set<Block> logs, Set<Block> leaves, TreeType type) {
        return validationFailure(root, logs, leaves, type) == null;
    }

    public TreeDetectionInvalidReason validationFailure(Block root, Set<Block> logs, Set<Block> leaves, TreeType type) {
        if (!hasVerticalTrunk(root, logs, type)) return TreeDetectionInvalidReason.FAILED_NATURAL_VALIDATION;
        if (!hasLeavesNearTop(logs, leaves)) return TreeDetectionInvalidReason.FAILED_NATURAL_VALIDATION;
        if (looksLikeFlatWall(logs)) return TreeDetectionInvalidReason.FAILED_NATURAL_VALIDATION;
        if (looksLikeArtificialCube(logs)) return TreeDetectionInvalidReason.FAILED_NATURAL_VALIDATION;
        if (!hasReasonableLeafRatio(logs, leaves)) return TreeDetectionInvalidReason.FAILED_NATURAL_VALIDATION;
        if (config.ignorePlayerPlacedWood() && placedBlockTracker.tooManyPlaced(logs)) return TreeDetectionInvalidReason.PLAYER_PLACED_REJECTED;
        if (config.ignorePlayerPlacedLeaves() && placedBlockTracker.tooManyPlaced(leaves)) return TreeDetectionInvalidReason.PLAYER_PLACED_REJECTED;
        return null;
    }

    public boolean isLargeTree(Block root, Set<Block> logs, TreeType type) { return type.canBeLargeTree() && hasTwoByTwoTrunk(root, logs); }

    private boolean hasVerticalTrunk(Block root, Set<Block> logs, TreeType type) {
        int straightLogs = 0;
        for (int y = 0; y < type.heuristic().height(); y++) {
            if (logs.contains(root.getRelative(0, y, 0))) straightLogs++; else break;
        }
        return straightLogs >= 3 || hasTwoByTwoTrunk(root, logs);
    }

    private boolean hasTwoByTwoTrunk(Block root, Set<Block> logs) {
        int[][] offsets = {{0,0}, {-1,0}, {0,-1}, {-1,-1}};
        for (int[] base : offsets) {
            int levels = 0;
            for (int y = 0; y < 4; y++) {
                boolean complete = logs.contains(root.getRelative(base[0], y, base[1]))
                        && logs.contains(root.getRelative(base[0] + 1, y, base[1]))
                        && logs.contains(root.getRelative(base[0], y, base[1] + 1))
                        && logs.contains(root.getRelative(base[0] + 1, y, base[1] + 1));
                if (complete) levels++; else break;
            }
            if (levels >= 2) return true;
        }
        return false;
    }

    private boolean hasLeavesNearTop(Set<Block> logs, Set<Block> leaves) {
        if (leaves.isEmpty()) return false;
        int minY = logs.stream().mapToInt(Block::getY).min().orElse(0);
        int maxY = logs.stream().mapToInt(Block::getY).max().orElse(0);
        int middleY = minY + ((maxY - minY) / 2);
        long upperLeaves = leaves.stream().filter(leaf -> leaf.getY() >= middleY).count();
        return upperLeaves >= Math.min(6, Math.max(1, leaves.size() / 2));
    }

    private boolean looksLikeFlatWall(Set<Block> logs) {
        Bounds bounds = Bounds.from(logs);
        return (bounds.widthX() >= 6 && bounds.widthZ() <= 1 && bounds.height() >= 3)
                || (bounds.widthZ() >= 6 && bounds.widthX() <= 1 && bounds.height() >= 3);
    }

    private boolean looksLikeArtificialCube(Set<Block> logs) {
        Bounds bounds = Bounds.from(logs);
        int volume = bounds.widthX() * bounds.height() * bounds.widthZ();
        double density = volume <= 0 ? 0.0D : logs.size() / (double) volume;
        return volume >= 27 && density > 0.65D;
    }

    private boolean hasReasonableLeafRatio(Set<Block> logs, Set<Block> leaves) {
        if (logs.isEmpty()) return false;
        double ratio = leaves.size() / (double) logs.size();
        return ratio >= 0.25D && ratio <= 25.0D;
    }

    private record Bounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        static Bounds from(Set<Block> blocks) {
            return new Bounds(blocks.stream().mapToInt(Block::getX).min().orElse(0), blocks.stream().mapToInt(Block::getX).max().orElse(0),
                    blocks.stream().mapToInt(Block::getY).min().orElse(0), blocks.stream().mapToInt(Block::getY).max().orElse(0),
                    blocks.stream().mapToInt(Block::getZ).min().orElse(0), blocks.stream().mapToInt(Block::getZ).max().orElse(0));
        }
        int widthX() { return maxX - minX + 1; }
        int widthZ() { return maxZ - minZ + 1; }
        int height() { return maxY - minY + 1; }
    }
}
