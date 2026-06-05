package me.nakilex.levelplugin.dialogue.render;

import me.nakilex.levelplugin.dialogue.model.DialoguePage;
import me.nakilex.levelplugin.utils.ChatFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Static Lux-style actionbar renderer for a single loaded dialogue page.
 */
public class ActionBarDialogueRenderer implements DialogueRenderer {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final int BACKGROUND_REWIND_PIXELS = -220;
    private static final int CHARACTER_REWIND_PIXELS = -205;
    private static final int NAME_MID_REPEAT_PADDING = 2;
    private static final String LINE_SEPARATOR = "  ";

    @Override
    public void render(Player player, DialogueRenderContext context) {
        if (player == null || context == null || context.page() == null) {
            return;
        }
        player.sendActionBar(render(context));
    }

    public Component render(DialogueRenderContext context) {
        if (context == null || context.page() == null) {
            return Component.empty();
        }

        Component hud = Component.empty();
        if (context.fogEnabled()) {
            hud = hud.append(dialogueGlyph(DialogueGlyphs.FOG))
                    .append(DialogueOffsetGlyphs.component(BACKGROUND_REWIND_PIXELS));
        }

        hud = hud.append(dialogueGlyph(DialogueGlyphs.DIALOGUE_BACKGROUND));
        if (context.characterBoxEnabled()) {
            hud = hud.append(DialogueOffsetGlyphs.component(CHARACTER_REWIND_PIXELS))
                    .append(dialogueGlyph(DialogueGlyphs.CHARACTER_BACKGROUND));
        }

        if (context.nameBoxEnabled() && context.characterName() != null && !context.characterName().isBlank()) {
            hud = hud.append(DialogueOffsetGlyphs.component(context.nameOffsetPixels()))
                    .append(nameBox(context.characterName()))
                    .append(DialogueOffsetGlyphs.component(-nameBoxPixelWidth(context.characterName()) + 8))
                    .append(coloredText(context.nameColor() + context.characterName()));
        }

        hud = hud.append(DialogueOffsetGlyphs.component(context.contentOffsetPixels()))
                .append(pageText(context.page(), context));
        return hud;
    }

    private Component pageText(DialoguePage page, DialogueRenderContext context) {
        Component text = Component.empty();
        List<String> lines = page.lines();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                text = text.append(DialogueOffsetGlyphs.component(context.lineSpacingPixels()))
                        .append(coloredText(LINE_SEPARATOR));
            }
            text = text.append(coloredText(context.textColor() + lines.get(i)));
        }

        if (page.steadyInfoLine() != null && !page.steadyInfoLine().isBlank()) {
            if (!lines.isEmpty()) {
                text = text.append(coloredText(LINE_SEPARATOR));
            }
            text = text.append(dialogueGlyph(DialogueGlyphs.ARROW))
                    .append(coloredText(" " + context.infoColor() + page.steadyInfoLine()));
        }
        return text;
    }

    private Component nameBox(String characterName) {
        int midRepeats = Math.max(1, characterName.length() + NAME_MID_REPEAT_PADDING);
        return dialogueGlyph(DialogueGlyphs.NAME_START + DialogueGlyphs.NAME_MID.repeat(midRepeats) + DialogueGlyphs.NAME_END);
    }

    private int nameBoxPixelWidth(String characterName) {
        int midRepeats = Math.max(1, characterName.length() + NAME_MID_REPEAT_PADDING);
        return 2 + midRepeats;
    }

    private Component dialogueGlyph(String glyph) {
        return Component.text(glyph).font(DialogueGlyphs.DIALOGUE_FONT);
    }

    private Component coloredText(String text) {
        return LEGACY.deserialize(ChatFormatter.colorize(text == null ? "" : text));
    }
}
