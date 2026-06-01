package me.nakilex.levelplugin.npc.dialog.render;

import me.nakilex.levelplugin.npc.dialog.engine.DialogueConditionEvaluator;

/** State-only renderer extension point for a future custom-font HUD. */
public final class ResourcePackDialogueRenderer extends ActionBarDialogueRenderer {
    public ResourcePackDialogueRenderer(DialogueConditionEvaluator conditions) {
        super(conditions);
    }
}
