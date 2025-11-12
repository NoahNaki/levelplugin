package me.nakilex.levelplugin.dungeon;

/**
 * Observer for dungeon run outcomes. Managers that need to react to dungeon
 * completions (e.g. Frontier Rifts or Arcane Trials) can register with the
 * {@link DungeonManager} instead of duplicating completion detection logic.
 */
public interface DungeonRunObserver {

    /**
     * Called when a dungeon instance is completed by the participants.
     *
     * @param result summary information about the cleared run
     */
    void onDungeonCompleted(DungeonRunResult result);
}

