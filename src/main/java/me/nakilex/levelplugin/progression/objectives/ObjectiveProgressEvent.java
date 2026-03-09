package me.nakilex.levelplugin.progression.objectives;

import java.util.UUID;

public record ObjectiveProgressEvent(UUID playerId,
                                     ObjectiveType type,
                                     String target,
                                     int amount) {
}
