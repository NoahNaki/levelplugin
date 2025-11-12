package me.nakilex.levelplugin.dungeon.rift;

/** Daily mutators applied to frontier rift runs. */
public final class FrontierRiftMutator {
    private final String id;
    private final String displayName;
    private final String description;
    private final double rewardMultiplier;
    private final double successBonus;

    public FrontierRiftMutator(String id,
                               String displayName,
                               String description,
                               double rewardMultiplier,
                               double successBonus) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.rewardMultiplier = rewardMultiplier;
        this.successBonus = successBonus;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public double getRewardMultiplier() {
        return rewardMultiplier;
    }

    public double getSuccessBonus() {
        return successBonus;
    }
}

