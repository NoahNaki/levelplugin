package me.nakilex.levelplugin.dialogue.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable sound configuration for a dialogue answer.
 */
public record DialogueSoundSpec(Map<String, Object> values) {
    public DialogueSoundSpec {
        values = values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static DialogueSoundSpec empty() {
        return new DialogueSoundSpec(Map.of());
    }

    public static DialogueSoundSpec ofId(String id) {
        if (id == null || id.isBlank()) {
            return empty();
        }
        return new DialogueSoundSpec(Map.of("id", id));
    }

    public String id() {
        Object id = values.get("id");
        return id == null ? null : String.valueOf(id);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }
}
