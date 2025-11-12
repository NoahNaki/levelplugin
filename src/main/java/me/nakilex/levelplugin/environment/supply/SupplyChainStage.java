package me.nakilex.levelplugin.environment.supply;

import org.bukkit.Material;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** A single stage within a supply chain recipe. */
public final class SupplyChainStage {
    private final int index;
    private final String name;
    private final Map<Material, Integer> requirements;
    private final int productionSeconds;
    private final int guildExp;
    private final int guildCoins;
    private final int battlePassProgress;
    private final String rewardDescription;

    private SupplyChainStage(Builder builder) {
        this.index = builder.index;
        this.name = builder.name;
        this.requirements = Collections.unmodifiableMap(new EnumMap<>(builder.requirements));
        this.productionSeconds = builder.productionSeconds;
        this.guildExp = builder.guildExp;
        this.guildCoins = builder.guildCoins;
        this.battlePassProgress = builder.battlePassProgress;
        this.rewardDescription = builder.rewardDescription;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public Map<Material, Integer> getRequirements() {
        return requirements;
    }

    public int getProductionSeconds() {
        return productionSeconds;
    }

    public int getGuildExp() {
        return guildExp;
    }

    public int getGuildCoins() {
        return guildCoins;
    }

    public int getBattlePassProgress() {
        return battlePassProgress;
    }

    public String getRewardDescription() {
        return rewardDescription;
    }

    public static Builder builder(int index) {
        return new Builder(index);
    }

    public static final class Builder {
        private final int index;
        private String name;
        private final Map<Material, Integer> requirements = new EnumMap<>(Material.class);
        private int productionSeconds = 600;
        private int guildExp = 150;
        private int guildCoins = 150;
        private int battlePassProgress = 25;
        private String rewardDescription = "";

        private Builder(int index) {
            this.index = index;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder require(Material material, int amount) {
            requirements.put(material, amount);
            return this;
        }

        public Builder productionSeconds(int productionSeconds) {
            this.productionSeconds = productionSeconds;
            return this;
        }

        public Builder guildExp(int guildExp) {
            this.guildExp = guildExp;
            return this;
        }

        public Builder guildCoins(int guildCoins) {
            this.guildCoins = guildCoins;
            return this;
        }

        public Builder battlePassProgress(int battlePassProgress) {
            this.battlePassProgress = battlePassProgress;
            return this;
        }

        public Builder rewardDescription(String rewardDescription) {
            this.rewardDescription = rewardDescription;
            return this;
        }

        public SupplyChainStage build() {
            if (name == null) {
                throw new IllegalStateException("Stage name missing");
            }
            return new SupplyChainStage(this);
        }
    }
}

