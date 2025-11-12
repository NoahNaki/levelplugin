package me.nakilex.levelplugin.dungeon.rift;

import org.bukkit.configuration.ConfigurationSection;

/** Persistent guild-level state for the frontier rift ladder. */
public final class FrontierRiftState {
    private int currentStage = 1;
    private int bestStage = 0;
    private String mutatorId = "steady";
    private long mutatorEpoch = Long.MIN_VALUE;
    private long lastCompletion = 0L;
    private int failStreak = 0;

    public static FrontierRiftState load(ConfigurationSection section) {
        FrontierRiftState state = new FrontierRiftState();
        if (section == null) {
            return state;
        }
        state.currentStage = Math.max(1, section.getInt("current_stage", 1));
        state.bestStage = Math.max(0, section.getInt("best_stage", 0));
        state.mutatorId = section.getString("mutator", "steady");
        state.mutatorEpoch = section.getLong("mutator_epoch", Long.MIN_VALUE);
        state.lastCompletion = section.getLong("last_completion", 0L);
        state.failStreak = Math.max(0, section.getInt("fail_streak", 0));
        return state;
    }

    public void save(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        section.set("current_stage", currentStage);
        section.set("best_stage", bestStage);
        section.set("mutator", mutatorId);
        section.set("mutator_epoch", mutatorEpoch);
        section.set("last_completion", lastCompletion);
        section.set("fail_streak", failStreak);
    }

    public int getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(int currentStage) {
        this.currentStage = Math.max(1, currentStage);
    }

    public void advanceStage() {
        currentStage = Math.max(1, currentStage + 1);
        if (currentStage - 1 > bestStage) {
            bestStage = currentStage - 1;
        }
        failStreak = 0;
        lastCompletion = System.currentTimeMillis();
    }

    public void recordFailure() {
        failStreak++;
        lastCompletion = System.currentTimeMillis();
        if (failStreak >= 3 && currentStage > 1) {
            currentStage--;
            failStreak = 0;
        }
    }

    public int getBestStage() {
        return bestStage;
    }

    public void setBestStage(int bestStage) {
        this.bestStage = Math.max(this.bestStage, Math.max(0, bestStage));
    }

    public String getMutatorId() {
        return mutatorId;
    }

    public void setMutator(String mutatorId, long epochDay) {
        this.mutatorId = mutatorId;
        this.mutatorEpoch = epochDay;
    }

    public long getMutatorEpoch() {
        return mutatorEpoch;
    }

    public long getLastCompletion() {
        return lastCompletion;
    }

    public int getFailStreak() {
        return failStreak;
    }
}

