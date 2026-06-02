package me.nakilex.levelplugin.player.fishing.minigame;

import org.bukkit.entity.Player;

/** A single interactive fishing challenge. Instances are created per bite. */
public interface FishingMiniGame {
    void start();
    void handleClick();
    default boolean usesRightClickInput() { return false; }
    default void handleRightClick() { }
    void handleSneak(boolean sneaking);
    void handleMovement(Movement movement);
    void cancel();
    default void cancelSilently() { cancel(); }
    boolean isFinished();

    enum Movement { LEFT, RIGHT, JUMP }
}
