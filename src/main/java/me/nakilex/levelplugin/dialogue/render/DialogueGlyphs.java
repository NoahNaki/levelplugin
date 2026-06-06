package me.nakilex.levelplugin.dialogue.render;

import net.kyori.adventure.key.Key;

/**
 * Glyph constants from the levelplugin_dialogue:dialogue font.
 */
public final class DialogueGlyphs {
    public static final Key DIALOGUE_FONT = Key.key("levelplugin_dialogue", "dialogue");

    public static final String DIALOGUE_BACKGROUND = "\uE100";
    public static final String ANSWER_BACKGROUND = "\uE101";
    public static final String ARROW = "\uE102";
    public static final String NAME_START = "\uE103";
    public static final String NAME_MID = "\uE104";
    public static final String NAME_END = "\uE105";
    public static final String FOG = "\uE106";
    public static final String CHARACTER_BACKGROUND = "\uE107";

    public static final int DIALOGUE_WIDTH = 420;
    public static final int CHARACTER_WIDTH = 64;
    public static final int ANSWER_WIDTH = 220;
    public static final int ARROW_WIDTH = 16;
    public static final int NAME_START_WIDTH = 8;
    public static final int NAME_MID_WIDTH = 2;
    public static final int NAME_END_WIDTH = 8;
    public static final int FOG_WIDTH = 1536;

    private DialogueGlyphs() {
    }
}
