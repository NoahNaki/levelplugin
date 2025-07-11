package me.nakilex.levelplugin.quests.data;

import me.nakilex.levelplugin.player.classes.data.PlayerClass;

import java.util.List;

public class Quest {
    private final String id;
    private final String name;
    private final String description;
    private final List<QuestObjective> objectives;

    private final int levelRequirement;
    private final List<String> questRequirements;
    private final PlayerClass classRequirement;
    private final QuestReward reward;

    /** ID of the NPC that starts this quest. */
    private final Integer npcGiverId;

    /** Dialog lines shown when starting the quest. */
    private final List<String> dialogLines;

    /** Whether this is considered a main quest. */
    private final boolean mainQuest;

    public Quest(String id, String name, String description, List<QuestObjective> objectives,
                 int levelRequirement, List<String> questRequirements,
                 PlayerClass classRequirement, QuestReward reward,
                 Integer npcGiverId, List<String> dialogLines,
                 boolean mainQuest) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.objectives = objectives;
        this.levelRequirement = levelRequirement;
        this.questRequirements = questRequirements;
        this.classRequirement = classRequirement;
        this.reward = reward;
        this.npcGiverId = npcGiverId;
        this.dialogLines = dialogLines;
        this.mainQuest = mainQuest;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<QuestObjective> getObjectives() {
        return objectives;
    }

    public int getLevelRequirement() {
        return levelRequirement;
    }

    public List<String> getQuestRequirements() {
        return questRequirements;
    }

    public PlayerClass getClassRequirement() {
        return classRequirement;
    }

    public QuestReward getReward() {
        return reward;
    }

    public Integer getNpcGiverId() {
        return npcGiverId;
    }

    public List<String> getDialogLines() {
        return dialogLines;
    }

    public boolean isMainQuest() {
        return mainQuest;
    }
}
