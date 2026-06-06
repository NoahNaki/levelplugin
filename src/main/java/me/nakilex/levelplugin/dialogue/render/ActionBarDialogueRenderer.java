package me.nakilex.levelplugin.dialogue.render;

import me.nakilex.levelplugin.dialogue.model.DialoguePage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Static Lux-style actionbar renderer for a single loaded dialogue page.
 */
public class ActionBarDialogueRenderer implements DialogueRenderer {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final String IMAGE_COLOR = "#ffffff";
    private static final String DIALOGUE_TEXT_COLOR = "#1e1e1e";

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
            hud.append(dialogueLineLayer(i + 1, lines.get(i), context));
        }

        if (context.page().steadyInfoLine() != null && !context.page().steadyInfoLine().isBlank()) {
            hud.append(infoLine(context.page().steadyInfoLine(), context));
            hud.append(arrowLayer(context));
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
        int nameWidth = DialogueTextWidth.width(DialoguePlaceholderFormatter.plainText(characterName));
        int midRepeats = Math.max(1, (int) Math.ceil(nameWidth / 2.0));
        String glyph = DialogueGlyphs.NAME_START + DialogueGlyphs.NAME_MID.repeat(midRepeats) + DialogueGlyphs.NAME_END;
        int boxWidth = DialogueGlyphs.NAME_START_WIDTH
                + (midRepeats * DialogueGlyphs.NAME_MID_WIDTH)
                + DialogueGlyphs.NAME_END_WIDTH;
        return imageLayer(IMAGE_COLOR, context.nameBackgroundOffsetPixels(), glyph, boxWidth);
    }

    private String dialogueLineLayer(int lineNumber, String text, DialogueRenderContext context) {
        int offset = context.lineOffsetPixels(lineNumber);
        int textWidth = DialogueTextWidth.width(DialoguePlaceholderFormatter.plainDialogueText(text));
        return offset(offset)
                + dialogueLine(lineNumber, text)
                + offset(-offset - textWidth);
    }

    private String dialogueLine(int lineNumber, String text) {
        String font = DialogueGlyphs.LINE_FONT_PREFIX + lineNumber;
        return colorOpen(DIALOGUE_TEXT_COLOR)
                + font(font, DialoguePlaceholderFormatter.miniMessageDialogueText(text))
                + colorClose();
    }

    private String infoLine(String infoLine, DialogueRenderContext context) {
        String text = " " + infoLine;
        int textWidth = DialogueTextWidth.width(DialoguePlaceholderFormatter.plainText(text));
        int offset = context.infoTextOffsetPixels();
        return offset(offset)
                + text(context.infoColor(), text, DialogueGlyphs.DEFAULT_TEXT_FONT)
                + offset(-offset - textWidth);
    }

    private String arrowLayer(DialogueRenderContext context) {
        int offset = context.arrowOffsetPixels();
        return offset(offset)
                + colorOpen(context.infoColor())
                + font(DialogueGlyphs.DIALOGUE_FONT_TAG, DialogueGlyphs.ARROW)
                + colorClose()
                + offset(-offset - DialogueGlyphs.ARROW_WIDTH);
    }

    private String textLayer(int offset, String color, String text, String font) {
        String safeText = text == null ? "" : text;
        int textWidth = DialogueTextWidth.width(DialoguePlaceholderFormatter.plainText(safeText));
        return offset(offset)
                + text(color, safeText, font)
                + offset(-offset - textWidth);
    }

    private String text(String color, String text, String font) {
        return colorOpen(color)
                + font(font, DialoguePlaceholderFormatter.miniMessageText(text))
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
        return "<color:" + DialoguePlaceholderFormatter.miniMessageColor(color, "#ffffff") + ">";
    }

    private String colorClose() {
        return "</color>";
    }

}
