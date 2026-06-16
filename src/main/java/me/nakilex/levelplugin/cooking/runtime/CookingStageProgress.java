package me.nakilex.levelplugin.cooking.runtime;

import me.nakilex.levelplugin.cooking.model.CookingIngredientRequirement;
import me.nakilex.levelplugin.cooking.model.CookingStage;

import java.util.HashMap;
import java.util.Map;

/** Mutable runtime progress for the currently selected cooking recipe. */
public class CookingStageProgress {
    private final Map<Integer, Map<String, Integer>> insertedByStage = new HashMap<>();
    private int currentStageIndex;

    public CookingStageProgress() {
        this.currentStageIndex = 0;
    }

    public int currentStageIndex() {
        return currentStageIndex;
    }

    public int insertedAmount(CookingIngredientRequirement requirement) {
        return insertedAmount(currentStageIndex, requirement);
    }

    public int insertedAmount(int stageIndex, CookingIngredientRequirement requirement) {
        if (requirement == null) {
            return 0;
        }
        return insertedByStage.getOrDefault(stageIndex, Map.of())
                .getOrDefault(requirement.progressKey(), 0);
    }

    public Map<Integer, Map<String, Integer>> insertedByStageSnapshot() {
        Map<Integer, Map<String, Integer>> snapshot = new HashMap<>();
        for (Map.Entry<Integer, Map<String, Integer>> entry : insertedByStage.entrySet()) {
            snapshot.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        return Map.copyOf(snapshot);
    }

    public int remainingAmount(CookingIngredientRequirement requirement) {
        return remainingAmount(requirement, 1);
    }

    public int remainingAmount(CookingIngredientRequirement requirement, int craftAmount) {
        if (requirement == null) {
            return 0;
        }
        return Math.max(0, requiredAmount(requirement, craftAmount) - insertedAmount(requirement));
    }

    public void addIngredient(CookingIngredientRequirement requirement, int amount) {
        if (requirement == null || amount <= 0) {
            return;
        }
        Map<String, Integer> currentStageProgress = insertedByStage.computeIfAbsent(currentStageIndex, ignored -> new HashMap<>());
        currentStageProgress.merge(requirement.progressKey(), amount, Integer::sum);
    }

    public boolean isRequirementComplete(CookingIngredientRequirement requirement) {
        return isRequirementComplete(requirement, 1);
    }

    public boolean isRequirementComplete(CookingIngredientRequirement requirement, int craftAmount) {
        return requirement != null && insertedAmount(requirement) >= requiredAmount(requirement, craftAmount);
    }

    public boolean areRequirementsComplete(CookingStage stage) {
        return areRequirementsComplete(stage, 1);
    }

    public boolean areRequirementsComplete(CookingStage stage, int craftAmount) {
        return stage != null && !stage.requirements().isEmpty()
                && stage.requirements().stream().allMatch(requirement -> isRequirementComplete(requirement, craftAmount));
    }

    private int requiredAmount(CookingIngredientRequirement requirement, int craftAmount) {
        return Math.max(1, requirement.amount() * Math.max(1, craftAmount));
    }

    public void advance() {
        currentStageIndex++;
    }
}
