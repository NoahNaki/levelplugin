package me.nakilex.levelplugin.quests.dialogue.hud;

import me.nakilex.levelplugin.dialogue.DialogueAnswer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * LuxDialogues-style action-bar HUD composer that positions each visual element from one shared anchor.
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
        Component hud = Component.empty().append(DialogueHudGlyphs.offset(anchorOffset()));

        if (fogEnabled()) {
            hud = hud.append(at(fogOffset(), DialogueHudGlyphs.fogBackground(), dialogueWidth()));
        }

        hud = hud.append(at(dialogueBackgroundOffset(), DialogueHudGlyphs.background(), dialogueWidth()));

        String speakerText = speakerText(speaker);
        if (nameplateEnabled()) {
            hud = hud.append(at(nameBackgroundOffset(), composeNameplateBackground(speakerText), nameplateWidth(speakerText)));
            hud = hud.append(at(nameTextOffset(), DialogueHudGlyphs.defaultText(speakerText, NamedTextColor.YELLOW)));
        }

        hud = hud.append(at(dialogueLineOffset(), composeDialogueText(completedLines, visibleText)));
        hud = hud.append(composeAnswers(answers, selectedAnswerIndex));

        return hud;
    }

    private Component at(int offset, Component component) {
        return at(offset, component, DialogueTextWidth.width(component));
    }

    private Component at(int offset, Component component, int width) {
        if (component == null || component.equals(Component.empty())) {
            return Component.empty();
        }

        return Component.empty()
                .append(DialogueHudGlyphs.offset(offset))
                .append(component)
                .append(DialogueHudGlyphs.offset(-offset - Math.max(0, width)));
    }

    private Component composeDialogueText(List<Component> completedLines, Component visibleText) {
        List<Component> lines = collectLines(completedLines, visibleText);
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

    private List<Component> collectLines(List<Component> completedLines, Component visibleText) {
        int maxLines = plugin.getConfig().getInt("dialogue-hud.layout.dialogue.max-lines", 3);
        List<Component> lines = new ArrayList<>();

        if (completedLines != null) {
            lines.addAll(completedLines);
        }
        if (visibleText != null) {
            lines.add(visibleText);
        }
        if (lines.size() > maxLines) {
            return lines.subList(lines.size() - maxLines, lines.size());
        }
        return lines;
    }

    private Component composeNameplateBackground(String speakerText) {
        int repeats = nameplateMiddleRepeats(speakerText);
        Component background = Component.empty().append(DialogueHudGlyphs.nameplateLeft());
        for (int i = 0; i < repeats; i++) {
            background = background.append(DialogueHudGlyphs.nameplateMiddle());
        }
        return background.append(DialogueHudGlyphs.nameplateRight());
    }

    private Component composeAnswers(List<DialogueAnswer> answers, int selectedIndex) {
        if (!answersEnabled() || answers == null || answers.isEmpty()) {
            return Component.empty();
        }

        Component result = Component.empty();
        int limit = Math.min(answers.size(), maxVisibleAnswers());

        for (int i = 0; i < limit; i++) {
            int answerStart = i * (answerWidth() + answerGap());
            result = result.append(at(answerBackgroundOffset() + answerStart, DialogueHudGlyphs.answerBackground(), answerWidth()));

            String answerText = (i + 1) + ". " + answers.get(i).text();
            result = result.append(at(answerLineOffset() + answerStart,
                    DialogueHudGlyphs.defaultText(answerText, i == selectedIndex ? NamedTextColor.WHITE : NamedTextColor.GRAY)));

            if (i == selectedIndex) {
                result = result.append(at(arrowOffset() + answerStart, DialogueHudGlyphs.selector()));
            }
        }

        return result;
    }

    private String speakerText(Component speaker) {
        Component safeSpeaker = speaker == null
                ? Component.text("NPC", NamedTextColor.YELLOW)
                : speaker.colorIfAbsent(NamedTextColor.YELLOW);
        return PlainTextComponentSerializer.plainText().serialize(safeSpeaker);
    }

    private int nameplateMiddleRepeats(String speakerText) {
        return Math.max(1, (DialogueTextWidth.width(speakerText) + nameTextPadding() * 2) / nameMidWidth());
    }

    private int nameplateWidth(String speakerText) {
        return nameStartWidth() + nameplateMiddleRepeats(speakerText) * nameMidWidth() + nameEndWidth();
    }

    private int anchorOffset() { return plugin.getConfig().getInt("dialogue-hud.layout.anchor-offset", 320); }
    private int dialogueBackgroundOffset() { return layoutOffset("dialogue-background", 0); }
    private int dialogueLineOffset() { return layoutOffset("dialogue-line", 10); }
    private int nameBackgroundOffset() { return layoutOffset("name-background", 20); }
    private int nameTextOffset() { return layoutOffset("name", nameBackgroundOffset() + nameTextPadding()); }
    private int answerBackgroundOffset() { return layoutOffset("answer-background", 140); }
    private int answerLineOffset() { return layoutOffset("answer-line", 153); }
    private int arrowOffset() { return layoutOffset("arrow", 133); }
    private int characterOffset() { return layoutOffset("character", -16); }
    private int fogOffset() { return layoutOffset("fog", 0); }

    private int dialogueWidth() { return plugin.getConfig().getInt("dialogue-hud.layout.dialogue.width", 209); }
    private int answerWidth() { return plugin.getConfig().getInt("dialogue-hud.layout.answers.width", 134); }
    private int answerGap() { return plugin.getConfig().getInt("dialogue-hud.layout.answers.gap", 10); }
    private int maxVisibleAnswers() { return plugin.getConfig().getInt("dialogue-hud.layout.answers.max-visible", 2); }
    private int nameStartWidth() { return 3; }
    private int nameMidWidth() { return Math.max(1, plugin.getConfig().getInt("dialogue-hud.layout.nameplate.mid-width", 2)); }
    private int nameEndWidth() { return 3; }
    private int nameTextPadding() { return plugin.getConfig().getInt("dialogue-hud.layout.nameplate.text-padding", 5); }

    private boolean nameplateEnabled() { return plugin.getConfig().getBoolean("dialogue-hud.layout.nameplate.enabled", true); }
    private boolean answersEnabled() { return plugin.getConfig().getBoolean("dialogue-hud.layout.answers.enabled", true); }
    private boolean characterEnabled() { return plugin.getConfig().getBoolean("dialogue-hud.layout.character.enabled", false); }
    private boolean fogEnabled() { return plugin.getConfig().getBoolean("dialogue-hud.layout.fog.enabled", false); }

    private int layoutOffset(String key, int fallback) {
        return plugin.getConfig().getInt("dialogue-hud.layout.offsets." + key, fallback);
    }

    public DialogueHudResourcePackManager manager() {
        return manager;
    }
}
