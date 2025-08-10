package me.nakilex.levelplugin.dungeon;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.Skull;
import com.destroystokyo.paper.profile.PlayerProfile;

import java.util.*;

/**
 * Represents a cuboid room template loaded from the flatland world.
 * Stores block data relative to the template origin as well as
 * connector positions marked with pink wool or redstone blocks. If a lime
 * wool block sits on top of a connector marker the connector is treated as
 * the preferred entrance.
 */
public class RoomTemplate {
    public static class BlockDef {
        public final int x, y, z;
        public final BlockData data;
        public final PlayerProfile profile;
        public BlockDef(int x, int y, int z, BlockData data, PlayerProfile profile) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.data = data;
            this.profile = profile;
        }
    }

    public static class Connector {
        /** X/Z center of the connector and lowest Y of the marker stack */
        public final int x, z, bottomY;
        public final Direction facing;
        public final boolean entrance;
        public Connector(int x, int z, int bottomY, Direction facing, boolean entrance) {
            this.x = x;
            this.z = z;
            this.bottomY = bottomY;
            this.facing = facing;
            this.entrance = entrance;
        }
    }

    /** Simple coordinate used for portal and hologram markers. */
    public static class Marker {
        public final int x, y, z;
        public Marker(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /** Marker used for loot chest placeholders. */
    public static class ChestMarker {
        public final int x, y, z;
        public final BlockData data;
        public ChestMarker(int x, int y, int z, BlockData data) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.data = data;
        }
    }

    private final List<BlockDef> blocks;
    private final List<Connector> connectors;
    private final List<Marker> portals;
    private final List<Marker> exits;
    private final Marker bossSpawn;
    private final List<ChestMarker> chests;
    private final int width, height, depth;
    private final int minY;
    private final int connectorMinY;
    private final double centerX, centerZ;

    public RoomTemplate(List<BlockDef> blocks, List<Connector> connectors,
                        List<Marker> portals, List<Marker> exits, List<ChestMarker> chests,
                        Marker bossSpawn, int width, int height, int depth, int minY) {
        this.blocks = blocks;
        this.connectors = connectors;
        this.portals = portals;
        this.exits = exits;
        this.chests = chests;
        this.bossSpawn = bossSpawn;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.minY = minY;
        int lowest = Integer.MAX_VALUE;
        for (Connector c : connectors) lowest = Math.min(lowest, c.bottomY);
        this.connectorMinY = lowest == Integer.MAX_VALUE ? 0 : lowest;
        this.centerX = (width - 1) / 2.0;
        this.centerZ = (depth - 1) / 2.0;
    }

    public List<BlockDef> getBlocks() { return blocks; }
    public List<Connector> getConnectors() { return connectors; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getDepth() { return depth; }
    public int getMinY() { return minY; }
    public int getConnectorMinY() { return connectorMinY; }
    public double getCenterX() { return centerX; }
    public double getCenterZ() { return centerZ; }
    public List<Marker> getPortals() { return portals; }
    public List<Marker> getExitMarkers() { return exits; }
    public Marker getBossSpawn() { return bossSpawn; }
    public List<ChestMarker> getChests() { return chests; }

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
                                       int x2, int y2, int z2,
                                       boolean recordExit) {
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
        Set<Location> limeBlocks = new HashSet<>();
        List<Marker> portalMarks = new ArrayList<>();
        List<Marker> exitMarks = new ArrayList<>();
        Marker bossMark = null;
        List<ChestMarker> chestMarks = new ArrayList<>();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new Location(world, x, y, z);
                    BlockData data = loc.getBlock().getBlockData();
                    if (data.getMaterial() != Material.AIR) {
                        PlayerProfile profile = null;
                        var state = loc.getBlock().getState();
                        if (state instanceof Skull skull) {
                            profile = skull.getPlayerProfile();
                        }
                        Material mat = data.getMaterial();
                        if (mat == Material.MAGENTA_WOOL) {
                            portalMarks.add(new Marker(x - minX, y - minY, z - minZ));
                        } else if (mat == Material.RED_WOOL && recordExit) {
                            exitMarks.add(new Marker(x - minX, y - minY, z - minZ));
                        } else if (mat == Material.BLACK_WOOL) {
                            bossMark = new Marker(x - minX, y - minY, z - minZ);
                        } else if (mat == Material.CHEST || mat == Material.TRAPPED_CHEST) {
                            chestMarks.add(new ChestMarker(x - minX, y - minY, z - minZ, data));
                        } else {
                            blocks.add(new BlockDef(x - minX, y - minY, z - minZ, data, profile));
                            if (mat == Material.REDSTONE_BLOCK || mat == Material.PINK_WOOL) {
                                markerBlocks.add(loc);
                            } else if (mat == Material.LIME_WOOL) {
                                limeBlocks.add(loc);
                            }
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
            int minGroupY = Integer.MAX_VALUE;
            for (Location l : group) {
                sx += l.getBlockX();
                sy += l.getBlockY();
                sz += l.getBlockZ();
                if (l.getBlockY() < minGroupY) minGroupY = l.getBlockY();
            }
            int cx = (int)Math.round(sx / group.size()) - minX;
            int cz = (int)Math.round(sz / group.size()) - minZ;
            int dx = cx - (int)Math.round((width - 1) / 2.0);
            int dz = cz - (int)Math.round((depth - 1) / 2.0);
            Direction dir = Direction.fromDelta(dx, dz);
            boolean entrance = false;
            for (Location l : group) {
                Location above = l.clone().add(0, 1, 0);
                if (limeBlocks.contains(above)) { entrance = true; break; }
            }
            connectors.add(new Connector(cx, cz, minGroupY - minY, dir, entrance));
        }

        return new RoomTemplate(blocks, connectors, portalMarks, exitMarks, chestMarks, bossMark, width, height, depth, minY);
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

    /** Rotate block data for directional blocks to match rotation. */
    public static BlockData rotateBlockData(BlockData data, int rotation) {
        if (rotation % 4 == 0) return data;
        BlockData copy = data.clone();
        if (copy instanceof org.bukkit.block.data.Directional dir) {
            org.bukkit.block.BlockFace face = dir.getFacing();
            dir.setFacing(rotateFace(face, rotation));
            return copy;
        }
        if (copy instanceof org.bukkit.block.data.Rotatable rot) {
            org.bukkit.block.BlockFace face = rot.getRotation();
            rot.setRotation(rotateFace(face, rotation));
            return copy;
        }
        if (copy instanceof org.bukkit.block.data.Orientable orient) {
            org.bukkit.Axis axis = orient.getAxis();
            switch (axis) {
                case X -> orient.setAxis(rotation % 2 == 0 ? org.bukkit.Axis.X : org.bukkit.Axis.Z);
                case Z -> orient.setAxis(rotation % 2 == 0 ? org.bukkit.Axis.Z : org.bukkit.Axis.X);
                default -> {}
            }
            return copy;
        }
        if (copy instanceof org.bukkit.block.data.MultipleFacing multi) {
            boolean n = multi.hasFace(org.bukkit.block.BlockFace.NORTH);
            boolean e = multi.hasFace(org.bukkit.block.BlockFace.EAST);
            boolean s = multi.hasFace(org.bukkit.block.BlockFace.SOUTH);
            boolean w = multi.hasFace(org.bukkit.block.BlockFace.WEST);
            multi.setFace(org.bukkit.block.BlockFace.NORTH, false);
            multi.setFace(org.bukkit.block.BlockFace.EAST, false);
            multi.setFace(org.bukkit.block.BlockFace.SOUTH, false);
            multi.setFace(org.bukkit.block.BlockFace.WEST, false);
            org.bukkit.block.BlockFace[] order = {
                    org.bukkit.block.BlockFace.NORTH,
                    org.bukkit.block.BlockFace.EAST,
                    org.bukkit.block.BlockFace.SOUTH,
                    org.bukkit.block.BlockFace.WEST
            };
            boolean[] faces = {n, e, s, w};
            for (int i = 0; i < 4; i++) {
                if (faces[i]) {
                    multi.setFace(order[(i + rotation) & 3], true);
                }
            }
            return copy;
        }
        if (copy instanceof org.bukkit.block.data.type.Wall wall) {
            org.bukkit.block.BlockFace[] order = {
                    org.bukkit.block.BlockFace.NORTH,
                    org.bukkit.block.BlockFace.EAST,
                    org.bukkit.block.BlockFace.SOUTH,
                    org.bukkit.block.BlockFace.WEST
            };
            org.bukkit.block.data.type.Wall.Height n = wall.getHeight(org.bukkit.block.BlockFace.NORTH);
            org.bukkit.block.data.type.Wall.Height e = wall.getHeight(org.bukkit.block.BlockFace.EAST);
            org.bukkit.block.data.type.Wall.Height s = wall.getHeight(org.bukkit.block.BlockFace.SOUTH);
            org.bukkit.block.data.type.Wall.Height w = wall.getHeight(org.bukkit.block.BlockFace.WEST);
            org.bukkit.block.data.type.Wall.Height[] heights = {n, e, s, w};
            for (org.bukkit.block.BlockFace f : order) {
                wall.setHeight(f, org.bukkit.block.data.type.Wall.Height.NONE);
            }
            for (int i = 0; i < 4; i++) {
                wall.setHeight(order[(i + rotation) & 3], heights[i]);
            }
            return copy;
        }
        return copy;
    }

    private static org.bukkit.block.BlockFace rotateFace(org.bukkit.block.BlockFace face, int rotation) {
        org.bukkit.block.BlockFace[] order = {
                org.bukkit.block.BlockFace.NORTH,
                org.bukkit.block.BlockFace.EAST,
                org.bukkit.block.BlockFace.SOUTH,
                org.bukkit.block.BlockFace.WEST
        };
        int idx = java.util.Arrays.asList(order).indexOf(face);
        if (idx < 0) return face;
        return order[(idx + rotation) & 3];
    }
}
