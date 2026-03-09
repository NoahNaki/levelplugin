package me.nakilex.levelplugin.contracts;

import me.nakilex.levelplugin.progression.objectives.ObjectiveType;

public record ContractDefinition(String id,
                                 ObjectiveType objectiveType,
                                 String target,
                                 int requiredAmount,
                                 int rewardCoins) {
}
