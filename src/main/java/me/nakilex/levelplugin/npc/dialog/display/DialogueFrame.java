package me.nakilex.levelplugin.npc.dialog.display;

/** One rendered state of an animated dialogue line. */
public record DialogueFrame(String speaker, String text, int index, int total, boolean complete) {
    public DialogueFrame {
        speaker = speaker == null || speaker.isBlank() ? "NPC" : speaker;
        text = text == null ? "" : text;
    }
}
