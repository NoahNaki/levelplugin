package me.nakilex.levelplugin.dialogue.model;

import java.util.List;

/**
 * Immutable data for a selectable dialogue answer.
 */
public record DialogueAnswer(
        String id,
        String text,
        List<String> gotoTargets,
        List<String> replyMessages,
        DialogueSoundSpec sound,
        List<String> conditions,
        List<String> actions
) {
    public DialogueAnswer {
        id = id == null ? "" : id;
        gotoTargets = gotoTargets == null ? List.of() : List.copyOf(gotoTargets);
        replyMessages = replyMessages == null ? List.of() : List.copyOf(replyMessages);
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        sound = sound == null ? DialogueSoundSpec.empty() : sound;
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    public String firstGotoTarget() {
        return gotoTargets.isEmpty() ? null : gotoTargets.get(0);
    }
}
