package me.nakilex.levelplugin.quests.data;

import java.util.HashMap;
import java.util.Map;

public class PlayerQuestProgress {
    private final Quest quest;
    private final Map<Integer, Integer> objectiveProgress = new HashMap<>();

    public PlayerQuestProgress(Quest quest) {
        this.quest = quest;
        for (int i = 0; i < quest.getObjectives().size(); i++) {
            objectiveProgress.put(i, 0);
        }
    }

    public Quest getQuest() {
        return quest;
    }

    public int getProgress(int objectiveIndex) {
        return objectiveProgress.getOrDefault(objectiveIndex, 0);
    }

    public void incrementProgress(int objectiveIndex, int amount) {
        objectiveProgress.put(objectiveIndex, getProgress(objectiveIndex) + amount);
    }

    /**
     * Directly sets the progress value for an objective. Used when loading
     * progress from disk.
     */
    public void setProgress(int objectiveIndex, int amount) {
        objectiveProgress.put(objectiveIndex, amount);
    }

    public boolean isComplete() {
        for (int i = 0; i < quest.getObjectives().size(); i++) {
            QuestObjective obj = quest.getObjectives().get(i);
            if (getProgress(i) < obj.getAmount()) {
                return false;
            }
        }
        return true;
    }
}
