package me.nakilex.levelplugin.npc.dialog.render;

import me.nakilex.levelplugin.npc.dialog.engine.DialogueConditionEvaluator;
import net.kyori.adventure.key.Key;

/**
 * State-only extension point for the future custom-font HUD.
 * TODO: compose these glyphs from session state once the resource-pack artwork is finalized.
 */
public final class ResourcePackDialogueRenderer extends ActionBarDialogueRenderer {
    public static final Key DIALOGUE_FONT = Key.key("levelplugin:dialogue");
    public static final String GLYPH_DIALOGUE_BOX = "\uE001";
    public static final String GLYPH_ANSWER_BOX = "\uE002";
    public static final String GLYPH_SELECTOR = "\uE003";
    public static final String GLYPH_FOG = "\uE004";
    public static final String GLYPH_DEFAULT_CHARACTER = "\uE100";

    public ResourcePackDialogueRenderer(DialogueConditionEvaluator conditions) {
        super(conditions);
    }

    public String shift(int pixels) {
        // TODO: convert positive and negative pixel offsets into custom spacing glyphs.
        return "";
    }
}
