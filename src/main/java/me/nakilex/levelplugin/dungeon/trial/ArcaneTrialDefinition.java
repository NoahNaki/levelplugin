package me.nakilex.levelplugin.dungeon.trial;

/** Definition for a solo arcane trial tier. */
public final class ArcaneTrialDefinition {
    private final String id;
    private final int tier;
    private final String displayName;
    private final String layoutKey;
    private final String description;
    private final int markReward;
    private final int recommendedLevel;
    private final int battlePassProgress;
    private final int timeLimitMinutes;

    private ArcaneTrialDefinition(Builder builder) {
        this.id = builder.id;
        this.tier = builder.tier;
        this.displayName = builder.displayName;
        this.layoutKey = builder.layoutKey;
        this.description = builder.description;
        this.markReward = builder.markReward;
        this.recommendedLevel = builder.recommendedLevel;
        this.battlePassProgress = builder.battlePassProgress;
        this.timeLimitMinutes = builder.timeLimitMinutes;
    }

    public String getId() {
        return id;
    }

    public int getTier() {
        return tier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLayoutKey() {
        return layoutKey;
    }

    public String getDescription() {
        return description;
    }

    public int getMarkReward() {
        return markReward;
    }

    public int getRecommendedLevel() {
        return recommendedLevel;
    }

    public int getBattlePassProgress() {
        return battlePassProgress;
    }

    public int getTimeLimitMinutes() {
        return timeLimitMinutes;
    }

    public static Builder builder(String id, int tier) {
        return new Builder(id, tier);
    }

    public static final class Builder {
        private final String id;
        private final int tier;
        private String displayName;
        private String layoutKey;
        private String description = "";
        private int markReward = 50;
        private int recommendedLevel = 50;
        private int battlePassProgress = 30;
        private int timeLimitMinutes = 15;

        private Builder(String id, int tier) {
            this.id = id;
            this.tier = tier;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder layoutKey(String layoutKey) {
            this.layoutKey = layoutKey;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder markReward(int markReward) {
            this.markReward = markReward;
            return this;
        }

        public Builder recommendedLevel(int recommendedLevel) {
            this.recommendedLevel = recommendedLevel;
            return this;
        }

        public Builder battlePassProgress(int battlePassProgress) {
            this.battlePassProgress = battlePassProgress;
            return this;
        }

        public Builder timeLimitMinutes(int timeLimitMinutes) {
            this.timeLimitMinutes = timeLimitMinutes;
            return this;
        }

        public ArcaneTrialDefinition build() {
            if (displayName == null) throw new IllegalStateException("displayName missing");
            if (layoutKey == null) throw new IllegalStateException("layoutKey missing");
            return new ArcaneTrialDefinition(this);
        }
    }
}

