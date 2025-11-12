package me.nakilex.levelplugin.environment.supply;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Definition of a guild supply chain recipe unlocked by environment progress. */
public final class SupplyChainDefinition {
    private final String id;
    private final String displayName;
    private final String description;
    private final String requiredBuilding;
    private final int requiredStage;
    private final List<SupplyChainStage> stages;

    private SupplyChainDefinition(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.requiredBuilding = builder.requiredBuilding;
        this.requiredStage = builder.requiredStage;
        this.stages = Collections.unmodifiableList(new ArrayList<>(builder.stages));
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

    public String getRequiredBuilding() {
        return requiredBuilding;
    }

    public int getRequiredStage() {
        return requiredStage;
    }

    public List<SupplyChainStage> getStages() {
        return stages;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private String displayName;
        private String description = "";
        private String requiredBuilding = "workshop";
        private int requiredStage = 1;
        private final List<SupplyChainStage> stages = new ArrayList<>();

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

        public Builder requiredBuilding(String requiredBuilding, int stage) {
            this.requiredBuilding = requiredBuilding;
            this.requiredStage = stage;
            return this;
        }

        public Builder stage(SupplyChainStage stage) {
            this.stages.add(stage);
            return this;
        }

        public SupplyChainDefinition build() {
            if (displayName == null) {
                throw new IllegalStateException("displayName missing");
            }
            if (stages.isEmpty()) {
                throw new IllegalStateException("Supply chain must define at least one stage");
            }
            return new SupplyChainDefinition(this);
        }
    }
}

