package me.nakilex.levelplugin.dialogue.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable data for a dialogue page. Rendering, actions, and conditions are handled elsewhere.
 */
public record DialoguePage(
        String id,
        List<String> lines,
        String typingInfoLine,
        String steadyInfoLine,
        List<String> gotoTargets,
        Integer timer,
        List<String> preActions,
        List<String> postActions,
        List<String> exitActions,
        Map<String, DialogueAnswer> answers
) {
    public DialoguePage {
        id = id == null ? "" : id;
        lines = lines == null ? List.of() : List.copyOf(lines);
        gotoTargets = gotoTargets == null ? List.of() : List.copyOf(gotoTargets);
        preActions = preActions == null ? List.of() : List.copyOf(preActions);
        postActions = postActions == null ? List.of() : List.copyOf(postActions);
        exitActions = exitActions == null ? List.of() : List.copyOf(exitActions);
        answers = answers == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(answers));
    }

    public String firstGotoTarget() {
        return gotoTargets.isEmpty() ? null : gotoTargets.get(0);
    }
}
