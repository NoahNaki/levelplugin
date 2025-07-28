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
        public final int minX, minY, minZ, maxX, maxY, maxZ;
        public final String mob;
        public RoomInstance(RoomTemplate template, int rotation, Location center,
                            int minX, int minY, int minZ,
                            int maxX, int maxY, int maxZ,
                            String mob) {
            this.template = template;
            this.rotation = rotation;
            this.center = center;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.mob = mob;
        }

        public boolean contains(Location loc) {
            if (!loc.getWorld().equals(center.getWorld())) return false;
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();
            return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
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

    /** Remove a room that was previously added. */
    public void removeRoom(RoomInstance inst) {
        rooms.remove(inst);
    }

    public List<RoomInstance> getRooms() {
        return rooms;
    }

    /**
     * Find the room instance containing the given location.
     * @param loc world location to check
     * @return matching RoomInstance or null if none
     */
    public RoomInstance getRoomContaining(Location loc) {
        for (RoomInstance r : rooms) {
            if (r.contains(loc)) return r;
        }
        return null;
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
            for (RoomTemplate.Marker m : r.template.getPortals()) {
                int[] vec = RoomTemplate.rotate(m.x - (int)Math.round(r.template.getCenterX()),
                        m.z - (int)Math.round(r.template.getCenterZ()), r.rotation);
                int wx = r.center.getBlockX() + vec[0];
                int wy = r.center.getBlockY() + (m.y - r.template.getConnectorMinY());
                int wz = r.center.getBlockZ() + vec[1];
                world.getBlockAt(wx, wy, wz).setType(Material.AIR, false);
            }
            for (RoomTemplate.Marker m : r.template.getExitMarkers()) {
                int[] vec = RoomTemplate.rotate(m.x - (int)Math.round(r.template.getCenterX()),
                        m.z - (int)Math.round(r.template.getCenterZ()), r.rotation);
                Location loc = r.center.clone().add(vec[0], m.y - r.template.getConnectorMinY(), vec[1]);
                for (var ent : world.getNearbyEntities(loc, 1.5, 2.5, 1.5)) {
                    if (ent.getScoreboardTags().contains("dungeon_exit")) ent.remove();
                }
            }
        }
        rooms.clear();
    }
}
