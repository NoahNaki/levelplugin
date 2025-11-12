package me.nakilex.levelplugin.pathfinding.deployment;

import me.nakilex.levelplugin.quests.data.QuestReward;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable definition of a mercenary deployment option.  Definitions are
 * rotated daily by {@link MercenaryDeploymentManager} and drive the gameplay
 * loop (duration, rewards, recommended specialization, etc.).
 */
public final class MercenaryDeploymentDefinition {
    private final String id;
    private final String displayName;
    private final List<String> description;
    private final MercenarySpecialization recommended;
    private final int durationMinutes;
    private final int difficulty;
    private final double baseSuccessChance;
    private final QuestReward successReward;
    private final QuestReward failureReward;
    private final int battlePassProgress;
    private final int guildCoinReward;
    private final int guildExpReward;

    private MercenaryDeploymentDefinition(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.description = Collections.unmodifiableList(new ArrayList<>(builder.description));
        this.recommended = builder.recommended;
        this.durationMinutes = builder.durationMinutes;
        this.difficulty = builder.difficulty;
        this.baseSuccessChance = Math.min(0.95, Math.max(0.05, builder.baseSuccessChance));
        this.successReward = builder.successReward;
        this.failureReward = builder.failureReward;
        this.battlePassProgress = Math.max(0, builder.battlePassProgress);
        this.guildCoinReward = Math.max(0, builder.guildCoinReward);
        this.guildExpReward = Math.max(0, builder.guildExpReward);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public List<String> description() {
        return description;
    }

    public MercenarySpecialization recommended() {
        return recommended;
    }

    public int durationMinutes() {
        return durationMinutes;
    }

    public long durationMillis() {
        return durationMinutes * 60L * 1000L;
    }

    public int difficulty() {
        return difficulty;
    }

    public double baseSuccessChance() {
        return baseSuccessChance;
    }

    public QuestReward successReward() {
        return successReward;
    }

    public QuestReward failureReward() {
        return failureReward;
    }

    public int battlePassProgress() {
        return battlePassProgress;
    }

    public int guildCoinReward() {
        return guildCoinReward;
    }

    public int guildExpReward() {
        return guildExpReward;
    }

    /** Builder used so definitions read cleanly when registered. */
    public static class Builder {
        private final String id;
        private String displayName;
        private final List<String> description = new ArrayList<>();
        private MercenarySpecialization recommended = MercenarySpecialization.ROGUE;
        private int durationMinutes = 120;
        private int difficulty = 1;
        private double baseSuccessChance = 0.6;
        private QuestReward successReward;
        private QuestReward failureReward;
        private int battlePassProgress = 0;
        private int guildCoinReward = 0;
        private int guildExpReward = 0;

        public Builder(String id) {
            this.id = id;
            this.displayName = ChatColor.GOLD + id;
        }

        public Builder name(String name) {
            this.displayName = name;
            return this;
        }

        public Builder addDescription(String line) {
            if (line != null && !line.isBlank()) {
                this.description.add(ChatColor.GRAY + line);
            }
            return this;
        }

        public Builder recommended(MercenarySpecialization spec) {
            if (spec != null) {
                this.recommended = spec;
            }
            return this;
        }

        public Builder durationMinutes(int minutes) {
            if (minutes > 0) {
                this.durationMinutes = minutes;
            }
            return this;
        }

        public Builder difficulty(int difficulty) {
            this.difficulty = Math.max(1, Math.min(5, difficulty));
            return this;
        }

        public Builder baseSuccess(double chance) {
            this.baseSuccessChance = chance;
            return this;
        }

        public Builder reward(QuestReward reward) {
            this.successReward = reward;
            return this;
        }

        public Builder failureReward(QuestReward reward) {
            this.failureReward = reward;
            return this;
        }

        public Builder battlePassProgress(int amount) {
            this.battlePassProgress = amount;
            return this;
        }

        public Builder guildCoinReward(int coins) {
            this.guildCoinReward = coins;
            return this;
        }

        public Builder guildExpReward(int exp) {
            this.guildExpReward = exp;
            return this;
        }

        public MercenaryDeploymentDefinition build() {
            if (successReward == null) {
                throw new IllegalStateException("Deployment requires a success reward");
            }
            if (description.isEmpty()) {
                description.add(ChatColor.GRAY + "No description provided.");
            }
            return new MercenaryDeploymentDefinition(this);
        }
    }
}
