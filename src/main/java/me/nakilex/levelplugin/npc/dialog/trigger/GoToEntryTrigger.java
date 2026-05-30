package me.nakilex.levelplugin.npc.dialog.trigger;

import me.nakilex.levelplugin.npc.dialog.DialogueInteraction;
import me.nakilex.levelplugin.npc.dialog.model.DialogueTrigger;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;

/** Routes the current interaction directly to another entry in its dialogue graph. */
public final class GoToEntryTrigger implements DialogueTrigger {
    private final String targetEntryId;

    public GoToEntryTrigger(String targetEntryId) { this.targetEntryId = targetEntryId; }

    @Override public void execute(InteractionContext context) { context.interaction().ifPresent(interaction -> interaction.goTo(targetEntryId)); }
    @Override public void execute(DialogueInteraction interaction, InteractionContext context) { interaction.goTo(targetEntryId); }
}
