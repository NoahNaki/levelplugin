package me.nakilex.levelplugin.dungeon;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.util.*;

/**
 * Represents a cuboid room template loaded from the flatland world.
 * Stores block data relative to the template origin as well as
 * redstone block connector positions.
 */
public class RoomTemplate {
    public static class BlockDef {
        public final int x, y, z;
        public final BlockData data;
        public BlockDef(int x, int y, int z, BlockData data) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.data = data;
        }
    }

    public static class Connector {
        public final int x, y, z;
        public final Direction facing;
        public Connector(int x, int y, int z, Direction facing) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.facing = facing;
        }
    }

    private final List<BlockDef> blocks;
    private final List<Connector> connectors;
    private final int width, height, depth;
    private final int minY;
    private final double centerX, centerZ;

    public RoomTemplate(List<BlockDef> blocks, List<Connector> connectors,
                        int width, int height, int depth, int minY) {
        this.blocks = blocks;
        this.connectors = connectors;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.minY = minY;
        this.centerX = (width - 1) / 2.0;
        this.centerZ = (depth - 1) / 2.0;
    }

    public List<BlockDef> getBlocks() { return blocks; }
    public List<Connector> getConnectors() { return connectors; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getDepth() { return depth; }
    public int getMinY() { return minY; }
    public double getCenterX() { return centerX; }
    public double getCenterZ() { return centerZ; }

    /**
     * Rotate a 2D X/Z vector around the template center.
     */
    public static int[] rotate(int x, int z, int rotation) {
        return switch (rotation & 3) {
            case 0 -> new int[]{x, z};
            case 1 -> new int[]{-z, x};
            case 2 -> new int[]{-x, -z};
            default -> new int[]{z, -x};
        };
    }

    /**
     * Load a template from the given cuboid region in the world.
     */
    public static RoomTemplate capture(World world,
                                       int x1, int y1, int z1,
                                       int x2, int y2, int z2) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int depth = maxZ - minZ + 1;

        List<BlockDef> blocks = new ArrayList<>();
        Set<Location> markerBlocks = new HashSet<>();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new Location(world, x, y, z);
                    BlockData data = loc.getBlock().getBlockData();
                    if (data.getMaterial() != Material.AIR) {
                        blocks.add(new BlockDef(x - minX, y - minY, z - minZ, data));
                        if (data.getMaterial() == Material.REDSTONE_BLOCK) {
                            markerBlocks.add(loc);
                        }
                    }
                }
            }
        }

        // group connectors by contiguous marker blocks
        List<Connector> connectors = new ArrayList<>();
        Set<Location> visited = new HashSet<>();
        for (Location loc : markerBlocks) {
            if (visited.contains(loc)) continue;
            List<Location> group = new ArrayList<>();
            Deque<Location> stack = new ArrayDeque<>();
            stack.push(loc);
            visited.add(loc);
            while (!stack.isEmpty()) {
                Location l = stack.pop();
                group.add(l);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) continue;
                            Location n = l.clone().add(dx, dy, dz);
                            if (markerBlocks.contains(n) && visited.add(n)) {
                                stack.push(n);
                            }
                        }
                    }
                }
            }
            // compute center of group
            double sx = 0, sy = 0, sz = 0;
            for (Location l : group) {
                sx += l.getBlockX();
                sy += l.getBlockY();
                sz += l.getBlockZ();
            }
            int cx = (int)Math.round(sx / group.size()) - minX;
            int cy = (int)Math.round(sy / group.size()) - minY;
            int cz = (int)Math.round(sz / group.size()) - minZ;
            int dx = cx - (int)Math.round((width - 1) / 2.0);
            int dz = cz - (int)Math.round((depth - 1) / 2.0);
            Direction dir = Direction.fromDelta(dx, dz);
            connectors.add(new Connector(cx, cy, cz, dir));
        }

        return new RoomTemplate(blocks, connectors, width, height, depth, minY);
    }

    /**
     * Get the set of connector directions for a given rotation.
     */
    public java.util.Set<Direction> getRotatedDirections(int rotation) {
        java.util.Set<Direction> set = new java.util.HashSet<>();
        for (Connector c : connectors) {
            Direction dir = rotate(c.facing, rotation);
            set.add(dir);
        }
        return set;
    }

    private static Direction rotate(Direction dir, int rotation) {
        int ord = (dir.ordinal() + rotation) & 3;
        return Direction.values()[ord];
    }
}
