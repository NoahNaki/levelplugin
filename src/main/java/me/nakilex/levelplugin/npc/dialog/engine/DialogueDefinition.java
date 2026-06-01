package me.nakilex.levelplugin.npc.dialog.engine;

import org.bukkit.SoundCategory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DialogueDefinition(String id, String startPage, Map<String, DialoguePage> pages,
                                 int typingSpeedTicks, double range, boolean preventSkip,
                                 boolean preventExit, DialogueEffect effect, boolean answerNumbers,
                                 DialogueSound typingSound, DialogueSound selectionSound,
                                 DialogueSound confirmSound) {
    public static final int DEFAULT_TYPING_SPEED_TICKS = 1;
    public static final double DEFAULT_RANGE = 5.0;
    public static final DialogueSound DEFAULT_SELECTION_SOUND = new DialogueSound("minecraft:ui.button.click", SoundCategory.MASTER, 1f, 1f);
    public static final DialogueSound DEFAULT_CONFIRM_SOUND = new DialogueSound("minecraft:ui.button.click", SoundCategory.MASTER, 1f, 1f);

    public DialogueDefinition {
        pages = pages == null ? Map.of() : Map.copyOf(pages);
        typingSpeedTicks = Math.max(1, typingSpeedTicks);
        range = Math.max(0.0, range);
        effect = effect == null ? DialogueEffect.SLOWNESS : effect;
    }

    public DialogueDefinition(String id, String startPage, Map<String, DialoguePage> pages) {
        this(id, startPage, pages, DEFAULT_TYPING_SPEED_TICKS, DEFAULT_RANGE, false, false,
                DialogueEffect.SLOWNESS, false, null, DEFAULT_SELECTION_SOUND, DEFAULT_CONFIRM_SOUND);
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
