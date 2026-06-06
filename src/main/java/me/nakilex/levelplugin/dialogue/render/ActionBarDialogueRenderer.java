package me.nakilex.levelplugin.dialogue.render;

import me.nakilex.levelplugin.dialogue.model.DialogueAnswer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Static Lux-style actionbar renderer for a single loaded dialogue page.
 */
public class ActionBarDialogueRenderer implements DialogueRenderer {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
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
            hud.append(imageLayer(context.dialogueBackgroundColor(), 0,
                    DialogueGlyphs.FOG_FONT, DialogueGlyphs.FOG, DialogueGlyphs.FOG_WIDTH));
        }

        hud.append(backgroundLayer(
                context.dialogueBackgroundColor(),
                context.dialogueBackgroundOffsetPixels(),
                DialogueGlyphs.DIALOGUE_BACKGROUND_FONT,
                DialogueGlyphs.DIALOGUE_BACKGROUND,
                DialogueGlyphs.DIALOGUE_WIDTH
        ));

        if (context.characterBoxEnabled()) {
            hud.append(imageLayer(
                    context.characterBackgroundColor(),
                    context.characterOffsetPixels(),
                    DialogueGlyphs.CHARACTER_BACKGROUND_FONT,
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
            hud.append(arrowLayer(context.arrowOffsetPixels(), context.arrowColor()));
        }

        hud.append(answerPreview(context));
        return hud.toString();
    }

    public Component render(DialogueRenderContext context) {
        return MINI_MESSAGE.deserialize(renderMiniMessage(context));
    }

    private String imageLayer(String color, int offset, String font, String glyph, int width) {
        return colorOpen(color)
                + offset(offset)
                + font(font, glyph)
                + offset(-offset - width)
                + colorClose();
    }

    private String backgroundLayer(String color, int offset, String font, String glyph, int width) {
        return colorOpen(color)
                + offset(offset - width)
                + font(font, glyph)
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
        return imageLayer(context.nameBackgroundColor(), context.nameBackgroundOffsetPixels(),
                DialogueGlyphs.NAME_BOX_FONT, glyph, boxWidth);
    }

    private String dialogueLineLayer(int lineNumber, String text, DialogueRenderContext context) {
        int offset = context.dialogueTextOffsetPixels();
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

    private String arrowLayer(int offset, String color) {
        return offset(offset)
                + colorOpen(color)
                + font(DialogueGlyphs.ARROW_FONT, DialogueGlyphs.ARROW)
                + colorClose()
                + offset(-offset - DialogueGlyphs.ARROW_WIDTH);
    }

    private String answerPreview(DialogueRenderContext context) {
        if (context.page().answers().isEmpty()) {
            return "";
        }

        StringBuilder answers = new StringBuilder();
        answers.append(imageLayer(
                context.answerBackgroundColor(),
                context.answerBackgroundOffsetPixels(),
                DialogueGlyphs.ANSWER_BACKGROUND_FONT,
                DialogueGlyphs.ANSWER_BACKGROUND,
                DialogueGlyphs.ANSWER_WIDTH
        ));

        List<DialogueAnswer> visibleAnswers = new ArrayList<>(context.page().answers().values());
        int answerCount = Math.min(visibleAnswers.size(), 3);
        for (int i = 0; i < answerCount; i++) {
            answers.append(answerLine(i + 1, visibleAnswers.get(i).text(), context));
        }
        answers.append(arrowLayer(context.answerArrowOffsetPixels(), context.arrowColor()));
        return answers.toString();
    }

    private String answerLine(int answerNumber, String text, DialogueRenderContext context) {
        String safeText = DialoguePlaceholderFormatter.plainDialogueText(text);
        int offset = context.answerLineOffsetPixels();
        int textWidth = DialogueTextWidth.width(safeText);
        return offset(offset)
                + colorOpen(DIALOGUE_TEXT_COLOR)
                + font(DialogueGlyphs.ANSWER_FONT_PREFIX + answerNumber,
                DialoguePlaceholderFormatter.miniMessageDialogueText(text))
                + colorClose()
                + offset(-offset - textWidth);
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
