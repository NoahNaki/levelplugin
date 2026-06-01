package me.nakilex.levelplugin.npc.dialog.engine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DialogueDefinition(String id, String startPage, Map<String, DialoguePage> pages) {
    public DialogueDefinition {
        pages = pages == null ? Map.of() : Map.copyOf(pages);
    }

    public DialoguePage page(String pageId) {
        return pages.get(pageId);
    }

    public static DialogueDefinition linear(String id, List<String> lines) {
        Map<String, DialoguePage> pages = new LinkedHashMap<>();
        for (int index = 0; index < lines.size(); index++) {
            String pageId = "line-" + index;
            List<String> next = index + 1 < lines.size() ? List.of("line-" + (index + 1)) : List.of();
            pages.put(pageId, new DialoguePage(pageId, List.of(lines.get(index)), next));
        }
        return new DialogueDefinition(id, lines.isEmpty() ? null : "line-0", pages);
    }
}
