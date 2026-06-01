package me.nakilex.levelplugin.quests.dialogue;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Adds fully revealed timed quest dialogue lines to the player's normal chat history.
 *
 * <p>Chat messages cannot be edited after they are sent. Sending each typewriter frame would either
 * flood the player's chat history or require blank-line redraws that hide the messages the player had
 * before speaking to an NPC. The timed session remains the single source of dialogue progress, while
 * this renderer emits each completed line once so normal chat history stays visible and the NPC lines
 * remain in that history after the conversation ends.
 */
public class ChatRenderer implements QuestDialogueSession.Renderer {
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
        if (state != QuestDialogueSession.State.WAITING) {
            return;
        }

        Conversation conversation = conversations.computeIfAbsent(player.getUniqueId(), ignored -> new Conversation());
        if (conversation.markRendered(lineNumber, lineCount)) {
            player.sendMessage(dialogueLine(speaker, visibleText, state, lineNumber, lineCount));
        }
    }

    @Override
    public void clear(Player player) {
        Conversation conversation = conversations.get(player.getUniqueId());
        if (conversation != null && conversation.isComplete()) {
            conversations.remove(player.getUniqueId());
        }
    }

    /** Build the shared chat presentation for a fully revealed dialogue line. */
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

    private static class Conversation {
        private final Set<Integer> renderedLines = new HashSet<>();
        private int lineCount;

        private boolean markRendered(int lineNumber, int lineCount) {
            this.lineCount = Math.max(this.lineCount, lineCount);
            return renderedLines.add(lineNumber);
        }

        private boolean isComplete() {
            return lineCount > 0 && renderedLines.contains(lineCount);
        }
    }
}
