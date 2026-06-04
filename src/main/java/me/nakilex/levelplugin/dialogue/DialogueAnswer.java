package me.nakilex.levelplugin.dialogue;

import java.util.List;
import java.util.Objects;

public record DialogueAnswer(
        String id,
        String text,
        String gotoPageId,
        List<String> replyLines,
        String condition,
        DialogueSound sound,
        List<String> actions
) {
    public DialogueAnswer {
        id = Objects.requireNonNull(id, "id");
        text = Objects.requireNonNullElse(text, "");
        replyLines = replyLines == null ? List.of() : List.copyOf(replyLines);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    public static DialogueAnswer of(String id, String text, String gotoPageId) {
        return new DialogueAnswer(id, text, gotoPageId, List.of(), null, DialogueSound.UI_SELECT, List.of());
    }
}
