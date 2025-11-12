package me.nakilex.levelplugin.dungeon.trial;

import org.bukkit.configuration.ConfigurationSection;

/** Per-player persistence for arcane trials. */
public final class ArcaneTrialState {
    private int highestTier;
    private int marks;
    private int prestige;
    private long lastCompletion;

    public static ArcaneTrialState load(ConfigurationSection section) {
        ArcaneTrialState state = new ArcaneTrialState();
        if (section == null) {
            return state;
        }
        state.highestTier = section.getInt("highest_tier", 0);
        state.marks = section.getInt("marks", 0);
        state.prestige = section.getInt("prestige", 0);
        state.lastCompletion = section.getLong("last_completion", 0L);
        return state;
    }

    public void save(ConfigurationSection section) {
        section.set("highest_tier", highestTier);
        section.set("marks", marks);
        section.set("prestige", prestige);
        section.set("last_completion", lastCompletion);
    }

    public int getHighestTier() {
        return highestTier;
    }

    public void setHighestTier(int highestTier) {
        this.highestTier = Math.max(this.highestTier, highestTier);
    }

    public int getMarks() {
        return marks;
    }

    public void addMarks(int amount) {
        marks = Math.max(0, marks + amount);
        lastCompletion = System.currentTimeMillis();
    }

    public boolean spendMarks(int amount) {
        if (marks < amount) return false;
        marks -= amount;
        return true;
    }

    public int getPrestige() {
        return prestige;
    }

    public void prestigeUp() {
        prestige++;
        marks = 0;
        highestTier = 0;
    }

    public long getLastCompletion() {
        return lastCompletion;
    }
}

