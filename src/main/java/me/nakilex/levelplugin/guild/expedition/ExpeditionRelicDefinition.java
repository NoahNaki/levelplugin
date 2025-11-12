package me.nakilex.levelplugin.guild.expedition;

import org.bukkit.Material;

/** Immutable definition of a guild expedition relic. */
public final class ExpeditionRelicDefinition {
    private final String id;
    private final String displayName;
    private final String description;
    private final String layoutKey;
    private final int progressRequired;
    private final int investmentCost;
    private final int progressPerInvestment;
    private final int guildCoinReward;
    private final int guildExpReward;
    private final int battlePassReward;
    private final String effectDescription;
    private final int durationDays;
    private final Material maintenanceMaterial;
    private final int maintenanceBundle;
    private final int maintenanceExtensionDays;
    private final String requiredBuilding;
    private final int requiredStage;
    private final int timeLimitMinutes;

    private ExpeditionRelicDefinition(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.layoutKey = builder.layoutKey;
        this.progressRequired = builder.progressRequired;
        this.investmentCost = builder.investmentCost;
        this.progressPerInvestment = builder.progressPerInvestment;
        this.guildCoinReward = builder.guildCoinReward;
        this.guildExpReward = builder.guildExpReward;
        this.battlePassReward = builder.battlePassReward;
        this.effectDescription = builder.effectDescription;
        this.durationDays = builder.durationDays;
        this.maintenanceMaterial = builder.maintenanceMaterial;
        this.maintenanceBundle = builder.maintenanceBundle;
        this.maintenanceExtensionDays = builder.maintenanceExtensionDays;
        this.requiredBuilding = builder.requiredBuilding;
        this.requiredStage = builder.requiredStage;
        this.timeLimitMinutes = builder.timeLimitMinutes;
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

    public String getLayoutKey() {
        return layoutKey;
    }

    public int getProgressRequired() {
        return progressRequired;
    }

    public int getInvestmentCost() {
        return investmentCost;
    }

    public int getProgressPerInvestment() {
        return progressPerInvestment;
    }

    public int getGuildCoinReward() {
        return guildCoinReward;
    }

    public int getGuildExpReward() {
        return guildExpReward;
    }

    public int getBattlePassReward() {
        return battlePassReward;
    }

    public String getEffectDescription() {
        return effectDescription;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public Material getMaintenanceMaterial() {
        return maintenanceMaterial;
    }

    public int getMaintenanceBundle() {
        return maintenanceBundle;
    }

    public int getMaintenanceExtensionDays() {
        return maintenanceExtensionDays;
    }

    public String getRequiredBuilding() {
        return requiredBuilding;
    }

    public int getRequiredStage() {
        return requiredStage;
    }

    public int getTimeLimitMinutes() {
        return timeLimitMinutes;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private String displayName;
        private String description = "";
        private String layoutKey;
        private int progressRequired = 100;
        private int investmentCost = 250;
        private int progressPerInvestment = 10;
        private int guildCoinReward = 500;
        private int guildExpReward = 350;
        private int battlePassReward = 60;
        private String effectDescription = "";
        private int durationDays = 5;
        private Material maintenanceMaterial = Material.PRISMARINE_CRYSTALS;
        private int maintenanceBundle = 16;
        private int maintenanceExtensionDays = 1;
        private String requiredBuilding = "war_room";
        private int requiredStage = 1;
        private int timeLimitMinutes = 20;

        private Builder(String id) {
            this.id = id;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder layoutKey(String layoutKey) {
            this.layoutKey = layoutKey;
            return this;
        }

        public Builder progressRequired(int progressRequired) {
            this.progressRequired = progressRequired;
            return this;
        }

        public Builder investmentCost(int investmentCost) {
            this.investmentCost = investmentCost;
            return this;
        }

        public Builder progressPerInvestment(int progressPerInvestment) {
            this.progressPerInvestment = progressPerInvestment;
            return this;
        }

        public Builder guildCoinReward(int guildCoinReward) {
            this.guildCoinReward = guildCoinReward;
            return this;
        }

        public Builder guildExpReward(int guildExpReward) {
            this.guildExpReward = guildExpReward;
            return this;
        }

        public Builder battlePassReward(int battlePassReward) {
            this.battlePassReward = battlePassReward;
            return this;
        }

        public Builder effectDescription(String effectDescription) {
            this.effectDescription = effectDescription;
            return this;
        }

        public Builder durationDays(int durationDays) {
            this.durationDays = durationDays;
            return this;
        }

        public Builder maintenanceMaterial(Material maintenanceMaterial) {
            this.maintenanceMaterial = maintenanceMaterial;
            return this;
        }

        public Builder maintenanceBundle(int maintenanceBundle) {
            this.maintenanceBundle = maintenanceBundle;
            return this;
        }

        public Builder maintenanceExtensionDays(int maintenanceExtensionDays) {
            this.maintenanceExtensionDays = maintenanceExtensionDays;
            return this;
        }

        public Builder requiredBuilding(String requiredBuilding, int requiredStage) {
            this.requiredBuilding = requiredBuilding;
            this.requiredStage = requiredStage;
            return this;
        }

        public Builder timeLimitMinutes(int timeLimitMinutes) {
            this.timeLimitMinutes = timeLimitMinutes;
            return this;
        }

        public ExpeditionRelicDefinition build() {
            if (displayName == null) throw new IllegalStateException("displayName not set");
            if (layoutKey == null) throw new IllegalStateException("layoutKey not set");
            if (progressRequired <= 0) throw new IllegalStateException("progressRequired must be positive");
            if (progressPerInvestment <= 0) throw new IllegalStateException("progressPerInvestment must be positive");
            if (investmentCost <= 0) throw new IllegalStateException("investmentCost must be positive");
            return new ExpeditionRelicDefinition(this);
        }
    }
}
