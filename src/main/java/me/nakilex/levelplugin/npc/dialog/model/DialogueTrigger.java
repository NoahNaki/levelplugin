package me.nakilex.levelplugin.npc.dialog.model;

import me.nakilex.levelplugin.npc.dialog.DialogueInteraction;

/** Action executed after an entry or option completes. Existing context-only lambdas remain supported. */
@FunctionalInterface
public interface DialogueTrigger {
    void execute(InteractionContext context);

    default void execute(DialogueInteraction interaction, InteractionContext context) {
        execute(context);
    }
}
