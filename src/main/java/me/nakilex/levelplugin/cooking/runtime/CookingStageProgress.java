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
        if (requirement == null) {
            return 0;
        }
        return insertedByStage.getOrDefault(currentStageIndex, Map.of())
                .getOrDefault(requirement.progressKey(), 0);
    }

    public int remainingAmount(CookingIngredientRequirement requirement) {
        if (requirement == null) {
            return 0;
        }
        return Math.max(0, requirement.amount() - insertedAmount(requirement));
    }

    public void addIngredient(CookingIngredientRequirement requirement, int amount) {
        if (requirement == null || amount <= 0) {
            return;
        }
        Map<String, Integer> currentStageProgress = insertedByStage.computeIfAbsent(currentStageIndex, ignored -> new HashMap<>());
        currentStageProgress.merge(requirement.progressKey(), amount, Integer::sum);
    }

    public boolean isRequirementComplete(CookingIngredientRequirement requirement) {
        return requirement != null && insertedAmount(requirement) >= requirement.amount();
    }

    public boolean areRequirementsComplete(CookingStage stage) {
        return stage != null && !stage.requirements().isEmpty()
                && stage.requirements().stream().allMatch(this::isRequirementComplete);
    }

    public void advance() {
        currentStageIndex++;
    }
}
