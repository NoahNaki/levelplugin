package me.nakilex.levelplugin.quests.data;

public class QuestObjective {
    private final QuestObjectiveType type;
    private final String target;
    private final int amount;

    public QuestObjective(QuestObjectiveType type, String target, int amount) {
        this.type = type;
        this.target = target;
        this.amount = amount;
    }

    public QuestObjectiveType getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    public int getAmount() {
        return amount;
    }
}
