package me.nakilex.levelplugin.quests.dialogue.hud;

import me.nakilex.levelplugin.dialogue.DialogueAnswer;
import me.nakilex.levelplugin.dialogue.DialogueEndReason;
import me.nakilex.levelplugin.dialogue.DialoguePage;
import me.nakilex.levelplugin.dialogue.DialogueRenderer;
import me.nakilex.levelplugin.dialogue.DialogueSession;
import me.nakilex.levelplugin.quests.dialogue.QuestDialogueLine;
import me.nakilex.levelplugin.quests.dialogue.QuestDialogueSession;
import me.nakilex.levelplugin.utils.ChatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;

/** Action-bar dialogue renderer with optional resource-pack glyph framing and readable text fallback. */
public class ResourcePackDialogueRenderer implements QuestDialogueSession.Renderer, DialogueRenderer {
    private static final int MAX_VISIBLE_PAGE_LINES = 2;
    private static final int MAX_VISIBLE_ANSWERS = 3;

    private final DialogueHudResourcePackManager resourcePackManager;

    public ResourcePackDialogueRenderer(DialogueHudResourcePackManager resourcePackManager) {
        this.resourcePackManager = resourcePackManager;
    }

    public boolean canRenderGlyphUi() {
        return resourcePackManager != null
                && resourcePackManager.rendererEnabled()
                && resourcePackManager.actionBarMode()
                && resourcePackManager.useResourcePackGlyphs()
                && resourcePackManager.status().glyphUiEnabled();
    }

    @Override
    public void begin(Player player, DialogueSession session) {
        if (player != null) player.sendActionBar(Component.empty());
    }

    @Override
    public void render(Player player, DialogueSession session, DialoguePage page, Component speaker,
                       List<Component> completedPageLines, Component visibleText, int pageLineIndex, int pageLineCount,
                       List<DialogueAnswer> answers, int selectedAnswerIndex, List<Component> replyLines) {
        if (player == null || resourcePackManager == null || !resourcePackManager.rendererEnabled()) return;
        player.sendActionBar(canRenderGlyphUi()
                ? glyphActionBar(speaker, completedPageLines, visibleText, pageLineIndex, pageLineCount, answers, selectedAnswerIndex, replyLines)
                : plainActionBar(speaker, completedPageLines, visibleText, pageLineIndex, pageLineCount, answers, selectedAnswerIndex, replyLines));
    }

    @Override
    public void clear(Player player, DialogueSession session, DialogueEndReason reason) {
        clear(player);
    }

    @Override
    public void render(Player player, QuestDialogueLine line, Component speaker, Component visibleText,
                       QuestDialogueSession.State state, int lineNumber, int lineCount) {
        if (player == null) return;
        Component message = Component.text("[", NamedTextColor.DARK_GRAY)
                .append(speaker.colorIfAbsent(NamedTextColor.YELLOW))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                .append(visibleText.colorIfAbsent(NamedTextColor.WHITE));
        player.sendActionBar(message);
    }

    @Override
    public void clear(Player player) {
        if (player != null) {
            player.sendActionBar(Component.empty());
            player.clearTitle();
        }
    }

    private Component glyphActionBar(Component speaker, List<Component> completedPageLines, Component visibleText,
                                     int pageLineIndex, int pageLineCount, List<DialogueAnswer> answers,
                                     int selectedAnswerIndex, List<Component> replyLines) {
        return DialogueHudGlyphs.background()
                .append(DialogueHudGlyphs.offset(-8))
                .append(renderSpeakerName(speaker))
                .append(Component.space())
                .append(renderPageLines(lastLines(completedPageLines), visibleText))
                .append(progressComponent(pageLineIndex, pageLineCount))
                .append(answerSummary(answers, selectedAnswerIndex))
                .append(replySummary(replyLines));
    }

    private Component plainActionBar(Component speaker, List<Component> completedPageLines, Component visibleText,
                                     int pageLineIndex, int pageLineCount, List<DialogueAnswer> answers,
                                     int selectedAnswerIndex, List<Component> replyLines) {
        Component output = Component.text("[", NamedTextColor.DARK_GRAY)
                .append(speaker.colorIfAbsent(NamedTextColor.YELLOW))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                .append(renderPageLines(lastLines(completedPageLines), visibleText))
                .append(progressComponent(pageLineIndex, pageLineCount))
                .append(answerSummary(answers, selectedAnswerIndex))
                .append(replySummary(replyLines));
        return output;
    }

