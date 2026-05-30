package me.nakilex.levelplugin.npc.dialog.messenger;

import java.time.Duration;

/** Reusable elapsed-time state for dialogue messengers that reveal text gradually. */
final class TypingAnimation {
    private final Duration duration;
    private Duration elapsed = Duration.ZERO;

    TypingAnimation(Duration duration) {
        this.duration = duration == null || duration.isNegative() ? Duration.ZERO : duration;
    }

    void tick(Duration deltaTime) {
        if (deltaTime != null && !deltaTime.isNegative()) elapsed = elapsed.plus(deltaTime);
    }

    void complete() { elapsed = duration; }

    boolean isComplete(String text) {
        return text == null || text.isEmpty() || elapsed.compareTo(duration) >= 0;
    }

    int visibleCharacters(String text) {
        if (text == null || text.isEmpty()) return 0;
        long typingMillis = duration.toMillis();
        if (typingMillis <= 0L) return text.length();
        return (int) Math.min(text.length(), Math.floor(text.length() * (elapsed.toMillis() / (double) typingMillis)));
    }

    Duration elapsed() { return elapsed; }
    Duration duration() { return duration; }
}
