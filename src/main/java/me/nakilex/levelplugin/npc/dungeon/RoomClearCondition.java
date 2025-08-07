package me.nakilex.levelplugin.npc.dungeon;

/**
 * Determines when a dungeon room has been cleared.
 */
@FunctionalInterface
public interface RoomClearCondition {
    /**
     * @return true if the room is cleared and the NPC should proceed
     */
    boolean isMet();
}
