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
    /** Target used for navigation beacons. May be null. */
    private final BeaconTarget beaconTarget;

    public QuestObjective(QuestObjectiveType type, String target, int amount) {
        this(type, target, amount, false, null);
    }

    public QuestObjective(QuestObjectiveType type, String target, int amount,
                          BeaconTarget beaconTarget) {
        this(type, target, amount, false, beaconTarget);
    }

    public QuestObjective(QuestObjectiveType type, String target, int amount,
                          boolean allowOverflow) {
        this(type, target, amount, allowOverflow, null);
    }

    public QuestObjective(QuestObjectiveType type, String target, int amount,
                          boolean allowOverflow, BeaconTarget beaconTarget) {
        this.type = type;
        this.target = target;
        this.amount = amount;
        this.allowOverflow = allowOverflow;
        this.beaconTarget = beaconTarget;
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

    public BeaconTarget getBeaconTarget() {
        return beaconTarget;
    }
}
