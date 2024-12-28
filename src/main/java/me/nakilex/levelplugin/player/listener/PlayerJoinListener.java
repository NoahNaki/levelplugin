package me.nakilex.levelplugin.player.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import me.nakilex.levelplugin.player.level.managers.LevelManager;

public class PlayerJoinListener implements Listener {

    private final LevelManager levelManager;

    public PlayerJoinListener(LevelManager levelManager) {
        this.levelManager = levelManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        levelManager.initializePlayer(player);
    }
}
