package me.nakilex.levelplugin.npc.dungeon;

import me.nakilex.levelplugin.dungeon.Dungeon;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;

/**
 * Room completion condition based on scoreboard tags. A room is
 * considered cleared when no living entities within the room's bounds
 * have the given tag.
 */
public class TagClearCondition implements RoomClearCondition {

    private final Dungeon.RoomInstance room;
    private final String tag;

    public TagClearCondition(Dungeon.RoomInstance room, String tag) {
        this.room = room;
        this.tag = tag;
    }

    @Override
    public boolean isMet() {
        World world = room.center.getWorld();
        BoundingBox box = new BoundingBox(room.minX, room.minY, room.minZ,
                room.maxX + 1, room.maxY + 1, room.maxZ + 1);
        for (Entity e : world.getNearbyEntities(box)) {
            if (e instanceof LivingEntity && e.getScoreboardTags().contains(tag)) {
                return false;
            }
        }
        return true;
    }
}
