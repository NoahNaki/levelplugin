package me.nakilex.levelplugin.quests.dialogue.hud;

import me.nakilex.levelplugin.dialogue.DialogueAnswer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/** Reimplementation of the LuxDialogues action-bar composition pattern for LevelPlugin's dialogue HUD. */
public final class LuxDialogueHudComposer {
    private final JavaPlugin plugin;

    public LuxDialogueHudComposer(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Component compose(Component speaker, List<Component> completedLines, Component visibleText,
                             List<DialogueAnswer> answers, int selectedAnswerIndex) {
        Component hud = Component.empty().append(DialogueHudGlyphs.offset(anchorOffset()));

        if (fogEnabled()) {
            hud = hud.append(DialogueHudGlyphs.offset(fogOffset() - dialogueWidth()))
                    .append(DialogueHudGlyphs.fogBackground())
                    .append(DialogueHudGlyphs.offset(-fogOffset() - dialogueWidth()));
        }

        hud = hud.append(composeDialogueBackground());
        hud = hud.append(composeNameplate(speaker));

        hud = hud.append(DialogueHudGlyphs.offset(dialogueLineOffset()));
        List<Component> lines = collectLines(completedLines, visibleText);
        for (int i = 0; i < lines.size(); i++) {
            Component line = (lines.get(i) == null ? Component.empty() : lines.get(i)).colorIfAbsent(NamedTextColor.WHITE);
            hud = hud.append(DialogueHudGlyphs.lineText(i, line));
            hud = hud.append(DialogueHudGlyphs.offset(-DialogueTextWidth.width(line)));
        }
        hud = hud.append(DialogueHudGlyphs.offset(-dialogueLineOffset()));

        if (answers != null && !answers.isEmpty()) {
            hud = hud.append(composeAnswersLikeLux(answers, selectedAnswerIndex));
        }

        return hud;
    }

    public Component composeDialogueBackground() {
        int offset = dialogueBackgroundOffset();
        int width = dialogueWidth();
        return Component.empty()
                .append(DialogueHudGlyphs.offset(offset - width))
                .append(DialogueHudGlyphs.background())
                .append(DialogueHudGlyphs.offset(-offset - width));
    }

    public Component composeNameplate(Component speaker) {
        if (!nameplateEnabled()) return Component.empty();

        String text = plainSpeaker(speaker);
        int textWidth = DialogueTextWidth.width(text);
        int bgOffset = nameBackgroundOffset();
        int textOffset = nameTextOffset();
        int repeats = Math.max(1, textWidth / nameMidWidth());

        Component out = Component.empty()
                .append(DialogueHudGlyphs.offset(bgOffset))
                .append(DialogueHudGlyphs.nameplateLeft());

        for (int i = 0; i < repeats; i++) {
            out = out.append(DialogueHudGlyphs.offset(-1))
                    .append(DialogueHudGlyphs.nameplateMiddle());
        }

        out = out.append(DialogueHudGlyphs.offset(-1))
                .append(DialogueHudGlyphs.nameplateRight());

        int plateWidth = nameStartWidth() + repeats * nameMidWidth() + nameEndWidth();
        return out.append(DialogueHudGlyphs.offset(-plateWidth))
                .append(DialogueHudGlyphs.offset(textOffset))
                .append(DialogueHudGlyphs.defaultText(text, NamedTextColor.YELLOW))
                .append(DialogueHudGlyphs.offset(-textWidth))
                .append(DialogueHudGlyphs.offset(-textOffset))
                .append(DialogueHudGlyphs.offset(-bgOffset));
    }

    public Component composeAnswersLikeLux(List<DialogueAnswer> answers, int selectedIndex) {
        if (!answersEnabled() || answers == null || answers.isEmpty()) return Component.empty();

        Component out = Component.empty();
        int bgOffset = answerBackgroundOffset();
        int textOffset = answerLineOffset();
        int arrowOffset = arrowOffset();
        int answerWidth = answerWidth();
        int limit = Math.min(answers.size(), maxVisibleAnswers());

        for (int i = 0; i < limit; i++) {
            DialogueAnswer answer = answers.get(i);
            String text = (i + 1) + ". " + answer.text();
            out = out.append(DialogueHudGlyphs.offset(bgOffset))
                    .append(DialogueHudGlyphs.answerBackground())
                    .append(DialogueHudGlyphs.offset(-answerWidth));

            if (i == selectedIndex) {
                out = out.append(DialogueHudGlyphs.offset(arrowOffset))
                        .append(DialogueHudGlyphs.selector())
                        .append(DialogueHudGlyphs.offset(-arrowOffset));
            }

            out = out.append(DialogueHudGlyphs.offset(textOffset))
                    .append(DialogueHudGlyphs.answerText(i,
                            Component.text(text, i == selectedIndex ? NamedTextColor.WHITE : NamedTextColor.GRAY)))
                    .append(DialogueHudGlyphs.offset(-DialogueTextWidth.width(text)))
                    .append(DialogueHudGlyphs.offset(-textOffset))
                    .append(DialogueHudGlyphs.offset(-bgOffset));

            bgOffset += answerWidth + answerGap();
        }

        return out;
    }

    public List<Component> collectLines(List<Component> completedLines, Component visibleText) {
        int maxLines = plugin.getConfig().getInt("dialogue-hud.layout.dialogue.max-lines", 3);
        List<Component> lines = new ArrayList<>();
        if (completedLines != null) lines.addAll(completedLines);
        if (visibleText != null) lines.add(visibleText);
        if (lines.size() > maxLines) return lines.subList(lines.size() - maxLines, lines.size());
        return lines;
    }

    private String plainSpeaker(Component speaker) {
        Component safe = speaker == null ? Component.text("NPC", NamedTextColor.YELLOW) : speaker.colorIfAbsent(NamedTextColor.YELLOW);
        return PlainTextComponentSerializer.plainText().serialize(safe);
    }

    private int anchorOffset() { return plugin.getConfig().getInt("dialogue-hud.layout.anchor-offset", 320); }
    private int dialogueBackgroundOffset() { return layoutOffset("dialogue-background", 0); }
    private int dialogueLineOffset() { return layoutOffset("dialogue-line", 10); }
    private int nameBackgroundOffset() { return layoutOffset("name-background", 20); }
    private int nameTextOffset() { return layoutOffset("name", 0); }
    private int answerBackgroundOffset() { return layoutOffset("answer-background", 140); }
    private int answerLineOffset() { return layoutOffset("answer-line", 153); }
    private int arrowOffset() { return layoutOffset("arrow", 133); }
    private int fogOffset() { return layoutOffset("fog", 0); }

    private int dialogueWidth() { return plugin.getConfig().getInt("dialogue-hud.layout.dialogue.width", 209); }
    private int answerWidth() { return plugin.getConfig().getInt("dialogue-hud.layout.answers.width", 134); }
    private int answerGap() { return plugin.getConfig().getInt("dialogue-hud.layout.answers.gap", 10); }
    private int maxVisibleAnswers() { return plugin.getConfig().getInt("dialogue-hud.layout.answers.max-visible", 2); }
    private int nameStartWidth() { return 3; }
    private int nameMidWidth() { return Math.max(1, plugin.getConfig().getInt("dialogue-hud.layout.nameplate.mid-width", 2)); }
    private int nameEndWidth() { return 3; }

    private boolean nameplateEnabled() { return plugin.getConfig().getBoolean("dialogue-hud.layout.nameplate.enabled", true); }
    private boolean answersEnabled() { return plugin.getConfig().getBoolean("dialogue-hud.layout.answers.enabled", true); }
    private boolean fogEnabled() { return plugin.getConfig().getBoolean("dialogue-hud.layout.fog.enabled", false); }

    private int layoutOffset(String key, int fallback) {
        return plugin.getConfig().getInt("dialogue-hud.layout.offsets." + key, fallback);
    }
}
