package me.nakilex.levelplugin.utils;

/** Utility helpers for staggering recurring announcement schedules. */
public final class AnnouncementTimingUtil {

    private AnnouncementTimingUtil() {
    }

    /**
     * Compute a deterministic startup offset for a recurring task.
     *
     * @param intervalTicks recurring interval of the task in ticks
     * @param slotIndex zero-based slot index for this task in the group
     * @param slotCount total number of slots in the group
     * @param minimumDelayTicks minimum delay to apply when slot index is non-zero
     * @return initial delay in ticks
     */
    public static long computeInitialDelayTicks(long intervalTicks,
                                                int slotIndex,
                                                int slotCount,
                                                long minimumDelayTicks) {
        if (intervalTicks <= 0L || slotCount <= 1 || slotIndex <= 0) {
            return 0L;
        }

        long clampedMinDelay = Math.max(0L, minimumDelayTicks);
        int clampedSlot = Math.min(Math.max(slotIndex, 0), slotCount - 1);
        long spreadDelay = (intervalTicks * clampedSlot) / slotCount;

        if (spreadDelay <= 0L) {
            return clampedMinDelay;
        }
        return Math.max(spreadDelay, clampedMinDelay);
    }
}
