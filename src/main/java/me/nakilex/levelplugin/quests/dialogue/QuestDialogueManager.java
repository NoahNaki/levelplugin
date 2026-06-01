package me.nakilex.levelplugin.quests.dialogue;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs lightweight per-player timed dialogue sessions for linear quest text.
 */
public class QuestDialogueManager implements Listener {
    private static final Pattern PLAYER_PLACEHOLDER = Pattern.compile("(?i)<player>");

    private final Map<UUID, QuestDialogueSession> sessions = new HashMap<>();
    private final ChatRenderer chatRenderer = new ChatRenderer();
    private final BukkitTask tickTask;

    public QuestDialogueManager(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    /** Start a timed linear dialogue, replacing any active timed dialogue for the player. */
    public void startDialogue(Player player, int npcId, List<QuestDialogueLine> lines, Runnable onFinish) {
        if (player == null || lines == null || lines.isEmpty()) {
            return;
        }

        cancel(player);
        chatRenderer.begin(player);
        List<QuestDialogueLine> preparedLines = lines.stream()
                .map(line -> prepareLine(player, line))
                .toList();
        startDialogue(player, npcId, preparedLines, 0, preparedLines.size(), onFinish);
    }

    /**
     * Start one line from a larger legacy dialogue sequence while preserving its displayed position.
     */
    public void startDialogueLine(Player player, int npcId, QuestDialogueLine line, int lineNumber, int lineCount,
                                  Runnable onFinish) {
        if (player == null || line == null) {
            return;
        }
        if (lineNumber <= 1) {
            cancel(player);
            chatRenderer.begin(player);
        } else {
            cancelSession(player);
        }
        startDialogue(player, npcId, List.of(prepareLine(player, line)), Math.max(0, lineNumber - 1), lineCount,
                onFinish);
    }

    /** Skip or advance only when the player clicked the NPC that owns the active session. */
    public boolean nextOrSkip(Player player, int npcId) {
        if (player == null) {
            return false;
        }
        QuestDialogueSession session = sessions.get(player.getUniqueId());
        if (session == null || session.getNpcId() != npcId) {
            return false;
        }
        session.nextOrSkip();
        return true;
    }

    public boolean hasSession(Player player) {
        return player != null && sessions.containsKey(player.getUniqueId());
    }

    /** Cancel a player's active timed dialogue without running its finish callback. */
    public void cancel(Player player) {
        if (player == null) {
            return;
        }
        cancelSession(player);
        chatRenderer.discard(player);
    }

    /** Stop the scheduler and discard all sessions during plugin shutdown. */
    public void shutdown() {
        tickTask.cancel();
        for (QuestDialogueSession session : new ArrayList<>(sessions.values())) {
            session.cancel();
        }
        sessions.clear();
        chatRenderer.discardAll();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer());
    }

    private void tick() {
        for (QuestDialogueSession session : new ArrayList<>(sessions.values())) {
            if (!session.getPlayer().isOnline()) {
                cancel(session.getPlayer());
                continue;
            }
            session.tick();
        }
    }


    private void startDialogue(Player player, int npcId, List<QuestDialogueLine> lines, int lineNumberOffset,
                               int lineCount, Runnable onFinish) {
        QuestDialogueSession session = new QuestDialogueSession(player, npcId, lines, lineNumberOffset, lineCount,
                onFinish, this::removeFinishedSession, System::currentTimeMillis, new ActionBarRenderer(chatRenderer));
        sessions.put(player.getUniqueId(), session);
    }

    private QuestDialogueLine prepareLine(Player player, QuestDialogueLine line) {
        String playerName = Matcher.quoteReplacement(player.getName());
        return new QuestDialogueLine(
                PLAYER_PLACEHOLDER.matcher(line.speakerName()).replaceAll(playerName),
                PLAYER_PLACEHOLDER.matcher(line.text()).replaceAll(playerName),
                line.typingMillis(),
                line.waitMillis()
        );
    }

    private void removeFinishedSession(QuestDialogueSession session) {
        sessions.remove(session.getPlayer().getUniqueId(), session);
    }

    private void cancelSession(Player player) {
        QuestDialogueSession session = sessions.remove(player.getUniqueId());
        if (session != null) {
            session.cancel();
        }
    }

    private static class ActionBarRenderer implements QuestDialogueSession.Renderer {
        private final ChatRenderer chatRenderer;

        private ActionBarRenderer(ChatRenderer chatRenderer) {
            this.chatRenderer = chatRenderer;
        }
        @Override
        public void render(Player player, QuestDialogueLine line, Component speaker, Component visibleText,
                           QuestDialogueSession.State state, int lineNumber, int lineCount) {
            Component message = ChatRenderer.dialogueLine(speaker, visibleText, state, lineNumber, lineCount);
            player.sendActionBar(Component.empty());
            player.sendActionBar(message);
            chatRenderer.render(player, line, speaker, visibleText, state, lineNumber, lineCount);
        }

        @Override
        public void clear(Player player) {
            player.sendActionBar(Component.empty());
            chatRenderer.clear(player);
        }
    }
}
