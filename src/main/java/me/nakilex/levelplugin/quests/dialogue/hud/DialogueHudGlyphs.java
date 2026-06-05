package me.nakilex.levelplugin.quests.dialogue.hud;

import me.nakilex.levelplugin.utils.glyph.OffsetGlyphs;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

/** Placeholder glyph catalogue for a future LuxDialogues-style dialogue HUD resource pack. */
public final class DialogueHudGlyphs {
    public static final Key DIALOGUE_FONT = Key.key("levelplugin_dialogue", "dialogue");
    public static final Key OFFSET_FONT = Key.key("levelplugin_dialogue", "offset_chars");
    public static final Key VANILLA_FONT = Key.key("minecraft", "default");

    public static final char DIALOGUE_BACKGROUND = '\uE100';
    public static final char ANSWER_BACKGROUND = '\uE101';
    public static final char SELECTOR_ARROW = '\uE102';
    public static final char NAMEPLATE_LEFT = '\uE103';
    public static final char NAMEPLATE_MIDDLE = '\uE104';
    public static final char NAMEPLATE_RIGHT = '\uE105';
    public static final char FOG_BACKGROUND = '\uE106';

    private DialogueHudGlyphs() { }

    /**
     * Builds a private-use HUD glyph with the dialogue font explicitly applied.
     * Minecraft clients do not resolve bitmap glyphs from arbitrary private Unicode characters unless the
     * component carries the matching font key.
     */
    public static Component glyph(char glyph) {
        return Component.text(Character.toString(glyph)).font(DIALOGUE_FONT);
    }

    public static Component background() { return glyph(DIALOGUE_BACKGROUND); }

    public static Component answerBackground() { return glyph(ANSWER_BACKGROUND); }

    public static Component selector() { return glyph(SELECTOR_ARROW); }

    public static Component nameplateLeft() { return glyph(NAMEPLATE_LEFT); }

    public static Component nameplateMiddle() { return glyph(NAMEPLATE_MIDDLE); }

    public static Component nameplateRight() { return glyph(NAMEPLATE_RIGHT); }

    public static Component fogBackground() { return glyph(FOG_BACKGROUND); }

    public static Component offset(int pixels) { return OffsetGlyphs.component(pixels, OFFSET_FONT); }

    /** Resets normal readable text back to Minecraft's default font when it is adjacent to HUD glyphs. */
    public static Component defaultFont(Component component) {
        return component == null ? Component.empty() : component.font(VANILLA_FONT);
    }

    public static Component defaultText(String text, TextColor color) {
        return Component.text(text, color).font(VANILLA_FONT);
    }
}
