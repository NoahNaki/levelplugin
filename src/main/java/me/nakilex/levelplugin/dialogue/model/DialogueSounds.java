package me.nakilex.levelplugin.dialogue.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record DialogueSounds(Map<String, Object> values) {
    public DialogueSounds {
        values = values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static DialogueSounds empty() {
        return new DialogueSounds(Map.of());
    }
}
