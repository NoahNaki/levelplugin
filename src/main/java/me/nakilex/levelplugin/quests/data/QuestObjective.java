package me.nakilex.levelplugin.quests.data;

public class QuestObjective {
    private final QuestObjectiveType type;
    private final String target;
    private final int amount;
    /**
     * Whether progress should continue counting after the objective
     * amount has been reached. Defaults to {@code false} so progress
     * is capped at the required amount.
     */
    private final boolean allowOverflow;

    public QuestObjective(QuestObjectiveType type, String target, int amount) {
        this(type, target, amount, false);
    }

    public QuestObjective(QuestObjectiveType type, String target, int amount,
                          boolean allowOverflow) {
        this.type = type;
        this.target = target;
        this.amount = amount;
        this.allowOverflow = allowOverflow;
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

    public boolean isAllowOverflow() {
        return allowOverflow;
    }
}
