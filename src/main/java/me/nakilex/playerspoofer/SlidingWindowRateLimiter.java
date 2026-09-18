package me.nakilex.playerspoofer;

import java.util.ArrayDeque;
import java.util.Deque;

final class SlidingWindowRateLimiter {
    private final int perMinute;
    private final int perHour;
    private final int perDay;
    private final Deque<Long> timestamps = new ArrayDeque<>();

    SlidingWindowRateLimiter(int perMinute, int perHour, int perDay) {
        this.perMinute = Math.max(0, perMinute);
        this.perHour = Math.max(0, perHour);
        this.perDay = Math.max(0, perDay);
    }

    synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();
        trim(now);
        if (perMinute > 0 && countSince(now - 60_000L) >= perMinute) return false;
        if (perHour > 0 && countSince(now - 3_600_000L) >= perHour) return false;
        if (perDay > 0 && timestamps.size() >= perDay) return false;
        timestamps.addLast(now);
        return true;
    }

    synchronized int usedLastMinute() {
        long now = System.currentTimeMillis();
        trim(now);
        return countSince(now - 60_000L);
    }

    synchronized int usedLastHour() {
        long now = System.currentTimeMillis();
        trim(now);
        return countSince(now - 3_600_000L);
    }

    synchronized int usedLastDay() {
        long now = System.currentTimeMillis();
        trim(now);
        return timestamps.size();
    }

    private int countSince(long cutoff) {
        int count = 0;
        for (Long ts : timestamps) if (ts >= cutoff) count++;
        return count;
    }

    private void trim(long now) {
        long cutoff = now - 86_400_000L;
        while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) timestamps.removeFirst();
    }
}
