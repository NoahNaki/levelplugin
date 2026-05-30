package me.nakilex.levelplugin.npc.dialog.model;

@FunctionalInterface
public interface DialogueCriteria {
    boolean matches(InteractionContext context);
}
