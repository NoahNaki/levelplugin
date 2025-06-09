package me.nakilex.levelplugin.fakeblock;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.List;

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

    /** Whether the gate is currently closed. When closed players will see the
     * fake blocks and be prevented from entering. */
    private boolean closed;

    public QuestGate(String id, Location pos1, Location pos2, BlockData closedData, boolean closed) {
        this.id = id;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.closedData = closedData;
        this.closed = closed;
        precomputeBlocks();
    }

    private void precomputeBlocks() {
        World world = pos1.getWorld();
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    blocks.add(new Location(world, x, y, z));
                }
            }
        }
    }

    public String getId() {
        return id;
    }

    public BlockData getClosedData() {
        return closedData;
    }

    public Location getPos1() { return pos1; }

    public Location getPos2() { return pos2; }

    public List<Location> getBlocks() { return blocks; }

    public boolean isClosed() { return closed; }

    public void setClosed(boolean closed) { this.closed = closed; }

    public boolean isInside(Location loc) {
        if (!loc.getWorld().equals(pos1.getWorld())) return false;
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }
}
