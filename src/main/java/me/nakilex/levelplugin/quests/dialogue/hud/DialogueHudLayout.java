package me.nakilex.levelplugin.quests.dialogue.hud;

import me.nakilex.levelplugin.dialogue.DialogueAnswer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * LuxDialogues-style action-bar HUD composer that treats glyphs as layered pixel elements instead of inline text.
 */
public final class DialogueHudLayout {
    private final JavaPlugin plugin;
    private final DialogueHudResourcePackManager manager;

    public DialogueHudLayout(JavaPlugin plugin, DialogueHudResourcePackManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public Component compose(Component speaker, List<Component> completedLines, Component visibleText,
                             List<DialogueAnswer> answers, int selectedAnswerIndex) {
        Component hud = Component.empty();

        int startOffset = plugin.getConfig().getInt("dialogue-hud.layout.start-offset", 420);
        int dialogueWidth = plugin.getConfig().getInt("dialogue-hud.layout.dialogue.width", 209);
        int textX = plugin.getConfig().getInt("dialogue-hud.layout.dialogue.text-x", 18);

        hud = hud.append(DialogueHudGlyphs.offset(startOffset));
        hud = hud.append(DialogueHudGlyphs.background());
        hud = hud.append(DialogueHudGlyphs.offset(-dialogueWidth + textX));

        Component text = composeDialogueText(completedLines, visibleText);
        hud = hud.append(text);
        int textWidth = DialogueTextWidth.width(text);
        hud = hud.append(DialogueHudGlyphs.offset(-textWidth));

        if (plugin.getConfig().getBoolean("dialogue-hud.layout.nameplate.enabled", true)) {
            int nameplateX = plugin.getConfig().getInt("dialogue-hud.layout.nameplate.x", 24);
            hud = hud.append(DialogueHudGlyphs.offset(nameplateX));
            Component nameplate = composeNameplate(speaker);
            hud = hud.append(nameplate);
            hud = hud.append(DialogueHudGlyphs.offset(-DialogueTextWidth.width(speaker)));
        }

        if (plugin.getConfig().getBoolean("dialogue-hud.layout.answers.enabled", true)
                && answers != null
                && !answers.isEmpty()) {
            int answersX = plugin.getConfig().getInt("dialogue-hud.layout.answers.x", 140);
            hud = hud.append(DialogueHudGlyphs.offset(answersX));
            hud = hud.append(composeAnswers(answers, selectedAnswerIndex));
        }

        return hud;
    }

    private Component composeDialogueText(List<Component> completedLines, Component visibleText) {
        int maxLines = plugin.getConfig().getInt("dialogue-hud.layout.dialogue.max-lines", 3);
        List<Component> lines = new ArrayList<>();

        if (completedLines != null) {
            lines.addAll(completedLines);
        }

        if (visibleText != null) {
            lines.add(visibleText);
        }

        if (lines.size() > maxLines) {
            lines = lines.subList(lines.size() - maxLines, lines.size());
        }

        Component result = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                result = result.append(DialogueHudGlyphs.defaultText("  ", NamedTextColor.WHITE));
            }

            result = result.append(DialogueHudGlyphs.defaultFont(
                    (lines.get(i) == null ? Component.empty() : lines.get(i)).colorIfAbsent(NamedTextColor.WHITE)
            ));
        }

        return result;
    }

    private Component composeNameplate(Component speaker) {
        Component safeSpeaker = speaker == null
                ? Component.text("NPC", NamedTextColor.YELLOW)
                : speaker.colorIfAbsent(NamedTextColor.YELLOW);

        String speakerText = PlainTextComponentSerializer.plainText().serialize(safeSpeaker);
        int speakerWidth = DialogueTextWidth.width(speakerText);

        int midWidth = plugin.getConfig().getInt("dialogue-hud.layout.nameplate.mid-width", 2);
        int repeats = Math.max(1, (speakerWidth + 8) / Math.max(1, midWidth));

        Component plate = Component.empty()
                .append(DialogueHudGlyphs.nameplateLeft());

        for (int i = 0; i < repeats; i++) {
            plate = plate.append(DialogueHudGlyphs.nameplateMiddle());
        }

        plate = plate.append(DialogueHudGlyphs.nameplateRight());

        int plateWidth = 3 + repeats * midWidth + 3;
        plate = plate.append(DialogueHudGlyphs.offset(-plateWidth + 4));
        plate = plate.append(DialogueHudGlyphs.defaultFont(safeSpeaker));
        plate = plate.append(DialogueHudGlyphs.offset(-speakerWidth));

        return plate;
    }

    private Component composeAnswers(List<DialogueAnswer> answers, int selectedAnswerIndex) {
        int maxVisible = plugin.getConfig().getInt("dialogue-hud.layout.answers.max-visible", 3);
        int answerWidth = plugin.getConfig().getInt("dialogue-hud.layout.answers.background-width", 134);
        int answerTextX = plugin.getConfig().getInt("dialogue-hud.layout.answers.text-x", 10);

        Component result = Component.empty();
        int limit = Math.min(answers.size(), maxVisible);

        for (int i = 0; i < limit; i++) {
            DialogueAnswer answer = answers.get(i);
            boolean selected = i == selectedAnswerIndex;

            result = result.append(DialogueHudGlyphs.answerBackground());
            result = result.append(DialogueHudGlyphs.offset(-answerWidth + answerTextX));

            if (selected) {
                result = result.append(DialogueHudGlyphs.selector());
                result = result.append(DialogueHudGlyphs.offset(4));
            }

            String text = (i + 1) + ". " + answer.text();
            Component answerText = DialogueHudGlyphs.defaultFont(
                    Component.text(text, selected ? NamedTextColor.WHITE : NamedTextColor.GRAY)
            );
            result = result.append(answerText);

            int textWidth = DialogueTextWidth.width(text);
            result = result.append(DialogueHudGlyphs.offset(answerWidth - answerTextX - textWidth + 4));
        }

        return result;
    }

    public DialogueHudResourcePackManager manager() {
        return manager;
    }
}
