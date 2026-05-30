package me.nakilex.levelplugin.npc.dialog.model;

@FunctionalInterface
public interface DialogueModifier {
    void apply(InteractionContext context);
}
