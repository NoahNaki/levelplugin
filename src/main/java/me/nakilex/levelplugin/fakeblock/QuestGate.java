package me.nakilex.levelplugin.fakeblock;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.type.Wall;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import me.nakilex.levelplugin.fakeblock.GateAnimation;

/**
 * Represents a region of blocks that should appear closed until a quest is
 * completed.
 */
public class QuestGate {
    /** Identifier for this gate. Originally intended to be a quest id but for
     * now it simply acts as a unique name. */
    private final String id;

    private final Location pos1;
    private final Location pos2;
    private final BlockData closedData;
    private final List<Location> blocks = new ArrayList<>();
    private final Map<Location, BlockData> blockDataMap = new HashMap<>();
    private final java.util.Set<Location> blockSet = new java.util.HashSet<>();
    private final boolean customBlocks;

    private final GateAnimation animation;
    private int minX, maxX, minY, maxY, minZ, maxZ;
    private long animationTicks;

    /** Whether the gate is closed by default for new players. */
    private boolean defaultClosed;

    /** Individual player gate states. True means the gate is closed for that
     * player. If a player is not present in the map, {@code defaultClosed} is
     * used. */
    private final Map<UUID, Boolean> playerStates = new HashMap<>();

    public QuestGate(String id, Location pos1, Location pos2, BlockData closedData, boolean closed, GateAnimation anim) {
        this(id, pos1, pos2, closedData, closed, anim, 40L);
    }

    public QuestGate(String id, Location pos1, Location pos2, BlockData closedData, boolean closed, GateAnimation anim, long ticks) {
        this.id = id;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.closedData = closedData;
        this.defaultClosed = closed;
        this.animation = anim == null ? GateAnimation.INSTANT : anim;
        this.animationTicks = ticks > 0 ? ticks : 40L;
        this.customBlocks = false;
        precomputeBlocks(null);
    }

    public QuestGate(String id, Location pos1, Location pos2, Map<Location, BlockData> closedMap, boolean closed, GateAnimation anim, long ticks) {
        this.id = id;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.closedData = org.bukkit.Material.AIR.createBlockData();
        this.defaultClosed = closed;
        this.animation = anim == null ? GateAnimation.INSTANT : anim;
        this.animationTicks = ticks > 0 ? ticks : 40L;
        this.customBlocks = true;
        precomputeBlocks(closedMap);
    }

    private void precomputeBlocks(Map<Location, BlockData> customMap) {
        World world = pos1.getWorld();
        minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new Location(world, x, y, z);
                    blocks.add(loc);
                    blockSet.add(loc);
                }
            }
        }

        if (customMap != null && !customMap.isEmpty()) {
            blockDataMap.putAll(customMap);
        } else {
            // compute connection-aware block data for connectable materials
            for (Location loc : blocks) {
                blockDataMap.put(loc, buildConnectedData(loc));
            }
        }
    }

    private BlockData buildConnectedData(Location loc) {
        BlockData data = closedData.clone();
        if (data instanceof MultipleFacing mf) {
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Location adj = loc.clone().add(face.getModX(), face.getModY(), face.getModZ());
                boolean connect = blockSet.contains(adj) || !adj.getBlock().getType().isAir();
                mf.setFace(face, connect);
            }
        } else if (data instanceof Wall wall) {
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Location adj = loc.clone().add(face.getModX(), face.getModY(), face.getModZ());
                boolean connect = blockSet.contains(adj) || !adj.getBlock().getType().isAir();
                wall.setHeight(face, connect ? Wall.Height.TALL : Wall.Height.NONE);
            }
            wall.setUp(true);
        }
        return data;
    }

    public String getId() {
        return id;
    }

    public BlockData getClosedData() { return closedData; }
    public BlockData getClosedData(Location loc) { return blockDataMap.getOrDefault(loc, closedData); }
    public Map<Location, BlockData> getClosedDataMap() { return blockDataMap; }
    public boolean hasCustomBlocks() { return customBlocks; }

    public GateAnimation getAnimation() { return animation; }
    public long getAnimationTicks() { return animationTicks; }
    public void setAnimationTicks(long ticks) { if (ticks > 0) this.animationTicks = ticks; }

    public int getMinY() { return minY; }
    public int getMaxY() { return maxY; }
    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }

    public Location getPos1() { return pos1; }

    public Location getPos2() { return pos2; }

    public List<Location> getBlocks() { return blocks; }

    /**
     * Returns the default closed state used for players that have no
     * personalised setting.
     */
    public boolean isDefaultClosed() { return defaultClosed; }

    public void setDefaultClosed(boolean closed) { this.defaultClosed = closed; }

    public boolean isClosed(UUID player) {
        return playerStates.getOrDefault(player, defaultClosed);
    }

    public void setClosed(UUID player, boolean closed) {
        playerStates.put(player, closed);
    }

    public void toggle(UUID player) {
        setClosed(player, !isClosed(player));
    }

    public boolean isInside(Location loc) {
        if (!loc.getWorld().equals(pos1.getWorld())) return false;
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }
}
