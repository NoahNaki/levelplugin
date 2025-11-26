package me.nakilex.levelplugin.tower;

/** Snapshot of a player's current tower run for scoreboard display. */
public record TowerStatus(int stage, long secondsRemaining, long nextStartSeconds,
                          int mobsRemaining, int timeLimitSeconds, boolean awaitingNext) {
}
