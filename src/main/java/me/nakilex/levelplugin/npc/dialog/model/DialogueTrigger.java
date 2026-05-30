package me.nakilex.levelplugin.npc.dialog.model;

@FunctionalInterface
public interface DialogueTrigger {
    void execute(InteractionContext context);
}
