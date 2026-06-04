package me.nakilex.levelplugin.quests.dialogue.hud;

import me.nakilex.levelplugin.dialogue.DialogueAnswer;
import me.nakilex.levelplugin.dialogue.DialogueEndReason;
import me.nakilex.levelplugin.dialogue.DialoguePage;
import me.nakilex.levelplugin.dialogue.DialogueRenderer;
import me.nakilex.levelplugin.dialogue.DialogueSession;
import me.nakilex.levelplugin.quests.dialogue.QuestDialogueLine;
import me.nakilex.levelplugin.quests.dialogue.QuestDialogueSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Stub renderer for the future glyph-based dialogue HUD. It is intentionally not wired in as the default renderer yet.
 */
public class ResourcePackDialogueRenderer implements QuestDialogueSession.Renderer, DialogueRenderer {
    private final DialogueHudResourcePackManager resourcePackManager;

    public ResourcePackDialogueRenderer(DialogueHudResourcePackManager resourcePackManager) {
        this.resourcePackManager = resourcePackManager;
    }

    public boolean canRenderGlyphUi() {
        return resourcePackManager != null && resourcePackManager.rendererEnabled()
                && resourcePackManager.status().glyphUiEnabled();
    }

    @Override
    public void begin(Player player, DialogueSession session) {
        // HUD renderer is a stub until the resource-pack layout is implemented.
    }

    @Override
    public void render(Player player, DialogueSession session, DialoguePage page, Component speaker,
                       List<Component> completedPageLines, Component visibleText, int pageLineIndex, int pageLineCount,
                       List<DialogueAnswer> answers, int selectedAnswerIndex, List<Component> replyLines) {
        if (player == null || canRenderGlyphUi()) {
            return;
        }
        player.sendActionBar(Component.text("Dialogue HUD assets unavailable; using chat dialogue.", NamedTextColor.DARK_GRAY));
    }

    @Override
    public void clear(Player player, DialogueSession session, DialogueEndReason reason) {
        clear(player);
    }

    @Override
    public void render(Player player, QuestDialogueLine line, Component speaker, Component visibleText,
                       QuestDialogueSession.State state, int lineNumber, int lineCount) {
        if (player == null || canRenderGlyphUi()) {
            return;
        }
        player.sendActionBar(Component.text("Dialogue HUD assets unavailable; using chat dialogue.", NamedTextColor.DARK_GRAY));
    }

    @Override
    public void clear(Player player) {
        if (player != null) {
            player.sendActionBar(Component.empty());
            player.clearTitle();
        }
    }

    public Component renderSpeakerName(Component speaker) {
        return Component.empty().append(DialogueHudGlyphs.glyph(DialogueHudGlyphs.NAMEPLATE_LEFT))
                .append(speaker == null ? Component.empty() : speaker)
                .append(DialogueHudGlyphs.glyph(DialogueHudGlyphs.NAMEPLATE_RIGHT));
    }

    public Component renderPageLines(List<Component> completedPageLines, Component visibleText) {
        Component output = Component.empty();
        if (completedPageLines != null) {
            for (Component line : completedPageLines) {
                output = output.append(line == null ? Component.empty() : line).append(Component.newline());
            }
        }
        return output.append(renderVisibleText(visibleText));
    }

    public Component renderVisibleText(Component visibleText) {
        return visibleText == null ? Component.empty() : visibleText;
    }

    public Component renderAnswers(List<Component> answers, int selectedAnswer) {
        Component output = Component.empty();
        if (answers == null) return output;
        for (int i = 0; i < answers.size(); i++) {
            output = output.append(renderAnswer(answers.get(i), i == selectedAnswer));
            if (i + 1 < answers.size()) output = output.append(Component.newline());
        }
        return output;
    }

    public Component renderAnswer(Component answer, boolean selected) {
        Component selector = selected ? DialogueHudGlyphs.glyph(DialogueHudGlyphs.SELECTOR_ARROW) : Component.text(" ");
        return selector.append(DialogueHudGlyphs.glyph(DialogueHudGlyphs.ANSWER_BACKGROUND))
                .append(answer == null ? Component.empty() : answer);
    }

    public Component renderDialogueBackground() {
        return DialogueHudGlyphs.glyph(DialogueHudGlyphs.DIALOGUE_BACKGROUND);
    }
}
