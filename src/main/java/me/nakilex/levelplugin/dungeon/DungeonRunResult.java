package me.nakilex.levelplugin.dungeon;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Summary of a dungeon run completion. */
public final class DungeonRunResult {
    private final String layoutKey;
    private final String displayName;
    private final Set<UUID> participants;
    private final long startTime;
    private final long completionTime;

    public DungeonRunResult(String layoutKey,
                             String displayName,
                             Set<UUID> participants,
                             long startTime,
                             long completionTime) {
        this.layoutKey = layoutKey;
        this.displayName = displayName;
        this.participants = Collections.unmodifiableSet(new HashSet<>(participants));
        this.startTime = startTime;
        this.completionTime = completionTime;
    }

    public String getLayoutKey() {
        return layoutKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<UUID> getParticipants() {
        return participants;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getCompletionTime() {
        return completionTime;
    }
}

