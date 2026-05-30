package me.nakilex.levelplugin.npc.dialog.display;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

/** Tracks dialogue-owned frames separately from permanent Bukkit chat history. */
public final class DialogueChatHistory {
    private static final int MAX_PREVIOUS_FRAMES = 3;
    private final Map<UUID, PlayerHistory> histories = new ConcurrentHashMap<>();

    public DialogueRenderedMessage update(Player player, DialogueFrame frame) {
        PlayerHistory history = histories.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerHistory());
        synchronized (history) {
            DialogueRenderedMessage next = new DialogueRenderedMessage(frame);
            if (history.current != null && isNewLine(history.current.frame(), frame)) {
                remember(history, history.current);
            }
            history.current = next;
            return next;
        }
    }

    public boolean isActive(Player player) { return player != null && histories.containsKey(player.getUniqueId()); }

    public List<DialogueRenderedMessage> previous(Player player) {
        PlayerHistory history = player == null ? null : histories.get(player.getUniqueId());
        if (history == null) return List.of();
        synchronized (history) { return List.copyOf(history.previous); }
    }

    public String composeDarkMessage(Player player, DialogueFrame frame) {
        update(player, frame);
        PlayerHistory history = histories.get(player.getUniqueId());
        synchronized (history) {
            List<String> lines = new ArrayList<>();
            for (DialogueRenderedMessage previous : history.previous) lines.add(previous.composeDarkMessage());
            if (history.current != null) lines.add(history.current.composeMessage());
            return String.join("\n", lines);
        }
    }

    public void clear(Player player) { if (player != null) histories.remove(player.getUniqueId()); }

    private boolean isNewLine(DialogueFrame current, DialogueFrame next) {
        return current.index() != next.index() || current.total() != next.total() || !current.speaker().equals(next.speaker());
    }

    private void remember(PlayerHistory history, DialogueRenderedMessage message) {
        history.previous.addLast(message);
        while (history.previous.size() > MAX_PREVIOUS_FRAMES) history.previous.removeFirst();
    }

    private static final class PlayerHistory {
        private final Deque<DialogueRenderedMessage> previous = new ArrayDeque<>();
        private DialogueRenderedMessage current;
    }
}
