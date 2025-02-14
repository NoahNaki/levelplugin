package me.nakilex.levelplugin.player.listener;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final LevelManager levelManager;

    public PlayerJoinListener(LevelManager levelManager) {
        this.levelManager = levelManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        // Recalculate stats to apply the proper health scaling
        StatsManager.getInstance().recalcDerivedStats(player);
        levelManager.initializePlayer(player);

        // Ensure the player's data is loaded, then apply health scaling and update scoreboard
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            player.setHealthScaled(true);
            player.setHealthScale(20.0);
            // Update the scoreboard with current money and party info
        }, 20L); // delay of 20 ticks (1 second)
    }
}
