package me.nakilex.levelplugin.dialogue.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Raw immutable Settings plus Character data from Lux-style dialogue YAML.
 */
public record DialogueSettings(Map<String, Object> values, Map<String, Object> character) {
    public DialogueSettings {
        values = values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
        character = character == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(character));
    }

    public static DialogueSettings empty() {
        return new DialogueSettings(Map.of(), Map.of());
    }
}
