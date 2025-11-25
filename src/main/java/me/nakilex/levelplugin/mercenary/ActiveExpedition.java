package me.nakilex.levelplugin.mercenary;

import java.time.Instant;
import java.util.List;

/** Tracks an expedition currently running for a player and their party. */
public final class ActiveExpedition {
    private final List<Integer> npcIds;
    private final ExpeditionDefinition definition;
    private final Instant endTime;
    private final double successChance;

    public ActiveExpedition(List<Integer> npcIds, ExpeditionDefinition definition, Instant endTime, double successChance) {
        this.npcIds = npcIds;
        this.definition = definition;
        this.endTime = endTime;
        this.successChance = successChance;
    }

    public List<Integer> getNpcIds() {
        return npcIds;
    }

    public ExpeditionDefinition getDefinition() {
        return definition;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public double getSuccessChance() {
        return successChance;
    }
}
