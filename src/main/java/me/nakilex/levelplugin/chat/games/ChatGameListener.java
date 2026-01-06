package me.nakilex.levelplugin.chat.games;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import me.nakilex.levelplugin.utils.WorldExclusionUtil;

/** Observes chat messages and forwards them to the active game. */
public class ChatGameListener implements Listener {

    private final ChatGameManager manager;

    public ChatGameListener(ChatGameManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (manager == null || manager.getActiveGame() == null) {
            return;
        }
        if (WorldExclusionUtil.isExcluded(event.getPlayer())) {
            return;
        }
        manager.handleChatAsync(event.getPlayer(), event.getMessage());
    }
}
