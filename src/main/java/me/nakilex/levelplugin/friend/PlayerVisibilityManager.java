package me.nakilex.levelplugin.friend;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.settings.data.PlayerSettings;
import me.nakilex.levelplugin.settings.data.PlayerVisibility;
import me.nakilex.levelplugin.settings.managers.SettingsManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Applies player visibility settings using Bukkit's hidePlayer/showPlayer.
 */
public class PlayerVisibilityManager implements Listener {
    private final Main plugin;
    private final FriendManager friendManager;
    private final SettingsManager settingsManager;

    public PlayerVisibilityManager(Main plugin, FriendManager friendManager, SettingsManager settingsManager) {
        this.plugin = plugin;
        this.friendManager = friendManager;
        this.settingsManager = settingsManager;
    }

    public void updatePlayer(Player viewer) {
        apply(viewer);
    }

    public void apply(Player viewer) {
        PlayerSettings settings = settingsManager.getSettings(viewer);
        PlayerVisibility vis = settings.getPlayerVisibility();
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(viewer)) continue;
            switch (vis) {
                case HIDE_ALL -> viewer.hidePlayer(plugin, target);
                case FRIENDS_ONLY -> {
                    if (friendManager.areFriends(viewer.getUniqueId(), target.getUniqueId())) {
                        viewer.showPlayer(plugin, target);
                    } else {
                        viewer.hidePlayer(plugin, target);
                    }
                }
                case SHOW_ALL -> viewer.showPlayer(plugin, target);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();
        // apply new player's visibility setting
        Bukkit.getScheduler().runTaskLater(plugin, () -> apply(joined), 1L);
        // update existing players in relation to the new player
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(joined)) apply(p);
            }
        }, 2L);
    }
}
