package me.nakilex.levelplugin.quests.dialogue;

import me.nakilex.levelplugin.dialogue.DialogueDefinition;
import me.nakilex.levelplugin.dialogue.DialogueEndReason;
import me.nakilex.levelplugin.dialogue.DialogueSessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/** Compatibility adapter for legacy quest dialogue APIs backed by the unified page-based dialogue engine. */
public class QuestDialogueManager implements Listener {
    private final DialogueSessionManager sessions;

    public QuestDialogueManager(JavaPlugin plugin, DialogueSessionManager sessions) {
        this.sessions = sessions;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /** Start a timed linear dialogue, replacing any active dialogue for the player. */
    public void startDialogue(Player player, int npcId, List<QuestDialogueLine> lines, Runnable onFinish) {
        if (player == null || lines == null || lines.isEmpty()) return;
        DialogueDefinition definition = DialogueDefinition.fromLegacyQuest(String.valueOf(npcId), "NPC", lines);
        sessions.startDialogue(player, definition, npcId, player.getLocation(), onFinish);
    }

    /** Start one legacy line; retained for compatibility while NPCDialogManager delegates full sequences directly. */
    public void startDialogueLine(Player player, int npcId, QuestDialogueLine line, int lineNumber, int lineCount,
                                  Runnable onFinish) {
        if (player == null || line == null) return;
        startDialogue(player, npcId, List.of(line), onFinish);
    }

    public boolean nextOrSkip(Player player, int npcId) {
        return sessions.nextOrSkip(player, npcId);
    }

    public boolean hasSession(Player player) {
        return sessions.hasSession(player);
    }

    public void cancel(Player player) {
        sessions.endDialogue(player, DialogueEndReason.RESET);
    }

    public void shutdown() {
        // DialogueSessionManager owns the scheduler and active sessions.
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.endDialogue(event.getPlayer(), DialogueEndReason.QUIT);
    }
}
