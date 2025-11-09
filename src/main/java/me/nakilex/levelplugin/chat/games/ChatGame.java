package me.nakilex.levelplugin.chat.games;

import java.util.Optional;

import org.bukkit.entity.Player;

/**
 * Generic contract for chat mini-games that react to player messages.
 */
public interface ChatGame {

    /** Unique identifier used for debug toggles. */
    String getId();

    /** Human readable name shown to players. */
    String getDisplayName();

    /** Whether the game is currently enabled for rotation. */
    boolean isEnabled();

    /** Toggle the game on or off for runtime debugging. */
    void setEnabled(boolean enabled);

    /** Whether the game is presently running an active round. */
    boolean isRunning();

    /** Whether sufficient data exists to play this game. */
    boolean canPlay();

    /** Start a new round of the game. */
    void start(ChatGameManager manager);

    /** Forcefully stop the current round. */
    void stop(ChatGameManager manager);

    /**
     * Inspect a chat message for a potential win condition.
     *
     * @return empty if the message is irrelevant, otherwise a completed result
     */
    Optional<ChatGameResult> handleChat(Player player, String message);
}
