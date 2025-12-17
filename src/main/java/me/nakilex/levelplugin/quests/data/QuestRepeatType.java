package me.nakilex.levelplugin.quests.data;

/**
 * Defines how often a quest can be repeated. This is intentionally generic so
 * quests can opt into daily, weekly, or one-time behaviour without bespoke
 * flags scattered across the codebase.
 */
public enum QuestRepeatType {
    ONE_TIME(0L, "One-Time"),
    DAILY(24 * 60 * 60 * 1000L, "Daily"),
    WEEKLY(7 * 24 * 60 * 60 * 1000L, "Weekly");

    private final long cooldownMillis;
    private final String displayName;

    QuestRepeatType(long cooldownMillis, String displayName) {
        this.cooldownMillis = cooldownMillis;
        this.displayName = displayName;
    }

    /** Milliseconds before the quest becomes available again. */
    public long getCooldownMillis() {
        return cooldownMillis;
    }

    /** Player-facing label for menus and chat output. */
    public String getDisplayName() {
        return displayName;
    }

    public boolean isRepeatable() {
        return cooldownMillis > 0;
    }
}
