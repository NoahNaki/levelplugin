package me.nakilex.levelplugin.dialogue.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record DialogueImages(Map<String, Object> values) {
    public DialogueImages {
        values = values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static DialogueImages empty() {
        return new DialogueImages(Map.of());
    }
}
