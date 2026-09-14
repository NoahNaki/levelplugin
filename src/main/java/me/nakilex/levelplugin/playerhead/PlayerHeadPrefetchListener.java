package me.nakilex.levelplugin.playerhead;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Renders a player's face on join so their first chat message already has it. Without this the
 * first message falls back to Steve while the async skin fetch is still in flight.
 */
public class PlayerHeadPrefetchListener implements Listener {
    private final JavaPlugin plugin;

    public PlayerHeadPrefetchListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerHeadRenderer.getHead(plugin, event.getPlayer());
    }
}
