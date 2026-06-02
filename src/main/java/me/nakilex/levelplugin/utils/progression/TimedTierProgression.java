package me.nakilex.levelplugin.utils.progression;

/**
 * Reusable short-lived progression state for gathering streaks and other activity chains.
 * Tiers rise after a configured number of activities and reset when the activity window expires.
 */
public final class TimedTierProgression {
    private final int maxTier;
    private final int activitiesPerTier;
    private final long timeoutMs;
    private int tier = 1;
    private int progress;
    private long lastActivityAt;

    public TimedTierProgression(int maxTier, int activitiesPerTier, long timeoutMs) {
        this.maxTier = Math.max(1, maxTier);
        this.activitiesPerTier = Math.max(1, activitiesPerTier);
        this.timeoutMs = Math.max(1L, timeoutMs);
    }

    public Update recordActivity(long now) {
        expireIfNeeded(now);
        int previousTier = tier;
        lastActivityAt = now;
        if (tier < maxTier) {
            progress++;
            while (progress >= activitiesPerTier && tier < maxTier) {
                progress -= activitiesPerTier;
                tier++;
            }
        }
        return new Update(tier, progress, tier > previousTier);
    }

    public int getTier(long now) {
        expireIfNeeded(now);
        return tier;
    }

    public int getProgress(long now) {
        expireIfNeeded(now);
        return progress;
    }

    public int getActivitiesPerTier() {
        return activitiesPerTier;
    }

    private void expireIfNeeded(long now) {
        if (lastActivityAt > 0L && now - lastActivityAt > timeoutMs) {
            tier = 1;
            progress = 0;
            lastActivityAt = 0L;
        }
    }

    public record Update(int tier, int progress, boolean tierIncreased) { }
}
