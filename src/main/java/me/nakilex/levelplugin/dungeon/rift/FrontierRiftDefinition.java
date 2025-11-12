package me.nakilex.levelplugin.dungeon.rift;

/** Immutable definition of a frontier rift stage. */
public final class FrontierRiftDefinition {
    private final String id;
    private final int stage;
    private final String displayName;
    private final String layoutKey;
    private final String description;
    private final int baseGuildCoins;
    private final int baseGuildExp;
    private final int battlePassProgress;
    private final String recommendedPower;
    private final int timeLimitMinutes;

    private FrontierRiftDefinition(Builder builder) {
        this.id = builder.id;
        this.stage = builder.stage;
        this.displayName = builder.displayName;
        this.layoutKey = builder.layoutKey;
        this.description = builder.description;
        this.baseGuildCoins = builder.baseGuildCoins;
        this.baseGuildExp = builder.baseGuildExp;
        this.battlePassProgress = builder.battlePassProgress;
        this.recommendedPower = builder.recommendedPower;
        this.timeLimitMinutes = builder.timeLimitMinutes;
    }

    public String getId() {
        return id;
    }

    public int getStage() {
        return stage;
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

    public int getBaseGuildCoins() {
        return baseGuildCoins;
    }

    public int getBaseGuildExp() {
        return baseGuildExp;
    }

    public int getBattlePassProgress() {
        return battlePassProgress;
    }

    public String getRecommendedPower() {
        return recommendedPower;
    }

    public int getTimeLimitMinutes() {
        return timeLimitMinutes;
    }

    public static Builder builder(String id, int stage) {
        return new Builder(id, stage);
    }

    public static final class Builder {
        private final String id;
        private final int stage;
        private String displayName;
        private String layoutKey;
        private String description = "";
        private int baseGuildCoins = 100;
        private int baseGuildExp = 75;
        private int battlePassProgress = 25;
        private String recommendedPower = "Tier I";
        private int timeLimitMinutes = 20;

        private Builder(String id, int stage) {
            this.id = id;
            this.stage = stage;
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

        public Builder baseGuildCoins(int baseGuildCoins) {
            this.baseGuildCoins = baseGuildCoins;
            return this;
        }

        public Builder baseGuildExp(int baseGuildExp) {
            this.baseGuildExp = baseGuildExp;
            return this;
        }

        public Builder battlePassProgress(int battlePassProgress) {
            this.battlePassProgress = battlePassProgress;
            return this;
        }

        public Builder recommendedPower(String recommendedPower) {
            this.recommendedPower = recommendedPower;
            return this;
        }

        public Builder timeLimitMinutes(int timeLimitMinutes) {
            this.timeLimitMinutes = timeLimitMinutes;
            return this;
        }

        public FrontierRiftDefinition build() {
            if (displayName == null) throw new IllegalStateException("displayName not set");
            if (layoutKey == null) throw new IllegalStateException("layoutKey not set");
            return new FrontierRiftDefinition(this);
        }
    }
}

