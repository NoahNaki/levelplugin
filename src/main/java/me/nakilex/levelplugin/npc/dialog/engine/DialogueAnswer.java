package me.nakilex.levelplugin.npc.dialog.engine;

import java.util.List;

public record DialogueAnswer(String text, List<String> gotoTargets, List<String> actions,
                             List<String> conditions, List<String> replies) {
    public DialogueAnswer {
        gotoTargets = copy(gotoTargets);
        actions = copy(actions);
        conditions = copy(conditions);
        replies = copy(replies);
    }

    public DialogueAnswer(String text) {
        this(text, List.of(), List.of(), List.of(), List.of());
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
