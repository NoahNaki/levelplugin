package me.nakilex.levelplugin.dialogue;

import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.dialogue.QuestDialogueLine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record DialogueDefinition(
        String id,
        DialogueSettings settings,
        List<DialogueSound> sounds,
        Map<String, DialoguePage> pages,
        String startPageId,
        String defaultSpeaker
) {
    public DialogueDefinition {
        id = Objects.requireNonNull(id, "id");
        settings = settings == null ? DialogueSettings.defaults() : settings;
        sounds = sounds == null ? List.of() : List.copyOf(sounds);
        pages = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(pages, "pages")));
        if (pages.isEmpty()) throw new IllegalArgumentException("pages cannot be empty");
        if (startPageId == null || startPageId.isBlank()) startPageId = pages.keySet().iterator().next();
        defaultSpeaker = Objects.requireNonNullElse(defaultSpeaker, "NPC");
    }

    public DialoguePage page(String pageId) {
        return pages.get(pageId);
    }

    public static DialogueDefinition fromLegacyLines(String id, String npcName, List<String> lines) {
        LinkedHashMap<String, DialoguePage> pages = new LinkedHashMap<>();
        List<String> safeLines = lines == null || lines.isEmpty() ? List.of(npcName + "|...") : lines;
        for (int i = 0; i < safeLines.size(); i++) {
            String pageId = "line_" + (i + 1);
            String next = i + 1 < safeLines.size() ? "line_" + (i + 2) : null;
            pages.put(pageId, DialoguePage.line(pageId, safeLines.get(i), next));
        }
        return new DialogueDefinition(id, DialogueSettings.defaults(), List.of(DialogueSound.UI_CLICK), pages,
                "line_1", npcName);
    }

    public static DialogueDefinition fromLegacyQuest(Quest quest, String npcName) {
        String id = quest == null ? "legacy_quest" : "quest_" + quest.getId();
        List<String> lines = quest == null ? List.of() : quest.getDialogLines();
        return fromLegacyLines(id, npcName, lines);
    }

    public static DialogueDefinition fromLegacyQuest(String questId, String npcName, List<QuestDialogueLine> lines) {
        List<String> rawLines = lines == null ? List.of() : lines.stream()
                .map(line -> line.speakerName() + "|" + line.text())
                .toList();
        return fromLegacyLines("quest_dialogue_" + questId, npcName, rawLines);
    }
}
