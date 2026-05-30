package me.nakilex.levelplugin.npc.dialog.trigger;

import me.nakilex.levelplugin.npc.dialog.DialogueInteraction;
import me.nakilex.levelplugin.npc.dialog.model.DialogueTrigger;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;

/** Requests a safe end after the current entry or option has completed. */
public final class EndInteractionTrigger implements DialogueTrigger {
    @Override public void execute(InteractionContext context) { context.interaction().ifPresent(DialogueInteraction::requestEnd); }
    @Override public void execute(DialogueInteraction interaction, InteractionContext context) { interaction.requestEnd(); }
}
