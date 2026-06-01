package me.nakilex.levelplugin.player.fishing.minigame;

import org.bukkit.entity.Player;

/** A single interactive fishing challenge. Instances are created per bite. */
public interface FishingMiniGame {
    void start();
    void handleClick();
    void handleSneak(boolean sneaking);
    void handleMovement(Movement movement);
    void cancel();
    boolean isFinished();

    enum Movement { LEFT, RIGHT, JUMP }
}
