package me.nakilex.levelplugin.dialogue.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record DialogueOffsets(Map<String, Object> values) {
    public DialogueOffsets {
        values = values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static DialogueOffsets empty() {
        return new DialogueOffsets(Map.of());
    }
}
