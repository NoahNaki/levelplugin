package me.nakilex.levelplugin.arena.match;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player combat statistics for arena matches so that both duels
 * and team fights can surface post-game summaries without duplicating state.
 */
public class ArenaCombatTracker {
    private final Map<UUID, CombatStats> statsByPlayer = new ConcurrentHashMap<>();

    /** Begin tracking the provided players, resetting any previous stats. */
    public void beginTracking(Collection<UUID> participants) {
        for (UUID id : participants) {
            statsByPlayer.put(id, new CombatStats());
        }
    }

    /** Record damage dealt by the given player. */
    public void recordDamage(UUID playerId, double amount) {
        if (amount <= 0) {
            return;
        }
        statsByPlayer.computeIfAbsent(playerId, ignored -> new CombatStats())
                .addDamage(amount);
    }

    /** Record healing performed by the given player. */
    public void recordHealing(UUID playerId, double amount) {
        if (amount <= 0) {
            return;
        }
        statsByPlayer.computeIfAbsent(playerId, ignored -> new CombatStats())
                .addHealing(amount);
    }

    /**
     * Return a snapshot of combat statistics for the provided players.
     * The returned map preserves the order of the given list and contains
     * copies so callers can freely mutate without affecting tracked values.
     */
    public Map<UUID, CombatStats> snapshot(List<UUID> orderedParticipants) {
        Map<UUID, CombatStats> snapshot = new LinkedHashMap<>();
        for (UUID id : orderedParticipants) {
            CombatStats stats = statsByPlayer.getOrDefault(id, CombatStats.EMPTY);
            snapshot.put(id, stats.copy());
        }
        return snapshot;
    }

    /** Stop tracking the provided players, clearing their accumulated stats. */
    public void clear(Collection<UUID> participants) {
        for (UUID id : participants) {
            statsByPlayer.remove(id);
        }
    }

    /** Immutable view of combat totals for a single player. */
    public static final class CombatStats {
        private static final CombatStats EMPTY = new CombatStats();

        private double damageDealt;
        private double healingDone;

        private CombatStats() {
        }

        private CombatStats(double damageDealt, double healingDone) {
            this.damageDealt = damageDealt;
            this.healingDone = healingDone;
        }

        private void addDamage(double amount) {
            damageDealt += amount;
        }

        private void addHealing(double amount) {
            healingDone += amount;
        }

        public double damageDealt() {
            return damageDealt;
        }

        public double healingDone() {
            return healingDone;
        }

        private CombatStats copy() {
            return new CombatStats(damageDealt, healingDone);
        }
    }
}
