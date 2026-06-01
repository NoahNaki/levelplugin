package me.nakilex.levelplugin.quests.dialogue;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mirrors timed quest dialogue into the chat window without owning dialogue timing.
 *
 * <p>This is intentionally lighter than a packet-level chat history interceptor. It redraws a small
 * dialogue window as one chat component, keeping completed lines dimmed above the currently typing
 * line. The session renderer remains the single source of timing, formatting-safe slicing and state.
 */
public class ChatRenderer implements QuestDialogueSession.Renderer {
    private static final int CLEAR_LINES = 20;
    private static final int HISTORY_LIMIT = 12;
    private static final int SPACING_LINES = 2;

    private final Map<UUID, Conversation> conversations = new HashMap<>();

    /** Start a fresh conversation, discarding any retained lines from an older dialogue. */
    public void begin(Player player) {
        if (player != null) {
            conversations.put(player.getUniqueId(), new Conversation());
        }
    }

    /** Discard retained state when dialogue is cancelled or a player leaves. */
    public void discard(Player player) {
        if (player != null) {
            conversations.remove(player.getUniqueId());
        }
    }

    /** Discard every retained conversation during plugin shutdown. */
    public void discardAll() {
        conversations.clear();
    }

    @Override
    public void render(Player player, QuestDialogueLine line, Component speaker, Component visibleText,
                       QuestDialogueSession.State state, int lineNumber, int lineCount) {
        Conversation conversation = conversations.computeIfAbsent(player.getUniqueId(), ignored -> new Conversation());
        Component currentLine = dialogueLine(speaker, visibleText, state, lineNumber, lineCount);
        Component retainedLine = dialogueLine(speaker, visibleText, QuestDialogueSession.State.TYPING,
                lineNumber, lineCount);
        conversation.update(lineNumber, lineCount, retainedLine);
        player.sendMessage(conversation.redraw(currentLine, lineNumber));
    }

    @Override
    public void clear(Player player) {
        Conversation conversation = conversations.get(player.getUniqueId());
        if (conversation != null && conversation.isComplete()) {
            conversations.remove(player.getUniqueId());
        }
    }

    /** Build the active-line presentation used in the chat dialogue window. */
    static Component dialogueLine(Component speaker, Component visibleText, QuestDialogueSession.State state,
                                  int lineNumber, int lineCount) {
        Component message = Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text(lineNumber + "/" + lineCount, NamedTextColor.GRAY))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                .append(speaker.colorIfAbsent(NamedTextColor.YELLOW))
                .append(Component.text(": ", NamedTextColor.WHITE))
                .append(visibleText.colorIfAbsent(NamedTextColor.WHITE));
        if (state == QuestDialogueSession.State.WAITING) {
            return message.append(Component.text("  [Click to continue]", NamedTextColor.DARK_GRAY));
        }
        return message;
    }

    private static Component darken(Component component) {
        List<Component> children = component.children().stream().map(ChatRenderer::darken).toList();
        return component.children(children).color(NamedTextColor.DARK_GRAY);
    }

    private static class Conversation {
        private final Map<Integer, Component> lines = new LinkedHashMap<>();
        private int lineCount;

        private void update(int lineNumber, int lineCount, Component line) {
            this.lineCount = Math.max(this.lineCount, lineCount);
            lines.put(lineNumber, line);
            while (lines.size() > HISTORY_LIMIT) {
                Integer firstLine = lines.keySet().iterator().next();
                lines.remove(firstLine);
            }
        }

        private Component redraw(Component currentLine, int currentLineNumber) {
            List<Component> previousLines = new ArrayList<>();
            for (Map.Entry<Integer, Component> entry : lines.entrySet()) {
                if (entry.getKey() < currentLineNumber) {
                    previousLines.add(darken(entry.getValue()));
                }
            }

            Component message = Component.text("\n".repeat(CLEAR_LINES));
            for (Component previousLine : previousLines) {
                message = message.append(previousLine).append(Component.newline());
            }
            if (!previousLines.isEmpty()) {
                message = message.append(Component.text("\n".repeat(SPACING_LINES)));
            }
            return message.append(currentLine);
        }

        private boolean isComplete() {
            return lineCount > 0 && lines.containsKey(lineCount);
        }
    }
}
