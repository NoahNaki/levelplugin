package me.nakilex.levelplugin.luxbridge.model;

import java.util.Map;

public record LuxDialogue(
        String id,
        int typingSpeed,
        double range,
        String effect,
        boolean answerNumbers,
        boolean preventExit,
        boolean preventSkip,
        boolean characterNameEnabled,
        boolean characterImageEnabled,
        boolean backgroundFogEnabled,
        boolean npcFocus,
        boolean saveProgress,
        LuxSoundSpec typingSound,
        LuxSoundSpec selectionSound,
        int nameOffset,
        int nameBackgroundOffset,
        int dialogueBackgroundOffset,
        int dialogueLineOffset,
        int answerBackgroundOffset,
        int answerLineOffset,
        int arrowOffset,
        int characterOffset,
        String characterName,
        String characterBackgroundImage,
        String arrowImage,
        String dialogueBackgroundImage,
        String answerBackgroundImage,
        String nameStartImage,
        String nameMidImage,
        String nameEndImage,
        String fogImage,
        String nameColor,
        String nameBackgroundColor,
        String dialogueColor,
        String dialogueBackgroundColor,
        String answerColor,
        String answerBackgroundColor,
        String characterBackgroundColor,
        String arrowColor,
        String selectedColor,
        String fogColor,
        Map<String, LuxPage> pages
) {
    public LuxPage firstPage() {
        return pages.values().stream().findFirst().orElse(null);
    }
}
