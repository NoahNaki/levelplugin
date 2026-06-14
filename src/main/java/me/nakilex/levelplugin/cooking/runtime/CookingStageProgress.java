package me.nakilex.levelplugin.cooking.runtime;

/** Mutable runtime progress for the currently selected cooking recipe. */
public class CookingStageProgress {
    private int currentStageIndex;

    public CookingStageProgress() {
        this.currentStageIndex = 0;
    }

    public int currentStageIndex() {
        return currentStageIndex;
    }

    public void advance() {
        currentStageIndex++;
    }
}