    private Component progressComponent(int pageLineIndex, int pageLineCount) {
        if (pageLineCount <= 1) return Component.empty();
        return Component.text("  (" + pageLineIndex + "/" + pageLineCount + ")", NamedTextColor.DARK_GRAY);
    }

    public Component renderSpeakerName(Component speaker) {
        if (canRenderGlyphUi()) {
            return DialogueHudGlyphs.glyph(DialogueHudGlyphs.NAMEPLATE_LEFT)
                    .append(speaker == null ? Component.empty() : speaker.colorIfAbsent(NamedTextColor.YELLOW))
                    .append(DialogueHudGlyphs.glyph(DialogueHudGlyphs.NAMEPLATE_RIGHT));
        }
        return speaker == null ? Component.empty() : speaker.colorIfAbsent(NamedTextColor.YELLOW);
    }

    public Component renderPageLines(List<Component> completedPageLines, Component visibleText) {
        Component output = Component.empty();
        if (completedPageLines != null) {
            for (Component line : completedPageLines) {
                output = appendLine(output, line == null ? Component.empty() : line.colorIfAbsent(NamedTextColor.GRAY));
            }
        }
        return appendLine(output, renderVisibleText(visibleText));
    }

    public Component renderVisibleText(Component visibleText) {
        return visibleText == null ? Component.empty() : visibleText.colorIfAbsent(NamedTextColor.WHITE);
    }

    public Component renderAnswers(List<Component> answers, int selectedAnswer) {
        Component output = Component.empty();
        if (answers == null) return output;
        for (int i = 0; i < answers.size(); i++) {
            output = appendLine(output, renderAnswer(answers.get(i), i == selectedAnswer));
        }
        return output;
    }

    public Component renderAnswer(Component answer, boolean selected) {
        Component selector = selected && canRenderGlyphUi()
                ? DialogueHudGlyphs.selector()
                : Component.text(selected ? "➤ " : "  ", selected ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY);
        return selector.append(answer == null ? Component.empty()
                : answer.colorIfAbsent(selected ? NamedTextColor.GREEN : NamedTextColor.WHITE));
    }

    public Component renderDialogueBackground() {
        return DialogueHudGlyphs.background();
    }

    private Component answerSummary(List<DialogueAnswer> answers, int selectedAnswerIndex) {
        if (answers == null || answers.isEmpty()) return Component.empty();
        Component output = Component.text("  Answers: ", NamedTextColor.AQUA);
        int limit = Math.min(MAX_VISIBLE_ANSWERS, answers.size());
        for (int i = 0; i < limit; i++) {
            if (i > 0) output = output.append(Component.text(" / ", NamedTextColor.DARK_GRAY));
            output = output.append(renderAnswer(ChatUtil.formattedComponent((i + 1) + ". " + answers.get(i).text()), i == selectedAnswerIndex));
        }
        if (answers.size() > limit) {
            output = output.append(Component.text(" / …", NamedTextColor.DARK_GRAY));
        }
        return output;
    }

    private Component replySummary(List<Component> replyLines) {
        if (replyLines == null || replyLines.isEmpty()) return Component.empty();
        Component output = Component.empty();
        for (Component reply : lastLines(replyLines)) {
            output = output.append(Component.text("  ↳ ", NamedTextColor.DARK_GRAY))
                    .append(reply == null ? Component.empty() : reply.colorIfAbsent(NamedTextColor.GRAY));
        }
        return output;
    }

    private List<Component> lastLines(List<Component> lines) {
        if (lines == null || lines.size() <= MAX_VISIBLE_PAGE_LINES) return lines == null ? List.of() : lines;
        return lines.subList(lines.size() - MAX_VISIBLE_PAGE_LINES, lines.size());
    }

    private Component appendLine(Component base, Component line) {
        if (line == null || line.equals(Component.empty())) return base;
        if (!base.equals(Component.empty())) base = base.append(Component.text("  |  ", NamedTextColor.DARK_GRAY));
        return base.append(line);
    }
}
