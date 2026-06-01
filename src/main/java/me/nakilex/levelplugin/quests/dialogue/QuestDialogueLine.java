package me.nakilex.levelplugin.quests.dialogue;

import java.util.Objects;

/**
 * A single timed line in a linear quest dialogue.
 *
 * @param speakerName  name displayed before the dialogue text
 * @param text         full text revealed by the typing animation
 * @param typingMillis time spent revealing the text
 * @param waitMillis   time the full text remains visible before automatically advancing
 */
public record QuestDialogueLine(String speakerName, String text, long typingMillis, long waitMillis) {
    public QuestDialogueLine {
        speakerName = Objects.requireNonNull(speakerName, "speakerName");
        text = Objects.requireNonNull(text, "text");
        if (typingMillis < 0) {
            throw new IllegalArgumentException("typingMillis cannot be negative");
        }
        if (waitMillis < 0) {
            throw new IllegalArgumentException("waitMillis cannot be negative");
        }
    }
}
