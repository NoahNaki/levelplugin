package me.nakilex.levelplugin.duels.managers;

import java.util.UUID;

/**
 * Represents a request for a duel from one player to another.
 */
public class DuelRequest {
    private final UUID requester;
    private final UUID target;
    private final long timestamp; // time created (ms)

    public DuelRequest(UUID requester, UUID target, long timestamp) {
        this.requester = requester;
        this.target = target;
        this.timestamp = timestamp;
    }

    public UUID getRequester() {
        return requester;
    }

    public UUID getTarget() {
        return target;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
