package me.nakilex.levelplugin.arena.match;

import me.nakilex.levelplugin.arena.instance.ArenaInstance;
import me.nakilex.levelplugin.arena.rating.ArenaRatingManager;

import java.util.Objects;
import java.util.UUID;

/**
 * Describes a live arena bout, tracking the participants, the world instance
 * backing the fight and their rating snapshots captured at the time the match
 * began.
 */
public final class ArenaMatch {
    public enum State { COUNTDOWN, ACTIVE, FINISHED }

    private final UUID playerOne;
    private final UUID playerTwo;
    private final ArenaInstance instance;
    private final ArenaRatingManager.RatingSnapshot ratingOne;
    private final ArenaRatingManager.RatingSnapshot ratingTwo;
    private final long createdAt;
    private State state = State.COUNTDOWN;
    private long activeAt;

    public ArenaMatch(UUID playerOne,
                      UUID playerTwo,
                      ArenaInstance instance,
                      ArenaRatingManager.RatingSnapshot ratingOne,
                      ArenaRatingManager.RatingSnapshot ratingTwo) {
        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
        this.instance = instance;
        this.ratingOne = ratingOne;
        this.ratingTwo = ratingTwo;
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getPlayerOne() {
        return playerOne;
    }

    public UUID getPlayerTwo() {
        return playerTwo;
    }

    public ArenaInstance getInstance() {
        return instance;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
        if (state == State.ACTIVE) {
            this.activeAt = System.currentTimeMillis();
        }
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getActiveAt() {
        return activeAt;
    }

    public boolean involves(UUID playerId) {
        return playerOne.equals(playerId) || playerTwo.equals(playerId);
    }

    public UUID opponent(UUID playerId) {
        if (playerOne.equals(playerId)) return playerTwo;
        if (playerTwo.equals(playerId)) return playerOne;
        throw new IllegalArgumentException("Player " + playerId + " is not part of this match");
    }

    public ArenaRatingManager.RatingSnapshot ratingFor(UUID playerId) {
        if (playerOne.equals(playerId)) {
            return ratingOne;
        }
        if (playerTwo.equals(playerId)) {
            return ratingTwo;
        }
        throw new IllegalArgumentException("Player " + playerId + " is not part of this match");
    }

    public ArenaRatingManager.RatingSnapshot ratingForOpponent(UUID playerId) {
        return ratingFor(opponent(playerId));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArenaMatch other)) return false;
        return playerOne.equals(other.playerOne)
                && playerTwo.equals(other.playerTwo)
                && Objects.equals(instance, other.instance)
                && createdAt == other.createdAt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerOne, playerTwo, instance, createdAt);
    }
}
