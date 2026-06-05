package me.nakilex.levelplugin.dialogue.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record DialogueColors(Map<String, Object> values) {
    public DialogueColors {
        values = values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static DialogueColors empty() {
        return new DialogueColors(Map.of());
    }
}
