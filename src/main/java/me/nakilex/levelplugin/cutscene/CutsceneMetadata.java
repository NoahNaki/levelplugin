package me.nakilex.levelplugin.cutscene;

import java.util.Collections;
import java.util.List;

/** Additional descriptive data for a cutscene beyond its raw frames. */
public final class CutsceneMetadata {
    private final String description;
    private final List<String> tags;
    private final boolean autoStart;
    private final List<String> endCommands;

    public CutsceneMetadata(String description, List<String> tags, boolean autoStart, List<String> endCommands) {
        this.description = description == null ? "" : description;
        this.tags = tags == null ? List.of() : List.copyOf(tags);
        this.autoStart = autoStart;
        this.endCommands = endCommands == null ? List.of() : List.copyOf(endCommands);
    }

    public String description() {
        return description;
    }

    public List<String> tags() {
        return tags;
    }

    public boolean autoStart() {
        return autoStart;
    }

    public List<String> endCommands() {
        return endCommands;
    }

    public boolean isEmpty() {
        return description.isEmpty() && tags.isEmpty() && !autoStart && endCommands.isEmpty();
    }

    public java.util.Map<String, Object> toMap() {
        if (isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        if (!description.isEmpty()) map.put("description", description);
        if (!tags.isEmpty()) map.put("tags", tags);
        if (autoStart) map.put("autoStart", true);
        if (!endCommands.isEmpty()) map.put("endCommands", endCommands);
        return map;
    }
}
