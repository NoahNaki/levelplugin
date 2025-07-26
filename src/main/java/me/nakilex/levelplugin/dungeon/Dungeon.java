package me.nakilex.levelplugin.dungeon;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a generated dungeon instance consisting of placed rooms.
 */
public class Dungeon {
    public static class RoomInstance {
        public final RoomTemplate template;
        public final int rotation;
        public final Location center;
        public RoomInstance(RoomTemplate template, int rotation, Location center) {
            this.template = template;
            this.rotation = rotation;
            this.center = center;
        }
    }

    private final World world;
    private final String name;
    private final List<RoomInstance> rooms = new ArrayList<>();
    private final Set<BlockPos> occupied = new HashSet<>();
    private final Set<BlockPos> connectorOccupied = new HashSet<>();

    private record BlockPos(int x, int y, int z) {}

    public Dungeon(World world, String name) {
        this.world = world;
        this.name = name;
    }

    public String getName() { return name; }

    public void addRoom(RoomInstance inst) {
        rooms.add(inst);
        for (RoomTemplate.BlockDef b : inst.template.getBlocks()) {
            if (b.data.getMaterial() == org.bukkit.Material.PINK_WOOL ||
                b.data.getMaterial() == org.bukkit.Material.REDSTONE_BLOCK) continue;
            int[] vec = RoomTemplate.rotate(b.x - (int)Math.round(inst.template.getCenterX()),
                    b.z - (int)Math.round(inst.template.getCenterZ()), inst.rotation);
            int wx = inst.center.getBlockX() + vec[0];
            int wy = inst.center.getBlockY() + (b.y - inst.template.getConnectorMinY());
            int wz = inst.center.getBlockZ() + vec[1];
            if (inst.template.isConnectorBlock(b.x, b.y, b.z)) {
                connectorOccupied.add(new BlockPos(wx, wy, wz));
            } else {
                occupied.add(new BlockPos(wx, wy, wz));
            }
        }
    }

    /** Remove all placed blocks for this dungeon. */
    public void delete() {
        for (RoomInstance r : rooms) {
            for (RoomTemplate.BlockDef b : r.template.getBlocks()) {
                int[] vec = RoomTemplate.rotate(b.x - (int)Math.round(r.template.getCenterX()),
                        b.z - (int)Math.round(r.template.getCenterZ()), r.rotation);
                int wx = r.center.getBlockX() + vec[0];
                int wy = r.center.getBlockY() + (b.y - r.template.getConnectorMinY());
                int wz = r.center.getBlockZ() + vec[1];
                world.getBlockAt(wx, wy, wz).setType(Material.AIR, false);
                BlockPos pos = new BlockPos(wx, wy, wz);
                occupied.remove(pos);
                connectorOccupied.remove(pos);
            }
        }
        rooms.clear();
    }

    /** Remove the most recently placed room. */
    public boolean removeLastRoom() {
        if (rooms.isEmpty()) return false;
        RoomInstance r = rooms.remove(rooms.size() - 1);
        for (RoomTemplate.BlockDef b : r.template.getBlocks()) {
            int[] vec = RoomTemplate.rotate(b.x - (int)Math.round(r.template.getCenterX()),
                    b.z - (int)Math.round(r.template.getCenterZ()), r.rotation);
            int wx = r.center.getBlockX() + vec[0];
            int wy = r.center.getBlockY() + (b.y - r.template.getConnectorMinY());
            int wz = r.center.getBlockZ() + vec[1];
            world.getBlockAt(wx, wy, wz).setType(Material.AIR, false);
            BlockPos pos = new BlockPos(wx, wy, wz);
            occupied.remove(pos);
            connectorOccupied.remove(pos);
        }
        return true;
    }

    public boolean isOccupied(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return occupied.contains(pos) || connectorOccupied.contains(pos);
    }

    public boolean isConnectorOccupied(int x, int y, int z) {
        return connectorOccupied.contains(new BlockPos(x, y, z));
    }
}
