package me.nakilex.levelplugin.dialogue.render;

import me.nakilex.levelplugin.dialogue.model.DialogueAnswer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

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
            hud.append(luxImageLayer(context.dialogueBackgroundColor(), 0,
                    DialogueGlyphs.FOG_FONT, DialogueGlyphs.FOG, DialogueGlyphs.FOG_WIDTH));
        }

        hud.append(luxImageLayer(
                context.dialogueBackgroundColor(),
                context.dialogueBackgroundOffsetPixels(),
                DialogueGlyphs.DIALOGUE_BACKGROUND_FONT,
                DialogueGlyphs.DIALOGUE_BACKGROUND,
                DialogueGlyphs.DIALOGUE_WIDTH
        ));

        if (context.characterBoxEnabled()) {
            hud.append(luxImageLayer(
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
                    DialogueGlyphs.CHARACTER_NAME_FONT));
        }

        hud.append(dialogueLines(context));
        hud.append(answerPreview(context));

        if (context.page().steadyInfoLine() != null && !context.page().steadyInfoLine().isBlank()) {
            hud.append(infoLine(context.page().steadyInfoLine(), context));
        }
        hud.append(handLayer(context.arrowOffsetPixels(), context.arrowColor()));

        return hud.toString();
    }

    public Component render(DialogueRenderContext context) {
        return MINI_MESSAGE.deserialize(renderMiniMessage(context));
    }

    private String luxImageLayer(String color, int offset, String font, String glyph, int width) {
        return colorOpen(color)
                + offset(offset)
                + font(font, glyph)
                + offset((-offset) - width)
                + colorClose();
    }

    private String nameBoxLayer(String characterName, DialogueRenderContext context) {
        int nameWidth = DialogueTextWidth.width(DialoguePlaceholderFormatter.plainText(characterName));
        int midRepeats = Math.max(1, (int) Math.ceil(nameWidth / (double) DialogueGlyphs.NAME_MID_WIDTH));
        String glyph = DialogueGlyphs.NAME_START + DialogueGlyphs.NAME_MID.repeat(midRepeats) + DialogueGlyphs.NAME_END;
        int boxWidth = DialogueGlyphs.NAME_START_WIDTH
                + (midRepeats * DialogueGlyphs.NAME_MID_WIDTH)
                + DialogueGlyphs.NAME_END_WIDTH;
        return luxImageLayer(context.nameBackgroundColor(), context.nameBackgroundOffsetPixels(),
                DialogueGlyphs.NAME_BOX_FONT, glyph, boxWidth);
    }

    private String dialogueLines(DialogueRenderContext context) {
        List<String> lines = context.page().lines();
        if (lines.isEmpty()) {
            return "";
        }

        StringBuilder text = new StringBuilder(offset(context.dialogueTextOffsetPixels()));
        for (int i = 0; i < Math.min(lines.size(), 5); i++) {
            String line = lines.get(i);
            text.append(textLine(DialogueGlyphs.LINE_FONT_PREFIX + (i + 1), DIALOGUE_TEXT_COLOR, line, true));
            text.append(offset(-DialogueTextWidth.width(DialoguePlaceholderFormatter.plainDialogueText(line))));
        }
        text.append(offset(-context.dialogueTextOffsetPixels()));
        return text.toString();
    }

    private String answerPreview(DialogueRenderContext context) {
        if (context.page().answers().isEmpty()) {
            return "";
        }

        StringBuilder answers = new StringBuilder(luxImageLayer(
                context.answerBackgroundColor(),
                context.answerBackgroundOffsetPixels(),
                DialogueGlyphs.ANSWER_BACKGROUND_FONT,
                DialogueGlyphs.ANSWER_BACKGROUND,
                DialogueGlyphs.ANSWER_WIDTH
        ));
        answers.append(offset(context.answerLineOffsetPixels()));
        int index = 0;
        for (DialogueAnswer answer : context.page().answers().values()) {
            if (index >= 3) {
                break;
            }
            String text = answer.text() == null || answer.text().isBlank() ? answer.id() : answer.text();
            answers.append(textLine(DialogueGlyphs.ANSWER_FONT_PREFIX + (index + 1), DIALOGUE_TEXT_COLOR, text, true));
            answers.append(offset(-DialogueTextWidth.width(DialoguePlaceholderFormatter.plainDialogueText(text))));
            index++;
        }
        answers.append(offset(-context.answerLineOffsetPixels()));
        answers.append(handLayer(context.answerArrowOffsetPixels(), context.arrowColor()));
        return answers.toString();
    }

    private String infoLine(String infoLine, DialogueRenderContext context) {
        String text = " " + infoLine;
        int textWidth = DialogueTextWidth.width(DialoguePlaceholderFormatter.plainText(text));
        int offset = context.infoTextOffsetPixels();
        return offset(offset)
                + text(context.infoColor(), text, DialogueGlyphs.INFO_FONT)
                + offset(-offset - textWidth);
    }

    private String handLayer(int offset, String color) {
        return offset(offset)
                + colorOpen(color)
                + font(DialogueGlyphs.HAND_FONT, DialogueGlyphs.HAND)
                + colorClose()
                + offset(-offset - DialogueGlyphs.HAND_WIDTH);
    }

    private String textLayer(int offset, String color, String text, String font) {
        String safeText = text == null ? "" : text;
        int textWidth = DialogueTextWidth.width(DialoguePlaceholderFormatter.plainText(safeText));
        return offset(offset)
                + text(color, safeText, font)
                + offset(-offset - textWidth);
    }

    private String text(String color, String text, String font) {
        return textLine(font, color, text, false);
    }

    private String textLine(String font, String color, String text, boolean dialogueText) {
        String formatted = dialogueText
                ? DialoguePlaceholderFormatter.miniMessageDialogueText(text)
                : DialoguePlaceholderFormatter.miniMessageText(text);
        return colorOpen(color)
                + font(font, formatted)
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
