package me.nakilex.levelplugin.dialogue.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable in-memory representation of one Lux-style dialogue file.
 */
public record DialogueDefinition(
        String id,
        DialogueSettings settings,
        DialogueSounds sounds,
        DialogueOffsets offsets,
        DialogueImages images,
        DialogueColors colors,
        Map<String, DialoguePage> pages
) {
    public DialogueDefinition {
        id = id == null ? "" : id;
        settings = settings == null ? DialogueSettings.empty() : settings;
        sounds = sounds == null ? DialogueSounds.empty() : sounds;
        offsets = offsets == null ? DialogueOffsets.empty() : offsets;
        images = images == null ? DialogueImages.empty() : images;
        colors = colors == null ? DialogueColors.empty() : colors;
        pages = pages == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(pages));
    }
}
