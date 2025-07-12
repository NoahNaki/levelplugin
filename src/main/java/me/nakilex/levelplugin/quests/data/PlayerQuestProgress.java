package me.nakilex.levelplugin.quests.data;

import java.util.*;

public class PlayerQuestProgress {
    private final Quest quest;
    private final Map<Integer, Integer> objectiveProgress = new HashMap<>();
    private final Set<String> flags = new HashSet<>();

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

    public void incrementProgress(int objectiveIndex, int amount, boolean allowOverflow, int max) {
        int current = getProgress(objectiveIndex);
        int newValue = current + amount;
        if (!allowOverflow) {
            newValue = Math.min(newValue, max);
        }
        objectiveProgress.put(objectiveIndex, newValue);
    }

    /**
     * Directly sets the progress value for an objective. Used when loading
     * progress from disk.
     */
    public void setProgress(int objectiveIndex, int amount) {
        objectiveProgress.put(objectiveIndex, amount);
    }

    /** Retrieve all flags currently set for this quest. */
    public Set<String> getFlags() {
        return flags;
    }

    /** Add a flag representing some state within the quest. */
    public void addFlag(String flag) {
        if (flag != null) {
            flags.add(flag.toLowerCase());
        }
    }

    /** Remove a previously set flag. */
    public void removeFlag(String flag) {
        if (flag != null) {
            flags.remove(flag.toLowerCase());
        }
    }

    /** Determine whether this quest progress contains a particular flag. */
    public boolean hasFlag(String flag) {
        return flag != null && flags.contains(flag.toLowerCase());
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
