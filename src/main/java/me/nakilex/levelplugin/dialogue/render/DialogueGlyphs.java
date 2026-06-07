package me.nakilex.levelplugin.dialogue.render;

import net.kyori.adventure.key.Key;

/**
 * Glyph constants from the levelplugin_dialogue HUD fonts.
 */
public final class DialogueGlyphs {
    public static final Key DIALOGUE_FONT = Key.key("levelplugin_dialogue", "dialogue");

    public static final String DIALOGUE_FONT_TAG = "levelplugin_dialogue:dialogue";
    public static final String DIALOGUE_BACKGROUND_FONT = DIALOGUE_FONT_TAG;
    public static final String ANSWER_BACKGROUND_FONT = DIALOGUE_FONT_TAG;
    public static final String CHARACTER_BACKGROUND_FONT = DIALOGUE_FONT_TAG;
    public static final String HAND_FONT = DIALOGUE_FONT_TAG;
    public static final String ARROW_FONT = HAND_FONT;
    public static final String FOG_FONT = DIALOGUE_FONT_TAG;
    public static final String NAME_BOX_FONT = DIALOGUE_FONT_TAG;
    public static final String KINGDOM_DIALOGUE_FONT = DIALOGUE_FONT_TAG;
    public static final String KINGDOM_ANSWER_FONT = DIALOGUE_FONT_TAG;
    public static final String KINGDOM_CHARACTER_FONT = DIALOGUE_FONT_TAG;
    public static final String KINGDOM_HAND_FONT = DIALOGUE_FONT_TAG;
    public static final String KINGDOM_NAME_BOX_FONT = DIALOGUE_FONT_TAG;
    public static final String OFFSET_FONT_TAG = "levelplugin_dialogue:offset_chars";
    public static final String DEFAULT_TEXT_FONT = "levelplugin_dialogue:levelplugin_dialogue_default";
    public static final String LINE_FONT_PREFIX = "levelplugin_dialogue:levelplugin_dialogue_line_";
    public static final String ANSWER_FONT_PREFIX = "levelplugin_dialogue:levelplugin_dialogue_answer_";
    public static final String CHARACTER_NAME_FONT = "levelplugin_dialogue:levelplugin_dialogue_character_name";
    public static final String INFO_FONT = "levelplugin_dialogue:levelplugin_dialogue_info";

    public static final String DIALOGUE_BACKGROUND = "\uE100";
    public static final String ANSWER_BACKGROUND = "\uE101";
    public static final String ARROW = "\uE102";
    public static final String HAND = ARROW;
    public static final String NAME_START = "\uE103";
    public static final String NAME_MID = "\uE104";
    public static final String NAME_END = "\uE105";
    public static final String FOG = "\uE106";
    public static final String CHARACTER_BACKGROUND = "\uE107";
    public static final String KINGDOM_HAND = "\uE108";
    public static final String KINGDOM_DIALOGUE = "\uE109";
    public static final String KINGDOM_ANSWER = "\uE10A";
    public static final String KINGDOM_CHARACTER = "\uE10B";
    public static final String KINGDOM_NAME_START = "\uE10C";
    public static final String KINGDOM_NAME_MID = "\uE10D";
    public static final String KINGDOM_NAME_END = "\uE10E";

    public static final int DIALOGUE_WIDTH = 209;
    public static final int CHARACTER_WIDTH = 32;
    public static final int ANSWER_WIDTH = 134;
    public static final int ARROW_WIDTH = 14;
    public static final int HAND_WIDTH = ARROW_WIDTH;
    public static final int NAME_START_WIDTH = 3;
    public static final int NAME_MID_WIDTH = 2;
    public static final int NAME_END_WIDTH = 3;
    public static final int FOG_WIDTH = 256;

    private DialogueGlyphs() {
    }
}
