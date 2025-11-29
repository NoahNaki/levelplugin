package me.nakilex.levelplugin.dungeon;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;

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
        public final java.util.List<Chest> chests;
        public final Location bossSpawn;
        public RoomInstance(RoomTemplate template, int rotation, Location center,
                            int minX, int minY, int minZ,
                            int maxX, int maxY, int maxZ,
                            String mob,
                            java.util.List<Chest> chests,
                            Location bossSpawn) {
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
            this.chests = chests == null ? java.util.List.of() : chests;
            this.bossSpawn = bossSpawn;
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

    /** Location and facing for a loot chest spawn point. */
    public static record Chest(Location loc, BlockFace facing) {}

    private final World world;
    private final String name;
    private final List<RoomInstance> rooms = new ArrayList<>();
    private boolean bossDefeated = false;

    public Dungeon(World world, String name) {
        this.world = world;
        this.name = name;
    }

    public String getName() { return name; }

    /** Return the world this dungeon resides in. */
    public World getWorld() { return world; }

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

    public boolean isBossDefeated() { return bossDefeated; }
    public void setBossDefeated(boolean defeated) { this.bossDefeated = defeated; }

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
        for (RoomInstance r : new ArrayList<>(rooms)) {
            deleteRoom(r);
        }
    }

    /** Remove a specific room from the dungeon and clear its blocks. */
    public void deleteRoom(RoomInstance r) {
        clearRoomBlocks(r);
        rooms.remove(r);
    }

    private void clearRoomBlocks(RoomInstance r) {
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
}
