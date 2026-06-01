package me.nakilex.levelplugin.npc.dialog.engine;

import java.util.List;

public record DialoguePage(String id, List<String> lines, List<String> gotoTargets,
                           List<String> preActions, List<String> postActions, List<String> exitActions,
                           List<DialogueAnswer> answers) {
    public DialoguePage {
        lines = copy(lines);
        gotoTargets = copy(gotoTargets);
        preActions = copy(preActions);
        postActions = copy(postActions);
        exitActions = copy(exitActions);
        answers = answers == null ? List.of() : List.copyOf(answers);
    }

    public DialoguePage(String id, List<String> lines, List<String> gotoTargets) {
        this(id, lines, gotoTargets, List.of(), List.of(), List.of(), List.of());
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
