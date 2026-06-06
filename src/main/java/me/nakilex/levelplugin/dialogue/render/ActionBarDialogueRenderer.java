package me.nakilex.levelplugin.dialogue.render;

import me.nakilex.levelplugin.dialogue.model.DialoguePage;
import me.nakilex.levelplugin.utils.ChatFormatter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Static Lux-style actionbar renderer for a single loaded dialogue page.
 */
public class ActionBarDialogueRenderer implements DialogueRenderer {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final String IMAGE_COLOR = "#ffffff";

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
        actionBarSender.sendMiniMessage(player, renderMiniMessage(context));
    }

    public String renderMiniMessage(DialogueRenderContext context) {
        if (context == null || context.page() == null) {
            return "";
        }

        StringBuilder hud = new StringBuilder();
        if (context.fogEnabled()) {
            hud.append(imageLayer(IMAGE_COLOR, 0, DialogueGlyphs.FOG, DialogueGlyphs.FOG_WIDTH));
        }

        hud.append(backgroundLayer(
                IMAGE_COLOR,
                context.dialogueBackgroundOffsetPixels(),
                DialogueGlyphs.DIALOGUE_BACKGROUND,
                DialogueGlyphs.DIALOGUE_WIDTH
        ));

        if (context.characterBoxEnabled()) {
            hud.append(imageLayer(
                    IMAGE_COLOR,
                    context.characterOffsetPixels(),
                    DialogueGlyphs.CHARACTER_BACKGROUND,
                    DialogueGlyphs.CHARACTER_WIDTH
            ));
        }

        if (context.nameBoxEnabled() && context.characterName() != null && !context.characterName().isBlank()) {
            hud.append(nameBoxLayer(context.characterName(), context));
            hud.append(textLayer(context.nameTextOffsetPixels(), context.nameColor(), context.characterName(),
                    DialogueGlyphs.DEFAULT_TEXT_FONT));
        }

        List<String> lines = context.page().lines();
        for (int i = 0; i < Math.min(lines.size(), 4); i++) {
            hud.append(dialogueLineLayer(i + 1, context.textColor(), lines.get(i), context));
        }

        if (context.page().steadyInfoLine() != null && !context.page().steadyInfoLine().isBlank()) {
            hud.append(infoLine(context.page().steadyInfoLine(), context));
        }
        return hud.toString();
    }

    public Component render(DialogueRenderContext context) {
        return MINI_MESSAGE.deserialize(renderMiniMessage(context));
    }

    private String imageLayer(String color, int offset, String glyph, int width) {
        return colorOpen(color)
                + offset(offset)
                + font(DialogueGlyphs.DIALOGUE_FONT_TAG, glyph)
                + offset(-offset - width)
                + colorClose();
    }

    private String backgroundLayer(String color, int offset, String glyph, int width) {
        return colorOpen(color)
                + offset(offset - width)
                + font(DialogueGlyphs.DIALOGUE_FONT_TAG, glyph)
                + offset(-offset - width)
                + colorClose();
    }

    private String nameBoxLayer(String characterName, DialogueRenderContext context) {
        int nameWidth = DialogueTextWidth.width(characterName);
        int midRepeats = Math.max(1, (int) Math.ceil(nameWidth / 2.0));
        String glyph = DialogueGlyphs.NAME_START + DialogueGlyphs.NAME_MID.repeat(midRepeats) + DialogueGlyphs.NAME_END;
        int boxWidth = DialogueGlyphs.NAME_START_WIDTH
                + (midRepeats * DialogueGlyphs.NAME_MID_WIDTH)
                + DialogueGlyphs.NAME_END_WIDTH;
        return imageLayer(IMAGE_COLOR, context.nameBackgroundOffsetPixels(), glyph, boxWidth);
    }

    private String dialogueLineLayer(int lineNumber, String color, String text, DialogueRenderContext context) {
        int offset = context.dialogueTextOffsetPixels();
        int textWidth = DialogueTextWidth.width(text);
        return offset(offset)
                + dialogueLine(lineNumber, color, text)
                + offset(-offset - textWidth);
    }

    private String dialogueLine(int lineNumber, String color, String text) {
        String font = DialogueGlyphs.LINE_FONT_PREFIX + lineNumber;
        return colorOpen(color)
                + font(font, escapeMiniMessage(text))
                + colorClose();
    }

    private String infoLine(String infoLine, DialogueRenderContext context) {
        String text = font(DialogueGlyphs.DIALOGUE_FONT_TAG, DialogueGlyphs.ARROW)
                + text(context.infoColor(), " " + infoLine, DialogueGlyphs.DEFAULT_TEXT_FONT);
        int textWidth = DialogueGlyphs.ARROW_WIDTH + DialogueTextWidth.width(" " + infoLine);
        int offset = context.infoTextOffsetPixels();
        return offset(offset) + text + offset(-offset - textWidth);
    }

    private String textLayer(int offset, String color, String text, String font) {
        String safeText = text == null ? "" : text;
        int textWidth = DialogueTextWidth.width(safeText);
        return offset(offset)
                + text(color, safeText, font)
                + offset(-offset - textWidth);
    }

    private String text(String color, String text, String font) {
        return colorOpen(color)
                + font(font, escapeMiniMessage(text))
                + colorClose();
    }

    private String offset(int pixels) {
        if (pixels == 0) {
            return "";
        }
        String glyph = pixels < 0 ? DialogueOffsetGlyphs.NEGATIVE_ONE_PIXEL : DialogueOffsetGlyphs.POSITIVE_ONE_PIXEL;
        return font(DialogueGlyphs.OFFSET_FONT_TAG, glyph.repeat(Math.abs(pixels)));
    }

    private String font(String font, String content) {
        return "<font:" + font + ">" + content + "</font>";
    }

    private String colorOpen(String color) {
        return "<color:" + normalizeMiniMessageColor(color) + ">";
    }

    private String colorClose() {
        return "</color>";
    }

    private String normalizeMiniMessageColor(String color) {
        if (color == null || color.isBlank()) {
            return "#ffffff";
        }
        return color.trim();
    }

    private String escapeMiniMessage(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("<", "\\<");
    }

    @Deprecated
    private Component dialogueGlyph(String glyph) {
        return Component.text(glyph).font(DialogueGlyphs.DIALOGUE_FONT);
    }

    @Deprecated
    private Component coloredGlyph(String color, Key font, String glyph) {
        return Component.text(glyph).font(font).color(parseColor(color));
    }

    @Deprecated
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
