package me.nakilex.levelplugin.dungeon;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

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

    public Dungeon(World world, String name) {
        this.world = world;
        this.name = name;
    }

    public String getName() { return name; }

    public void addRoom(RoomInstance inst) {
        rooms.add(inst);
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
            }
        }
        rooms.clear();
    }
}
