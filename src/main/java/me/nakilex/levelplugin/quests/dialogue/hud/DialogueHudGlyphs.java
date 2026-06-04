package me.nakilex.levelplugin.quests.dialogue.hud;

import me.nakilex.levelplugin.utils.glyph.OffsetGlyphs;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

/** Placeholder glyph catalogue for a future LuxDialogues-style dialogue HUD resource pack. */
public final class DialogueHudGlyphs {
    public static final Key DIALOGUE_FONT = Key.key("levelplugin_dialogue:dialogue");
    public static final Key OFFSET_FONT = Key.key("levelplugin_dialogue:offset_chars");

    public static final char DIALOGUE_BACKGROUND = '\uE100';
    public static final char ANSWER_BACKGROUND = '\uE101';
    public static final char SELECTOR_ARROW = '\uE102';
    public static final char NAMEPLATE_LEFT = '\uE103';
    public static final char NAMEPLATE_MIDDLE = '\uE104';
    public static final char NAMEPLATE_RIGHT = '\uE105';
    public static final char FOG_BACKGROUND = '\uE106';

    private DialogueHudGlyphs() { }

    public static Component glyph(char glyph) { return Component.text(glyph).font(DIALOGUE_FONT); }

    public static Component background() { return glyph(DIALOGUE_BACKGROUND); }

    public static Component selector() { return glyph(SELECTOR_ARROW); }

    public static Component offset(int pixels) { return OffsetGlyphs.component(pixels, OFFSET_FONT); }
}
