package me.nakilex.levelplugin.quests.dialogue.hud;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks which players have reported a successfully loaded server resource pack. */
public final class DialogueHudPackStatusListener implements Listener {
    private final JavaPlugin plugin;
    private final Set<UUID> loadedPlayers = ConcurrentHashMap.newKeySet();

    public DialogueHudPackStatusListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        String status = event.getStatus().name();
        switch (status) {
            case "SUCCESSFULLY_LOADED" -> {
                loadedPlayers.add(playerId);
                plugin.getLogger().info(event.getPlayer().getName() + " successfully loaded the resource pack.");
            }
            case "DECLINED", "FAILED_DOWNLOAD", "FAILED_RELOAD", "DISCARDED", "INVALID_URL" -> {
                loadedPlayers.remove(playerId);
                plugin.getLogger().warning(event.getPlayer().getName() + " did not load the resource pack: " + status);
            }
            default -> {
                // ACCEPTED / DOWNLOADED / other transitional states keep the current safe state.
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clear(event.getPlayer());
    }

    public boolean hasLoadedPack(Player player) {
        return player != null && loadedPlayers.contains(player.getUniqueId());
    }

    public void markLoaded(Player player) {
        if (player != null) loadedPlayers.add(player.getUniqueId());
    }

    public void clear(Player player) {
        if (player != null) loadedPlayers.remove(player.getUniqueId());
    }
}
