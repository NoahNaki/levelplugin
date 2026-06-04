package me.nakilex.levelplugin.dialogue.render;

import me.nakilex.levelplugin.dialogue.DialogueAnswer;
import me.nakilex.levelplugin.dialogue.DialogueEndReason;
import me.nakilex.levelplugin.dialogue.DialoguePage;
import me.nakilex.levelplugin.dialogue.DialogueRenderer;
import me.nakilex.levelplugin.dialogue.DialogueSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChatDialogueRenderer implements DialogueRenderer {
    private static final int CLEAR_LINES = 20;
    private static final int HISTORY_LIMIT = 12;
    private static final int SPACING_LINES = 2;
    private final Map<UUID, Conversation> conversations = new HashMap<>();

    @Override
    public void begin(Player player, DialogueSession session) {
        if (player != null) conversations.put(player.getUniqueId(), new Conversation());
    }

    @Override
    public void render(Player player, DialogueSession session, DialoguePage page, Component speaker, Component visibleText,
                       int lineNumber, int lineCount, List<DialogueAnswer> answers, int selectedAnswerIndex,
                       List<Component> replyLines) {
        if (player == null) return;
        Conversation conversation = conversations.computeIfAbsent(player.getUniqueId(), ignored -> new Conversation());
        Component active = dialogueLine(speaker, visibleText, session.state(), lineNumber, lineCount);
        conversation.update(lineNumber, lineCount, active);
        Component message = conversation.redraw(active, lineNumber);
        for (Component reply : replyLines) {
            message = message.append(Component.newline())
                    .append(Component.text("↳ ", NamedTextColor.DARK_GRAY))
                    .append(reply.colorIfAbsent(NamedTextColor.GRAY));
        }
        if (answers != null && !answers.isEmpty()) {
            message = message.append(Component.newline()).append(Component.newline())
                    .append(Component.text("Choose your answer:", NamedTextColor.AQUA));
            for (int i = 0; i < answers.size(); i++) {
                DialogueAnswer answer = answers.get(i);
                boolean selected = i == selectedAnswerIndex;
                message = message.append(Component.newline())
                        .append(Component.text(selected ? "➤ " : "  ", selected ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY))
                        .append(Component.text(answer.text(), selected ? NamedTextColor.GREEN : NamedTextColor.WHITE));
            }
            message = message.append(Component.newline())
                    .append(Component.text("(Scroll to cycle, click NPC to confirm)", NamedTextColor.GRAY));
        }
        player.sendMessage(message);
    }

    @Override
    public void clear(Player player, DialogueSession session, DialogueEndReason reason) {
        if (player != null) conversations.remove(player.getUniqueId());
    }

    private static Component dialogueLine(Component speaker, Component visibleText, DialogueSession.State state,
                                          int lineNumber, int lineCount) {
        Component message = Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text(lineNumber + "/" + lineCount, NamedTextColor.GRAY))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                .append(speaker.colorIfAbsent(NamedTextColor.YELLOW))
                .append(Component.text(": ", NamedTextColor.WHITE))
                .append(visibleText.colorIfAbsent(NamedTextColor.WHITE));
        if (state == DialogueSession.State.WAITING || state == DialogueSession.State.ANSWERING) {
            return message.append(Component.text("  [Click to continue]", NamedTextColor.DARK_GRAY));
        }
        return message;
    }

    private static Component darken(Component component) {
        List<Component> children = component.children().stream().map(ChatDialogueRenderer::darken).toList();
        return component.children(children).color(NamedTextColor.DARK_GRAY);
    }

    private static class Conversation {
        private final Map<Integer, Component> lines = new LinkedHashMap<>();
        private int lineCount;

        private void update(int lineNumber, int lineCount, Component line) {
            this.lineCount = Math.max(this.lineCount, lineCount);
            lines.put(lineNumber, line);
            while (lines.size() > HISTORY_LIMIT) {
                Integer first = lines.keySet().iterator().next();
                lines.remove(first);
            }
        }

        private Component redraw(Component currentLine, int currentLineNumber) {
            Component message = Component.text("\n".repeat(CLEAR_LINES));
            boolean hadPrevious = false;
            for (Map.Entry<Integer, Component> entry : lines.entrySet()) {
                if (entry.getKey() < currentLineNumber) {
                    message = message.append(darken(entry.getValue())).append(Component.newline());
                    hadPrevious = true;
                }
            }
            if (hadPrevious) message = message.append(Component.text("\n".repeat(SPACING_LINES)));
            return message.append(currentLine);
        }
    }
}
