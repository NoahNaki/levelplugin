package me.nakilex.levelplugin.dialogue;

import java.util.List;
import java.util.Objects;

public record DialoguePage(
        String id,
        List<String> lines,
        String gotoPageId,
        List<DialogueAnswer> answers,
        List<String> preActions,
        List<String> postActions,
        List<String> exitActions
) {
    public DialoguePage {
        id = Objects.requireNonNull(id, "id");
        lines = lines == null ? List.of() : List.copyOf(lines);
        answers = answers == null ? List.of() : List.copyOf(answers);
        preActions = preActions == null ? List.of() : List.copyOf(preActions);
        postActions = postActions == null ? List.of() : List.copyOf(postActions);
        exitActions = exitActions == null ? List.of() : List.copyOf(exitActions);
    }

    public static DialoguePage line(String id, String line, String gotoPageId) {
        return new DialoguePage(id, List.of(line), gotoPageId, List.of(), List.of(), List.of(), List.of());
    }
}
