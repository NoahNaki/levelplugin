package me.nakilex.levelplugin.mercenary;

import java.time.Instant;

/** Tracks an expedition currently running for a player/mercenary pair. */
public final class ActiveExpedition {
    private final int npcId;
    private final ExpeditionDefinition definition;
    private final Instant endTime;
    private final double successChance;

    public ActiveExpedition(int npcId, ExpeditionDefinition definition, Instant endTime, double successChance) {
        this.npcId = npcId;
        this.definition = definition;
        this.endTime = endTime;
        this.successChance = successChance;
    }

    public int getNpcId() {
        return npcId;
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
