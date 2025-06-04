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

    public Quest(String id, String name, String description, List<QuestObjective> objectives,
                 int levelRequirement, List<String> questRequirements,
                 PlayerClass classRequirement, QuestReward reward) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.objectives = objectives;
        this.levelRequirement = levelRequirement;
        this.questRequirements = questRequirements;
        this.classRequirement = classRequirement;
        this.reward = reward;
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
}
