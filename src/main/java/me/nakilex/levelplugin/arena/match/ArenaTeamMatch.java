package me.nakilex.levelplugin.arena.match;

import me.nakilex.levelplugin.arena.instance.ArenaInstance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a 2v2 arena match. Tracks team membership, alive players and
 * spectators so that rejoining players can be placed into the correct state.
 */
public class ArenaTeamMatch {
    public enum State { COUNTDOWN, ACTIVE, FINISHED }

    private final List<UUID> teamOne;
    private final List<UUID> teamTwo;
    private final Set<UUID> aliveTeamOne;
    private final Set<UUID> aliveTeamTwo;
    private final Set<UUID> spectators = new HashSet<>();
    private final ArenaInstance instance;
    private final long createdAt;
    private State state = State.COUNTDOWN;
    private long activeAt;

    public ArenaTeamMatch(List<UUID> teamOne, List<UUID> teamTwo, ArenaInstance instance) {
        this.teamOne = List.copyOf(teamOne);
        this.teamTwo = List.copyOf(teamTwo);
        this.instance = instance;
        this.createdAt = System.currentTimeMillis();
        this.aliveTeamOne = new HashSet<>(teamOne);
        this.aliveTeamTwo = new HashSet<>(teamTwo);
    }

    public List<UUID> getTeamOne() {
        return teamOne;
    }

    public List<UUID> getTeamTwo() {
        return teamTwo;
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

    public boolean isTeamOne(UUID playerId) {
        return teamOne.contains(playerId);
    }

    public boolean isTeamTwo(UUID playerId) {
        return teamTwo.contains(playerId);
    }

    public Set<UUID> getAliveTeamOne() {
        return new HashSet<>(aliveTeamOne);
    }

    public Set<UUID> getAliveTeamTwo() {
        return new HashSet<>(aliveTeamTwo);
    }

    public boolean isSpectator(UUID playerId) {
        return spectators.contains(playerId);
    }

    public void markEliminated(UUID playerId) {
        spectators.add(playerId);
        aliveTeamOne.remove(playerId);
        aliveTeamTwo.remove(playerId);
    }

    public boolean isTeamEliminated(boolean firstTeam) {
        return firstTeam ? aliveTeamOne.isEmpty() : aliveTeamTwo.isEmpty();
    }

    public boolean involves(UUID playerId) {
        return teamOne.contains(playerId) || teamTwo.contains(playerId);
    }

    public List<UUID> allPlayers() {
        List<UUID> players = new ArrayList<>(teamOne);
        players.addAll(teamTwo);
        return players;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArenaTeamMatch that)) return false;
        return createdAt == that.createdAt
                && Objects.equals(teamOne, that.teamOne)
                && Objects.equals(teamTwo, that.teamTwo)
                && Objects.equals(instance, that.instance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamOne, teamTwo, instance, createdAt);
    }
}

