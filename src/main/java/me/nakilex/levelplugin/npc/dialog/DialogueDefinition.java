package me.nakilex.levelplugin.npc.dialog;

import java.util.List;
import me.nakilex.levelplugin.npc.dialog.entry.DialogueEntry;
import me.nakilex.levelplugin.npc.dialog.model.DialogNpcRef;
import me.nakilex.levelplugin.npc.dialog.model.DialogueCriteria;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;

public record DialogueDefinition(String id, String name, DialogNpcRef npc, int priority,
                                 List<DialogueCriteria> criteria, List<DialogueEntry> entries) {
    public DialogueDefinition {
        criteria = List.copyOf(criteria);
        entries = List.copyOf(entries);
    }

    public boolean matches(InteractionContext context) {
        return criteria.stream().allMatch(criteria -> criteria.matches(context))
                && entries.stream().anyMatch(entry -> entry.matches(context));
    }
}
