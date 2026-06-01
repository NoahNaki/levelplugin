package me.nakilex.levelplugin.quests.dialogue;

import org.bukkit.ChatColor;

import java.util.Objects;

/**
 * A single timed line in a linear quest dialogue.
 *
 * @param speakerName  name displayed before the dialogue text
 * @param text         full text revealed by the typing animation
 * @param typingMillis base time spent revealing the text, before natural punctuation pauses
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

    /**
     * Convert the established {@code Speaker|Text} NPC dialogue format into a timed line.
     */
    public static QuestDialogueLine fromLegacy(String raw, String defaultSpeaker, long millisPerCharacter,
                                               long waitMillis) {
        String value = Objects.requireNonNull(raw, "raw");
        String speaker = Objects.requireNonNullElse(defaultSpeaker, "NPC");
        String message = value;
        int separator = value.indexOf('|');
        if (separator >= 0) {
            speaker = value.substring(0, separator);
            message = value.substring(separator + 1);
        }

        String visibleText = ChatColor.stripColor(message);
        long typingMillis = Math.max(0L, millisPerCharacter) * (visibleText == null ? 0 : visibleText.length());
        return new QuestDialogueLine(speaker, message, typingMillis, waitMillis);
    }
}
