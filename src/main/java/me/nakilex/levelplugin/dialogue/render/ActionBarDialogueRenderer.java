package me.nakilex.levelplugin.dialogue.render;

import me.nakilex.levelplugin.dialogue.model.DialoguePage;
import me.nakilex.levelplugin.utils.ChatFormatter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Static Lux-style actionbar renderer for a single loaded dialogue page.
 */
public class ActionBarDialogueRenderer implements DialogueRenderer {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final String IMAGE_COLOR = "#ffffff";
    private static final String LINE_SEPARATOR = "  ";

    private final DialogueActionBarSender actionBarSender;

    public ActionBarDialogueRenderer() {
        this(new DialogueActionBarSender());
    }

    public ActionBarDialogueRenderer(DialogueActionBarSender actionBarSender) {
        this.actionBarSender = actionBarSender == null ? new DialogueActionBarSender() : actionBarSender;
    }

    @Override
    public void render(Player player, DialogueRenderContext context) {
        if (player == null || context == null || context.page() == null) {
            return;
        }
        actionBarSender.send(player, render(context));
    }

    public Component render(DialogueRenderContext context) {
        if (context == null || context.page() == null) {
            return Component.empty();
        }

        Component hud = Component.empty();
        if (context.fogEnabled()) {
            hud = hud.append(imageLayer(IMAGE_COLOR, 0, DialogueGlyphs.FOG, DialogueGlyphs.FOG_WIDTH));
        }

        hud = hud.append(imageLayer(
                IMAGE_COLOR,
                context.dialogueBackgroundOffsetPixels(),
                DialogueGlyphs.DIALOGUE_BACKGROUND,
                DialogueGlyphs.DIALOGUE_WIDTH
        ));

        if (context.characterBoxEnabled()) {
            hud = hud.append(imageLayer(
                    IMAGE_COLOR,
                    context.characterOffsetPixels(),
                    DialogueGlyphs.CHARACTER_BACKGROUND,
                    DialogueGlyphs.CHARACTER_WIDTH
            ));
        }

        if (context.nameBoxEnabled() && context.characterName() != null && !context.characterName().isBlank()) {
            hud = hud.append(nameBoxLayer(context.characterName(), context))
                    .append(textLayer(context.nameTextOffsetPixels(), context.nameColor(), context.characterName()));
        }

        List<String> lines = context.page().lines();
        if (!lines.isEmpty()) {
            hud = hud.append(dialogueLine(1, lines.get(0), context));
        }

        if (context.page().steadyInfoLine() != null && !context.page().steadyInfoLine().isBlank()) {
            hud = hud.append(infoLine(context.page().steadyInfoLine(), context));
        }
        return hud;
    }

    private Component imageLayer(String color, int offset, String glyph, int glyphWidth) {
        return offset(offset)
                .append(coloredGlyph(color, DialogueGlyphs.DIALOGUE_FONT, glyph))
                .append(offset(-offset - glyphWidth));
    }

    private Component nameBoxLayer(String characterName, DialogueRenderContext context) {
        int nameWidth = DialogueTextWidth.width(characterName);
        int midRepeats = Math.max(1, (int) Math.ceil(nameWidth / 2.0));
        String glyph = DialogueGlyphs.NAME_START + DialogueGlyphs.NAME_MID.repeat(midRepeats) + DialogueGlyphs.NAME_END;
        int boxWidth = DialogueGlyphs.NAME_START_WIDTH
                + (midRepeats * DialogueGlyphs.NAME_MID_WIDTH)
                + DialogueGlyphs.NAME_END_WIDTH;
        return imageLayer(IMAGE_COLOR, context.nameBackgroundOffsetPixels(), glyph, boxWidth);
    }

    private Component dialogueLine(int lineNumber, String line, DialogueRenderContext context) {
        if (lineNumber != 1) {
            return Component.empty();
        }
        int offset = context.dialogueTextOffsetPixels();
        return textLayer(offset, context.textColor(), line);
    }

    private Component infoLine(String infoLine, DialogueRenderContext context) {
        Component text = dialogueGlyph(DialogueGlyphs.ARROW)
                .append(coloredText(" " + context.infoColor() + infoLine));
        int textWidth = DialogueGlyphs.ARROW_WIDTH + DialogueTextWidth.width(" " + infoLine);
        int offset = context.infoTextOffsetPixels();
        return offset(offset)
                .append(text)
                .append(offset(-offset - textWidth));
    }

    private Component textLayer(int offset, String color, String text) {
        String safeText = text == null ? "" : text;
        int textWidth = DialogueTextWidth.width(safeText);
        return offset(offset)
                .append(coloredText(color + safeText))
                .append(offset(-offset - textWidth));
    }

    private Component offset(int pixels) {
        return DialogueOffsetGlyphs.component(pixels);
    }

    private Component dialogueGlyph(String glyph) {
        return Component.text(glyph).font(DialogueGlyphs.DIALOGUE_FONT);
    }

    private Component coloredGlyph(String color, Key font, String glyph) {
        return Component.text(glyph).font(font).color(parseColor(color));
    }

    private Component coloredText(String text) {
        return LEGACY.deserialize(ChatFormatter.colorize(text == null ? "" : text));
    }

    private TextColor parseColor(String color) {
        if (color == null || color.isBlank()) {
            return NamedTextColor.WHITE;
        }
        TextColor parsed = TextColor.fromHexString(color.trim());
        return parsed == null ? NamedTextColor.WHITE : parsed;
    }
}
