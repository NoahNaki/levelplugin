package me.nakilex.levelplugin.luxbridge.render;

import me.nakilex.levelplugin.luxbridge.model.LuxAnswer;
import me.nakilex.levelplugin.luxbridge.model.LuxDialogue;
import me.nakilex.levelplugin.luxbridge.model.LuxPage;
import me.nakilex.levelplugin.luxbridge.resource.LuxBridgeResourceManager;
import me.nakilex.levelplugin.luxbridge.resource.LuxImageDefinition;
import me.nakilex.levelplugin.luxbridge.util.LuxBridgeElementUtil;
import me.nakilex.levelplugin.luxbridge.util.LuxBridgeFormat;
import me.nakilex.levelplugin.luxbridge.util.LuxBridgeTextWidth;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class LuxBridgeRenderer {
    private final LuxBridgeResourceManager resources;

    public LuxBridgeRenderer(LuxBridgeResourceManager resources) {
        this.resources = resources;
    }

    public String render(Player player, LuxDialogue dialogue, LuxPage page, int typedCharacters, int selectedAnswer) {
        StringBuilder bar = new StringBuilder();
        if (dialogue.backgroundFogEnabled()) {
            bar.append(imageLayer(dialogue.fogColor(), 0, dialogue.fogImage()));
        }
        bar.append(imageLayer(dialogue.dialogueBackgroundColor(), dialogue.dialogueBackgroundOffset(), dialogue.dialogueBackgroundImage()));
        if (dialogue.characterImageEnabled()) {
            bar.append(imageLayer(dialogue.characterBackgroundColor(), dialogue.characterOffset(), dialogue.characterBackgroundImage()));
        }
        if (dialogue.characterNameEnabled()) {
            bar.append(characterName(player, dialogue));
        }
        bar.append(dialogueLines(player, dialogue, page, typedCharacters));
        bar.append(answers(player, dialogue, page, selectedAnswer, typedCharacters >= totalLineLength(page)));
        return bar.toString();
    }

    private String characterName(Player player, LuxDialogue dialogue) {
        String name = LuxBridgeFormat.placeholders(player, dialogue.characterName());
        if (name.isBlank()) return "";
        LuxImageDefinition start = image(dialogue.nameStartImage());
        LuxImageDefinition mid = image(dialogue.nameMidImage());
        LuxImageDefinition end = image(dialogue.nameEndImage());
        int midRepeats = Math.max(1, LuxBridgeTextWidth.getWidth(name) / 2);
        int width = (start.width() - 1) + ((mid.width() - 1) * midRepeats) + end.width() - 1;
        StringBuilder builder = new StringBuilder();
        builder.append(LuxBridgeElementUtil.offset(dialogue.nameBackgroundOffset()));
        builder.append(LuxBridgeElementUtil.colorOpen(dialogue.nameBackgroundColor()))
                .append(imageFont(start)).append(LuxBridgeElementUtil.offset(-1))
                .append(imageFont(mid).repeat(midRepeats)).append(LuxBridgeElementUtil.offset(-1))
                .append(imageFont(end))
                .append(LuxBridgeElementUtil.offset(-width + 3))
                .append(LuxBridgeElementUtil.colorClose());
        builder.append(LuxBridgeElementUtil.offset(dialogue.nameOffset()));
        builder.append(text(dialogue.nameColor(), "levelplugin_dialogue:levelplugin_dialogue_character_name", name));
        builder.append(LuxBridgeElementUtil.offset((-dialogue.nameOffset()) - LuxBridgeTextWidth.getWidth(name) - 3));
        builder.append(LuxBridgeElementUtil.offset(-dialogue.nameBackgroundOffset()));
        return builder.toString();
    }

    private String dialogueLines(Player player, LuxDialogue dialogue, LuxPage page, int typedCharacters) {
        StringBuilder builder = new StringBuilder(LuxBridgeElementUtil.offset(dialogue.dialogueLineOffset()));
        int remaining = typedCharacters;
        List<String> lines = page.lines();
        for (int i = 0; i < Math.min(lines.size(), resources.lines().dialogueLineCount()); i++) {
            String line = LuxBridgeFormat.placeholders(player, lines.get(i));
            int take = Math.max(0, Math.min(line.length(), remaining));
            String shown = line.substring(0, take);
            builder.append(text(dialogue.dialogueColor(), "levelplugin_dialogue:levelplugin_dialogue_line_" + (i + 1), shown));
            builder.append(LuxBridgeElementUtil.offset(-LuxBridgeTextWidth.getWidth(LuxBridgeFormat.stripMini(shown))));
            remaining -= line.length();
        }
        builder.append(LuxBridgeElementUtil.offset(-dialogue.dialogueLineOffset()));
        return builder.toString();
    }

    private String answers(Player player, LuxDialogue dialogue, LuxPage page, int selectedAnswer, boolean typingDone) {
        if (!typingDone || !page.gotoPage().isBlank() || page.answers().isEmpty()) return "";
        StringBuilder builder = new StringBuilder(imageLayer(dialogue.answerBackgroundColor(), dialogue.answerBackgroundOffset(), dialogue.answerBackgroundImage()));
        builder.append(LuxBridgeElementUtil.offset(dialogue.answerLineOffset()));
        int index = 1;
        for (LuxAnswer answer : visibleAnswers(page)) {
            String prefix = dialogue.answerNumbers() ? index + ". " : "";
            String line = LuxBridgeFormat.placeholders(player, prefix + answer.text());
            String color = index == selectedAnswer ? dialogue.selectedColor() : dialogue.answerColor();
            builder.append(text(color, "levelplugin_dialogue:levelplugin_dialogue_answer_" + index, line));
            builder.append(LuxBridgeElementUtil.offset(-LuxBridgeTextWidth.getWidth(LuxBridgeFormat.stripMini(line))));
            index++;
        }
        builder.append(LuxBridgeElementUtil.offset(-dialogue.answerLineOffset()));
        builder.append(LuxBridgeElementUtil.offset(dialogue.arrowOffset()));
        builder.append(LuxBridgeElementUtil.coloredFont(dialogue.arrowColor(), imageFontName(dialogue.arrowImage()), image(dialogue.arrowImage()).glyph()));
        builder.append(LuxBridgeElementUtil.offset((-dialogue.arrowOffset()) - image(dialogue.arrowImage()).width()));
        return builder.toString();
    }

    public int totalLineLength(LuxPage page) {
        int total = 0;
        for (String line : page.lines()) total += line == null ? 0 : line.length();
        return total;
    }

    private List<LuxAnswer> visibleAnswers(LuxPage page) {
        return new ArrayList<>(page.answers().values()).subList(0, Math.min(page.answers().size(), resources.lines().answerLineCount()));
    }

    private String imageLayer(String color, int offset, String imageId) {
        LuxImageDefinition image = image(imageId);
        return LuxBridgeElementUtil.offset(offset)
                + LuxBridgeElementUtil.coloredFont(color, imageFontName(imageId), image.glyph())
                + LuxBridgeElementUtil.offset((-offset) - image.width());
    }

    private String imageFont(LuxImageDefinition image) {
        return LuxBridgeElementUtil.font(imageFontName(image.id()), image.glyph());
    }

    private String imageFontName(String imageId) {
        return "levelplugin_dialogue:" + imageId;
    }

    private LuxImageDefinition image(String id) {
        LuxImageDefinition image = resources.image(id);
        if (image == null) throw new IllegalArgumentException("Unknown LuxBridge image id: " + id);
        return image;
    }

    private String text(String color, String font, String text) {
        return LuxBridgeElementUtil.coloredFont(color, font, LuxBridgeFormat.miniMessageText(text));
    }
}
