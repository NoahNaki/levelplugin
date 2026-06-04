package me.nakilex.levelplugin.dialogue;

public record DialogueSettings(
        long typingMillisPerCharacter,
        long waitMillis,
        double maxRangeSquared,
        boolean lockPlayer,
        boolean autoAdvance,
        boolean hideUnavailableAnswers
) {
    public static DialogueSettings defaults() {
        return new DialogueSettings(35L, 3000L, 25.0D, true, true, true);
    }
}
